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

import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.mozilla.javascript.JavaScriptException;

public final class PatternMatchingTest extends SingleFileTranslationTest {

    public PatternMatchingTest() {
        super("patternMatching/");
    }

    public void testWhenType() throws Exception {
        fooBoxTest();
    }

    public void testWhenNotType() throws Exception {
        fooBoxTest();
    }

    public void testWhenExecutesOnlyOnce() throws Exception {
        fooBoxTest();
    }

    public void testWhenEvaluatesArgumentOnlyOnce() throws Exception {
        fooBoxTest();
    }

    public void testWhenValue() throws Exception {
        fooBoxTest();
    }

    public void testWhenValueOrType() throws Exception {
        fooBoxTest();
    }

    public void testWhenWithIf() throws Exception {
        fooBoxTest();
    }

    public void testMultipleCases() throws Exception {
        fooBoxIsValue(2.0);
    }

    public void testMatchNullableType() throws Exception {
        fooBoxTest();
    }

    public void testWhenAsExpression() throws Exception {
        fooBoxTest();
    }

    public void testWhenAsExpressionWithThrow() throws Exception {
        try {
            fooBoxTest();
            fail();
        }
        catch (JavaScriptException e) {
        }
    }

    public void testKT1665() throws Exception {
        checkOutput("kt1665.kt", "a", "");
    }

    public void testWhenWithoutExpression() throws Exception {
        fooBoxTest();
    }

    public void testWhenWithOnlyElse() throws Exception {
        fooBoxTest();
    }
}