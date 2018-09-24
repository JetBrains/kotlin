/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.maven;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class Util {
    static List<String> filterClassPath(final File basedir, List<String> classpath) {
        return classpath.stream().filter(s ->
                new File(s).exists() || new File(basedir, s).exists()
        ).collect(Collectors.toList());
    }

    @NotNull
    public static String[] joinArrays(@Nullable String[] first, @Nullable String[] second) {
        if (first == null) {
            first = new String[0];
        }
        if (second == null) {
            second = new String[0];
        }

        String[] result = new String[first.length + second.length];

        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);

        return result;
    }
}
