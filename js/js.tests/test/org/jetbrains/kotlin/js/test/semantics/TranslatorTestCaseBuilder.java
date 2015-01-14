/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.test.semantics;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class TranslatorTestCaseBuilder {
    public static FilenameFilter emptyFilter = new FilenameFilter() {
        @Override
        public boolean accept(@NotNull File file, @NotNull String name) {
            return true;
        }
    };

    public interface NamedTestFactory {
        @NotNull
        Test createTest(@NotNull String filename);
    }

    @NotNull
    public static TestSuite suiteForDirectory(@NotNull String dataPath,
                                              @NotNull NamedTestFactory factory) {
        return suiteForDirectory(dataPath, true, emptyFilter, factory);
    }

    @NotNull
    public static TestSuite suiteForDirectory(@NotNull String dataPath, boolean recursive,
                                              FilenameFilter filter, @NotNull NamedTestFactory factory) {
        TestSuite suite = new TestSuite(dataPath);
        suite.setName(dataPath);
        appendTestsInDirectory(dataPath, recursive, filter, factory, suite);
        return suite;
    }

    public static void appendTestsInDirectory(String dataPath, boolean recursive,
                                              final FilenameFilter filter, NamedTestFactory factory, TestSuite suite) {
        final String extensionKt = ".kt";
        final FilenameFilter extensionFilter = new FilenameFilter() {
            @Override
            public boolean accept(@NotNull File dir, @NotNull String name) {
                return name.endsWith(extensionKt);
            }
        };
        FilenameFilter resultFilter;
        if (filter != emptyFilter) {
            resultFilter = new FilenameFilter() {
                @Override
                public boolean accept(@NotNull File file, @NotNull String s) {
                    return extensionFilter.accept(file, s) && filter.accept(file, s);
                }
            };
        }
        else {
            resultFilter = extensionFilter;
        }
        File dir = new File(dataPath);
        FileFilter dirFilter = new FileFilter() {
            @Override
            public boolean accept(@NotNull File pathname) {
                return pathname.isDirectory();
            }
        };
        if (recursive) {
            File[] files = dir.listFiles(dirFilter);
            assert files != null : dir;
            List<File> subdirs = Arrays.asList(files);
            Collections.sort(subdirs);
            for (File subdir : subdirs) {
                suite.addTest(suiteForDirectory(dataPath + "/" + subdir.getName(), true, filter, factory));
            }
        }
        List<File> files = Arrays.asList(dir.listFiles(resultFilter));
        Collections.sort(files);
        for (File file : files) {
            suite.addTest(factory.createTest(file.getName()));
        }
    }
}
