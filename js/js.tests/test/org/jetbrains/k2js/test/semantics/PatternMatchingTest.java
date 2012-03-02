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

import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.mozilla.javascript.JavaScriptException;

/**
 * @author Pavel Talanov
 */
public final class PatternMatchingTest extends SingleFileTranslationTest {

    public PatternMatchingTest() {
        super("patternMatching/");
    }

    public void testWhenType() throws Exception {
        checkFooBoxIsTrue("whenType.kt");
    }

    public void testWhenNotType() throws Exception {
        checkFooBoxIsTrue("whenNotType.kt");
    }

    public void testWhenExecutesOnlyOnce() throws Exception {
        checkFooBoxIsTrue("whenExecutesOnlyOnce.kt");
    }

    public void testWhenValue() throws Exception {
        checkFooBoxIsTrue("whenValue.kt");
    }

    public void testWhenNotValue() throws Exception {
        checkFooBoxIsTrue("whenNotValue.kt");
    }

    public void testWhenValueOrType() throws Exception {
        checkFooBoxIsTrue("whenValueOrType.kt");
    }

    public void testWhenWithIf() throws Exception {
        checkFooBoxIsTrue("whenWithIf.kt");
    }

    public void testMultipleCases() throws Exception {
        runFunctionOutputTest("multipleCases.kt", "foo", "box", 2.0);
    }

    public void testMatchNullableType() throws Exception {
        checkFooBoxIsTrue("matchNullableType.kt");
    }

    public void testWhenAsExpression() throws Exception {
        checkFooBoxIsTrue("whenAsExpression.kt");
    }

    public void testWhenAsExpressionWithThrow() throws Exception {
        try {
            checkFooBoxIsTrue("whenAsExpressionWithThrow.kt");
            fail();
        } catch (JavaScriptException e) {
        }
    }
}