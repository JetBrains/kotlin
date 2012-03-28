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

package org.jetbrains.k2js.test.semantics;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.test.SingleFileTranslationTest;

import java.io.File;

/**
 * @author Pavel Talanov
 */
public final class InlineTest extends SingleFileTranslationTest {
    public InlineTest() {
        super("inline/");
    }

    public void testFunctionWithoutParameters() throws Exception {
        String filename = "functionWithoutParameters.kt";
        checkFooBoxIsTrue(filename);
        String generatedJSFilePath = getOutputFilePath(filename);
        String outputFileText = FileUtil.loadFile(new File(generatedJSFilePath));
        assertTrue(countOccurrences(outputFileText, "myInlineFun") == 1);
    }

    private static int countOccurrences(@NotNull String str, @NotNull String subStr) {
        int count = 0;
        String s = str;
        while (s.contains(subStr)) {
            s = s.replaceFirst(subStr, "");
            count++;
        }
        return count;
    }
}
