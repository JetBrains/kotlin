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

package org.jetbrains.k2js.test;

import org.mozilla.javascript.JavaScriptException;

/**
 * @author Pavel Talanov
 */
public final class PatternMatchingTest extends TranslationTest {

    final private static String MAIN = "patternMatching/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }


    public void testWhenType() throws Exception {
        testFooBoxIsTrue("whenType.kt");
    }


    public void testWhenNotType() throws Exception {
        testFooBoxIsTrue("whenNotType.kt");
    }


    public void testWhenExecutesOnlyOnce() throws Exception {
        testFooBoxIsTrue("whenExecutesOnlyOnce.kt");
    }


    public void testWhenValue() throws Exception {
        testFooBoxIsTrue("whenValue.kt");
    }


    public void testWhenNotValue() throws Exception {
        testFooBoxIsTrue("whenNotValue.kt");
    }


    public void testWhenValueOrType() throws Exception {
        testFooBoxIsTrue("whenValueOrType.kt");
    }


    public void testWhenWithIf() throws Exception {
        testFooBoxIsTrue("whenWithIf.kt");
    }


    public void testMultipleCases() throws Exception {
        testFunctionOutput("multipleCases.kt", "foo", "box", 2.0);
    }


    public void testMatchNullableType() throws Exception {
        testFooBoxIsTrue("matchNullableType.kt");
    }


    public void testWhenAsExpression() throws Exception {
        testFooBoxIsTrue("whenAsExpression.kt");
    }

    public void whenAsExpressionWithThrow() throws Exception {
        try {
            testFooBoxIsTrue("whenAsExpressionWithThrow.kt");
            fail();
        } catch (JavaScriptException e) {
        }
    }
}