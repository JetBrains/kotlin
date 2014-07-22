/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.k2js.test.utils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.config.Config;

import java.util.ArrayList;
import java.util.List;

public final class LibraryFilePathsUtil {
    private LibraryFilePathsUtil() {
    }

    @NotNull
    public static List<String> getBasicLibraryFiles() {
        List<String> files = new ArrayList<String>();
        files.addAll(Lists.transform(Config.LIB_FILE_NAMES, new Function<String, String>() {
            @Override
            public String apply(@Nullable String s) {
                return Config.LIBRARIES_LOCATION + s;
            }
        }));
        files.addAll(getReflectionLibraryFiles());
        return files;
    }

    @NotNull
    public static List<String> getAdditionalLibraryFiles() {
        List<String> additionalKotlinFiles = Lists.newArrayList();
        // lets add the standard JS library files
        Iterable<String> names = Config.LIB_FILE_NAMES_DEPENDENT_ON_STDLIB;
        for (String libFileName : names) {
            System.out.println("Compiling " + libFileName);
            additionalKotlinFiles.add(Config.LIBRARIES_LOCATION + libFileName);
        }

        // lets add the standard Kotlin library files
        for (String libFileName : Config.STDLIB_FILE_NAMES) {
            System.out.println("Compiling " + libFileName);
            additionalKotlinFiles.add(Config.STDLIB_LOCATION + libFileName);
        }
        return additionalKotlinFiles;
    }

    @NotNull
    public static List<String> getReflectionLibraryFiles() {
        return JsTestUtils.kotlinFilesInDirectory(Config.REFLECTION_LIB_LOCATION);
    }
}
