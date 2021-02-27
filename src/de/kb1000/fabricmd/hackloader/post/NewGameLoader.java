/*
 * Copyright 2021 kb1000
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.kb1000.fabricmd.hackloader.post;

import net.fabricmc.loader.launch.knot.KnotClient;

import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static de.kb1000.fabricmd.hackloader.accessor.Reflection.get;

public class NewGameLoader implements Runnable {
    @SuppressWarnings("unused")
    private boolean isServer;
    @SuppressWarnings("unused")
    private long window;
    @SuppressWarnings("unused")
    private long context;
    @SuppressWarnings("unused")
    private boolean isInMindustryJava;
    private ClassLoader oldClassLoader;

    @Override
    public void run() {
        final ClassLoader oldClassLoader = this.oldClassLoader;
        this.oldClassLoader = null;

        final Thread currentThread = Thread.currentThread();
        final ThreadGroup threadGroup = currentThread.getThreadGroup();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getThreadGroup() == threadGroup && thread != currentThread) {
                // TODO: kill instead of suspend forever, this may lead to deadlocked processes on exit
                //  That may involve using a Java Agent and re-transform for patching away the cleanup calls
                thread.suspend();
                thread.setName("old" + thread.getName());
            }
        }

        currentThread.setName("main");

        // Remove shutdown hooks registered by the first instance
        try {
            Map<Thread, Thread> threadMap = get(Class.forName("java.lang.ApplicationShutdownHooks"), "hooks");
            Field[] targetFields = Arrays.stream(Thread.class.getDeclaredFields()).filter(f -> f.getType() == Runnable.class).toArray(Field[]::new);
            AccessibleObject.setAccessible(targetFields, true);
            // copy to prevent concurrent modification
            for (final Thread thread: new ArrayList<>(threadMap.keySet())) {
                for (Field field: targetFields) {
                    Object o = field.get(thread);
                    if (o.getClass().getName().startsWith("mindustry.") || o.getClass().getName().startsWith("arc.")) {
                        Runtime.getRuntime().removeShutdownHook(thread);
                    }
                }
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // Remove java.beans reference from log4j (the stripped down Mindustry Java does not have it)
        if (isInMindustryJava) {
            try {
                Log4jPatcher.patch();
            } catch (IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        KnotClient.main(new String[] {});

        // In case we reach this, Mindustry has failed to call exit by itself, and we do not want to keep the process
        // alive (as there are still suspended non-daemon threads from the first instance).
        // Exit explicitly.
        System.exit(0);
    }
}
