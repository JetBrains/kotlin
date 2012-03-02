/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/**
 * @author Pavel Talanov
 */
public final class StringTest extends AbstractExpressionTest {

    public StringTest() {
        super("string/");
    }

    public void testStringConstant() throws Exception {
        checkFooBoxIsTrue("stringConstant.kt");
    }

    public void testStringAssignment() throws Exception {
        checkFooBoxIsTrue("stringAssignment.kt");
    }

    public void testIntInTemplate() throws Exception {
        runFunctionOutputTest("intInTemplate.kt", "foo", "box", "my age is 3");
    }

    public void testStringInTemplate() throws Exception {
        runFunctionOutputTest("stringInTemplate.kt", "foo", "box", "oHelloo");
    }

    public void testMultipleExpressionInTemplate() throws Exception {
        runFunctionOutputTest("multipleExpressionsInTemplate.kt", "foo", "box", "left = 3\nright = 2\nsum = 5\n");
    }

    public void testToStringMethod() throws Exception {
        checkFooBoxIsTrue("objectToStringCallInTemplate.kt");
    }
}
