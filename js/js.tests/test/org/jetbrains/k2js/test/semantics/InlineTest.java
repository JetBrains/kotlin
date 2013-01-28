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

package org.jetbrains.k2js.test.semantics;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.test.SingleFileTranslationTest;

import java.io.File;


//TODO:
//Inlining turned off
@SuppressWarnings("UnusedDeclaration")
public abstract class InlineTest extends SingleFileTranslationTest {
    public InlineTest() {
        super("inline/");
    }

    public void testFunctionWithoutParameters() throws Exception {
        checkFooBoxIsTrueAndFunctionNameIsNotReferenced("functionWithoutParameters.kt", "myInlineFun");
    }

    public void testFunctionWithBlockBody() throws Exception {
        checkFooBoxIsTrueAndFunctionNameIsNotReferenced("functionWithBlockBody.kt", "myInlineFun");
    }

    public void testFunctionWithOneParameter() throws Exception {
        checkFooBoxIsTrueAndFunctionNameIsNotReferenced("functionWithOneParameter.kt", "myInlineFun");
    }

    public void testFunctionWithTwoParameters() throws Exception {
        checkFooBoxIsTrueAndFunctionNameIsNotReferenced("functionWithTwoParameters.kt", "myInlineFun");
    }

    public void testMethod() throws Exception {
        checkFooBoxIsTrueAndFunctionNameIsNotReferenced("method.kt", "myInlineMethod");
    }

    public void testMethodWithReferenceToThis() throws Exception {
        checkFooBoxIsTrueAndFunctionNameIsNotReferenced("methodWithReferenceToThis.kt", "myInlineMethod");
    }

    public void testMethodWithIndirectlyReferencedThis() throws Exception {
        checkFooBoxIsTrueAndFunctionNameIsNotReferenced("methodWithIndirectlyReferencedThis.kt", "myInlineMethod");
    }

    public void testExtension() throws Exception {
        checkFooBoxIsTrueAndFunctionNameIsNotReferenced("extension.kt", "myInlineExtension");
    }

    public void testExtensionWithParameter() throws Exception {
        checkFooBoxIsTrueAndFunctionNameIsNotReferenced("extensionWithParameter.kt", "myInlineExtension");
    }

    private void checkFooBoxIsTrueAndFunctionNameIsNotReferenced(@NotNull String filename, String funName) throws Exception {
        fooBoxTest();
        String generatedJSFilePath = getOutputFilePath(filename, EcmaVersion.defaultVersion());
        String outputFileText = FileUtil.loadFile(new File(generatedJSFilePath));
        assertTrue(countOccurrences(outputFileText, funName) == 1);
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
