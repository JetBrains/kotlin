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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.config.EcmaVersion;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class JsTestUtils {

    private JsTestUtils() {
    }

    @NotNull
    public static EnumSet<EcmaVersion> successOnEcmaV5() {
        return EnumSet.of(EcmaVersion.v5);
    }

    @NotNull
    public static String convertFileNameToDotJsFile(@NotNull String filename, @NotNull EcmaVersion ecmaVersion) {
        String postFix = "_" + ecmaVersion.toString() + ".js";
        int index = filename.lastIndexOf('.');
        if (index < 0) {
            return filename + postFix;
        }
        return filename.substring(0, index) + postFix;
    }

    @NotNull
    public static String readFile(@NotNull String path) throws IOException {
        return FileUtil.loadFile(new File(path), /*convertLineSeparators = */ true);
    }

    @NotNull
    public static List<String> getAllFilesInDir(@NotNull String dirName) {
        File dir = new File(dirName);
        assert dir.isDirectory() : dir + " is not a directory.";
        List<String> fullFilePaths = new ArrayList<String>();
        for (String fileName : dir.list()) {
            fullFilePaths.add(dirName + "/" + fileName);
        }
        return fullFilePaths;
    }

    @NotNull
    public static List<String> kotlinFilesInDirectory(@NotNull String directory) {
        File dir = new File(directory);

        if (!dir.isDirectory()) {
            return ContainerUtil.emptyList();
        }

        File[] kotlinFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(@NotNull File dir, @NotNull String name) {
                return name.endsWith(".kt");
            }
        });

        if (kotlinFiles == null) {
            return ContainerUtil.emptyList();
        }

        return ContainerUtil.map2List(kotlinFiles, new Function<File, String>() {
            @Override
            public String fun(File kotlinFile) {
                return kotlinFile.getAbsolutePath();
            }
        });
    }
}
