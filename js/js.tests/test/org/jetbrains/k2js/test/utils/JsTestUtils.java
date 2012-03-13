/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class JsTestUtils {

    private JsTestUtils() {
    }

    public static String convertFileNameToDotJsFile(@NotNull String filename) {
        return filename.substring(0, filename.lastIndexOf('.')) + ".js";
    }

    @NotNull
    public static String readFile(@NotNull String path) throws IOException {
        @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
        FileInputStream stream = new FileInputStream(new File(path));
        return FileUtil.loadTextAndClose(stream);
    }

    @NotNull
    public static List<String> getAllFilesInDir(@NotNull String dirName) {
        File dir = new File(dirName);
        assert dir.isDirectory();
        List<String> fullFilePaths = new ArrayList<String>();
        for (String fileName : dir.list()) {
            fullFilePaths.add(dirName + "/" + fileName);
        }
        return fullFilePaths;
    }
}
