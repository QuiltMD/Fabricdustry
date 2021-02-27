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
package de.kb1000.fabricmd.hackloader;

import net.fabricmc.api.ModInitializer;

import java.util.Objects;

public class FabricDetection {
    public static boolean isFabric() {
        try {
            Objects.requireNonNull(ModInitializer.class.getMethod("onInitialize"));
            return true;
        } catch (NoSuchMethodException | NoClassDefFoundError | NullPointerException e) {
            return false;
        }
    }
}
