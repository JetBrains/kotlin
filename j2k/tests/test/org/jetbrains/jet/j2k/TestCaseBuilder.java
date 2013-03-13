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

package org.jetbrains.jet.j2k;

import com.intellij.openapi.application.PathManager;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

abstract class TestCaseBuilder {
    @NotNull
    private static final FilenameFilter emptyFilter = new FilenameFilter() {
        @Override
        public boolean accept(File file, String name) {
            return true;
        }
    };

    @NotNull
    public static String getTestDataPathBase() {
        return "testData";
    }

    public static String getHomeDirectory() {
        return new File(PathManager.getResourceRoot(TestCaseBuilder.class, "/org/jetbrains/jet/TestCaseBuilder.class")).getParentFile().getParentFile().getParent();
    }

    public interface NamedTestFactory {
        @NotNull
        Test createTest(@NotNull String dataPath, @NotNull String name);
    }

    @NotNull
    public static TestSuite suiteForDirectory(String baseDataDir, @NotNull String dataPath, @NotNull NamedTestFactory factory) {
        return suiteForDirectory(baseDataDir, dataPath, true, emptyFilter, factory);
    }

    @NotNull
    private static TestSuite suiteForDirectory(String baseDataDir, @NotNull String dataPath, boolean recursive, @NotNull final FilenameFilter filter, @NotNull NamedTestFactory factory) {
        TestSuite suite = new TestSuite(dataPath);
        final String extensionJava = ".jav";

        final FilenameFilter extensionFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, @NotNull String name) {
                return name.endsWith(extensionJava);
            }
        };
        FilenameFilter resultFilter;
        if (filter != emptyFilter) {
            resultFilter = new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return extensionFilter.accept(file, s) && filter.accept(file, s);
                }
            };
        }
        else {
            resultFilter = extensionFilter;
        }
        File dir = new File(baseDataDir + dataPath);
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
                suite.addTest(suiteForDirectory(baseDataDir, dataPath + "/" + subdir.getName(), recursive, filter, factory));
            }
        }
        List<File> files = Arrays.asList(dir.listFiles(resultFilter));
        Collections.sort(files);
        for (File file : files) {
            String fileName = file.getName();
            assert fileName != null;
            suite.addTest(factory.createTest(dataPath, fileName.substring(0, fileName.length() - extensionJava.length())));
        }
        return suite;
    }
}
