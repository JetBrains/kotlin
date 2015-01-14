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

import org.mozilla.javascript.JavaScriptException;

public final class WhenTest extends AbstractExpressionTest {

    public WhenTest() {
        super("when/");
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
            // Ignored
        }
    }

    public void testKt1665() throws Exception {
        checkFooBoxIsOk();
    }

    public void testWhenWithoutExpression() throws Exception {
        fooBoxTest();
    }

    public void testWhenWithOnlyElse() throws Exception {
        fooBoxTest();
    }

    public void testIfInWhen() throws Exception {
        checkFooBoxIsOk();
    }

    public void testForWithOneStmWhen() throws Exception {
        fooBoxTest();
    }

    public void testWhileWithOneStmWhen() throws Exception {
        fooBoxTest();
    }

    public void testDoWhileWithOneStmWhen() throws Exception {
        fooBoxTest();
    }

    public void testIfWithOneStmWhen() throws Exception {
        fooBoxTest();
    }

    public void testWhenWithOneStmWhen() throws Exception {
        fooBoxTest();
    }

    public void testIfInWhenDanglingElseIssue() throws Exception {
        checkFooBoxIsOk();
    }
}
