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
package de.kb1000.quiltmd.hackloader.accessor;

import java.lang.reflect.Field;

/**
 * Use {@link arc.util.Reflect} instead, except in {@link de.kb1000.fabricmd.hackloader.post}.
 */
public class Reflection {
    public static void set(Object o, String name, Object v) throws NoSuchFieldException, IllegalAccessException {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(o, v);
    }

    public static void set(Object o, String name, boolean v) throws NoSuchFieldException, IllegalAccessException {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.setBoolean(o, v);
    }

    public static void set(Object o, String name, long v) throws NoSuchFieldException, IllegalAccessException {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.setLong(o, v);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<?> c, String name) throws NoSuchFieldException, IllegalAccessException {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(null);
    }
}
