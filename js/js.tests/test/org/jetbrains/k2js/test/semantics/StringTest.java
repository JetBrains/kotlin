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
import org.jetbrains.k2js.config.EcmaVersion;

import java.io.File;
import java.io.IOException;

public final class StringTest extends AbstractExpressionTest {

    public StringTest() {
        super("string/");
    }

    public void testStringConstant() throws Exception {
        fooBoxTest();
    }

    public void testStringAssignment() throws Exception {
        fooBoxTest();
    }

    public void testIntInTemplate() throws Exception {
        fooBoxIsValue("my age is 3");
        checkHasNoToStringCalls();
    }

    public void testStringInTemplate() throws Exception {
        fooBoxIsValue("oHelloo");
        checkHasNoToStringCalls();
    }

    public void testMultipleExpressionsInTemplate() throws Exception {
        fooBoxIsValue("left = 3\nright = 2\nsum = 5\n");
        checkHasNoToStringCalls();
    }

    public void testObjectToStringCallInTemplate() throws Exception {
        fooBoxTest();
    }

    public void testStringNotEqualToNumber() throws Exception {
        fooBoxTest();
    }

    public void testKt2227() throws Exception {
        fooBoxTest();
    }

    public void testKt2227_2() throws Exception {
        fooBoxTest();
    }

    public void testNumbersInTemplate() throws Exception {
        fooBoxIsValue("2354");
    }

    private void checkHasNoToStringCalls() throws IOException {
        for (EcmaVersion ecmaVersion : DEFAULT_ECMA_VERSIONS) {
            String filePath = getOutputFilePath(getTestName(true) + ".kt", ecmaVersion);
            String text = FileUtil.loadFile(new File(filePath), /*convertLineSeparators = */ true);
            assertFalse(filePath + " should not contain toString calls", text.contains("toString"));
        }
    }

    public void testStringSplit() throws Exception {
        checkFooBoxIsOk();
    }

    public void testExtensionMethods() throws Exception {
        checkFooBoxIsOk();
    }

    public void testNullableTypeInStringTemplate() throws Exception {
        checkFooBoxIsOk();
    }
}
