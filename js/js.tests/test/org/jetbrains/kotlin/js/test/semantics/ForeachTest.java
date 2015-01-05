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

public final class ForeachTest extends AbstractExpressionTest {

    public ForeachTest() {
        super("for/");
    }

    public void testForIteratesOverArray() throws Exception {
        fooBoxTest();
    }

    public void testForOnEmptyArray() throws Exception {
        fooBoxTest();
    }

    public void testLabeledFor() throws Exception {
        fooBoxTest();
    }

    public void testLabeledForWithContinue() throws Exception {
        checkFooBoxIsOk();
    }

    public void testForWithComplexOneStatement() throws Exception {
        checkFooBoxIsOk();
    }

    public void testForIteratesOverLiteralRange() throws Exception {
        checkFooBoxIsOk();
    }

    public void testForIteratesOverNonLiteralRange() throws Exception {
        checkFooBoxIsOk();
    }

    public void testForIteratesOverSomethingWithIterator() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLabeledForWithWhile() throws Exception {
        checkFooBoxIsOk();
    }
}
