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
    private static final FilenameFilter EMPTY_FILTER = (file, name) -> true;

    public interface NamedTestFactory {
        @NotNull
        Test createTest(@NotNull String filename);
    }

    @NotNull
    public static TestSuite suiteForDirectory(@NotNull String dataPath,
                                              @NotNull NamedTestFactory factory) {
        return suiteForDirectory(dataPath, true, EMPTY_FILTER, factory);
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
                                              FilenameFilter filter, NamedTestFactory factory, TestSuite suite) {
        String extensionKt = ".kt";
        FilenameFilter extensionFilter = (dir, name) -> name.endsWith(extensionKt);
        FilenameFilter resultFilter;
        if (filter != EMPTY_FILTER) {
            resultFilter = (file, s) -> extensionFilter.accept(file, s) && filter.accept(file, s);
        }
        else {
            resultFilter = extensionFilter;
        }
        File dir = new File(dataPath);
        if (recursive) {
            File[] files = dir.listFiles(File::isDirectory);
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
