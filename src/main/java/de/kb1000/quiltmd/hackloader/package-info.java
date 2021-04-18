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
// The statement about not using anything here means this is not public API.
/**
 * DO NOT USE ANYTHING IN THIS PACKAGE (outside of this project).
 * <p>
 * This package contains the code responsible for extracting the JARs (if necessary) and then moves execution into a {@link java.lang.Thread} with a different {@link java.lang.ClassLoader} containing references to those JARs.
 */
package de.kb1000.quiltmd.hackloader;
