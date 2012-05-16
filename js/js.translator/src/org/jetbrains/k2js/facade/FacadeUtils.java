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

package org.jetbrains.k2js.facade;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Pavel Talanov
 */
public class FacadeUtils {

    private FacadeUtils() {
    }

    public static void writeCodeToFile(@NotNull String outputPath, @NotNull String programCode) throws IOException {
        File file = new File(outputPath);
        FileUtil.createParentDirs(file);
        FileWriter writer = new FileWriter(file);
        try {
            writer.write(programCode);
        }
        finally {
            writer.close();
        }
    }

    @NotNull
    public static List<String> parseString(@NotNull String argumentString) {
        List<String> result = new ArrayList<String>();
        StringTokenizer stringTokenizer = new StringTokenizer(argumentString);
        while (stringTokenizer.hasMoreTokens()) {
            result.add(stringTokenizer.nextToken());
        }
        return result;
    }
}
