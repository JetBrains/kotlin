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

public class EvaluationOrderTest extends AbstractExpressionTest {
    public EvaluationOrderTest() {
        super("evaluationOrder/");
    }

    public void test2dangerousInExpression() throws Exception {
        fooBoxTest();
    }

    public void testAndAndWithSideEffect() throws Exception {
        checkFooBoxIsOk();
    }

    public void testOrOrWithSideEffect() throws Exception {
        checkFooBoxIsOk();
    }

    public void testAssignToDotQualifiedWithSideEffect() throws Exception {
        checkFooBoxIsOk();
    }

    public void testAssignToArrayElementWithSideEffect() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCallArgs() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCallVarargs() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCompareToIntrinsicWithSideEffect() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDangerousInsideDangerous() throws Exception {
        fooBoxTest();
    }

    public void testDangerousInline() throws Exception {
        fooBoxTest();
    }

    public void testElvisComplex() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEvaluationOrder1() throws Exception {
        fooBoxTest();
    }

    public void testEqualsIntrinsicWithSideEffect() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEvaluationOrder2() throws Exception {
        checkFooBoxIsOk();
    }

    public void testIfAsFunArgument() throws Exception {
        fooBoxTest();
    }

    public void testIfAsPlusArgument() throws Exception {
        fooBoxTest();
    }

    public void testIfWithComplex() throws Exception {
        checkFooBoxIsOk();
    }

    public void testIntrinsicComplex() throws Exception {
        checkFooBoxIsOk();
    }

    public void testWhenAsMinusArgument() throws Exception {
        fooBoxTest();
    }

    public void testWhenWithComplexConditions() throws Exception {
        checkFooBoxIsOk();
    }

    public void testElvisWithBreakContinueReturn() throws Exception {
        checkFooBoxIsOk();
    }

    public void testAndAndWithBreakContinueReturn() throws Exception {
        checkFooBoxIsOk();
    }

    public void testOrOrWithBreakContinueReturn() throws Exception {
        checkFooBoxIsOk();
    }

    public void testWhenJsLiteralWithSideEffect() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLiteralFunctionAsArgumentWithSideEffect() throws Exception {
        checkFooBoxIsOk();
    }

    public void testIntrinsicWithBreakContinueReturn() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCallWithBreakContinueReturn() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLoopWithBreakContinueReturnInCondition() throws Exception {
        checkFooBoxIsOk();
    }
}
