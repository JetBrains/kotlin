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
        checkFooBoxIsOk();
    }

    public void testStringInTemplate() throws Exception {
        checkFooBoxIsOk();
    }

    public void testMultipleExpressionsInTemplate() throws Exception {
        checkFooBoxIsOk();
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

    public void testKt8020() throws Exception {
        checkFooBoxIsOk();
    }

    public void testKt2227_2() throws Exception {
        fooBoxTest();
    }

    public void testNumbersInTemplate() throws Exception {
        checkFooBoxIsOk();
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

    public void testSubSequence() throws Exception {
        checkFooBoxIsOk();
    }
}
