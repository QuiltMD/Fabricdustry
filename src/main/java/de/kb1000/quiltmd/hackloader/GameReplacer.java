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
package de.kb1000.quiltmd.hackloader;

import arc.Core;
import arc.util.Reflect;
import mindustry.Vars;
import mindustry.mod.Mod;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

import static arc.util.Reflect.set;

public class GameReplacer {
    // TODO: un-hardcode this list
    private static final String[] JARS = new String[] {
            "access-widener-1.0.2.jar", "asm-9.1.jar", "asm-analysis-9.1.jar", "asm-commons-9.1.jar",
            "asm-tree-9.1.jar", "asm-util-9.1.jar", "checker-qual-3.8.0.jar", "error_prone_annotations-2.5.1.jar",
            "failureaccess-1.0.1.jar", "gson-2.8.6.jar", "guava-30.1.1-jre.jar", "j2objc-annotations-1.3.jar",
            "jimfs-1.2.jar", "jsr305-3.0.2.jar", "listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar",
            "quilt-json5-1.0.0-rc.3.jar", "quilt-loader-0.12.0.1+local.jar", "quilt-loader-sat4j-2.3.5.jar",
            "sponge-mixin-0.9.2+mixin.0.8.2.jar", "tiny-mappings-parser-0.3.0.jar", "tiny-remapper-0.3.2.jar"
    };

    public static void replace() throws IOException, InterruptedException, ReflectiveOperationException, URISyntaxException {
        boolean isServer = Vars.headless;
        final long window;
        final long context;
        if (!isServer) {
            window = Reflect.get(Core.app, "window");
            context = Reflect.get(Core.app, "context");
        } else {
            window = context = 0;
        }

        URL fabricdustryLocation = GameReplacer.class.getProtectionDomain().getCodeSource().getLocation();
        URL fabricdustryJarsLocation;
        if (!fabricdustryLocation.getProtocol().equals("file") || !Files.isDirectory(Paths.get(fabricdustryLocation.toURI()))) {
            final Path path = Files.createTempDirectory("fabricdustrytmp");
            path.toFile().deleteOnExit();
            for (final String jar : JARS) {
                try (final InputStream is = GameReplacer.class.getClassLoader().getResourceAsStream(jar)) {
                    final Path jarFile = path.resolve(jar);
                    // This relies on the deletion order, which is specified to be the reverse of the registration order
                    jarFile.toFile().deleteOnExit();
                    Files.copy(Objects.requireNonNull(is), jarFile);
                }
            }

            fabricdustryJarsLocation = path.toUri().toURL();
        } else {
            fabricdustryJarsLocation = fabricdustryLocation;
        }
        final URL mindustryJar = Mod.class.getProtectionDomain().getCodeSource().getLocation();
        String fabricdustryLocationURL = fabricdustryJarsLocation.toExternalForm();
        if (!fabricdustryLocationURL.endsWith("/"))
            fabricdustryLocationURL += '/';
        final ArrayList<URL> urls = new ArrayList<>();
        for (final String jar : JARS) {
            urls.add(new URL(fabricdustryLocationURL + jar));
        }
        urls.add(mindustryJar);
        final URLClassLoader ucl = new URLClassLoader(urls.toArray(new URL[0]), Mod.class.getClassLoader().getParent());
        final URLClassLoader childUcl = new URLClassLoader(new URL[] {fabricdustryLocation}, ucl);

        boolean isInMindustryJava;
        try {
            isInMindustryJava = JavaDetection.isMindustryJava();
        } catch (LinkageError e) {
            isInMindustryJava = true;
        }

        if (isInMindustryJava) {
            System.setProperty("log4j2.disable.jmx", "true");
        }

        @SuppressWarnings("unchecked") final Class<? extends Runnable> gameLoaderClass = (Class<? extends Runnable>) childUcl.loadClass("de.kb1000.quiltmd.hackloader.post.NewGameLoader");
        final Runnable gameLoader = gameLoaderClass.getConstructor().newInstance();
        set(gameLoader, "isServer", isServer);
        if (!isServer) {
            set(gameLoader, "window", window);
            set(gameLoader, "context", context);
        }
        set(gameLoader, "isInMindustryJava", isInMindustryJava);
        set(gameLoader, "oldClassLoader", Mod.class.getClassLoader());
        final Thread thread = new Thread(gameLoader);
        thread.setDaemon(false);
        thread.setContextClassLoader(ucl);
        thread.start();


        // Block the current thread forever
        Object lock = new Object();
        // Yes, it's intended. Stop asking.
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (lock) {
            lock.wait();
        }
    }
}
