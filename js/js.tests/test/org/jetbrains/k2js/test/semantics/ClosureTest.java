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

public final class ClosureTest extends SingleFileTranslationTest {

    public ClosureTest() {
        super("closure/");
    }

    public void testIteratingCallbacks() throws Exception {
        fooBoxTest();
    }

    public void testLocalParameterInCallback() throws Exception {
        fooBoxTest();
    }

    public void testClosureReferencingMember() throws Exception {
        fooBoxTest();
    }

    public void testClosureInNestedFunctions() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureInNestedFunctionsWhichMixedWithObject() throws Exception {
        fooBoxTest();
    }

    public void testClosureInNestedFunctionsInMethod() throws Exception {
        checkFooBoxIsOk();
    }

    public void testWrappedVariableInExtensionFun() throws Exception {
        fooBoxTest();
    }

    public void testRecursiveFunction() throws Exception {
        checkFooBoxIsOk();
    }

    public void testRecursiveFunctionWithSameNameDeclaration() throws Exception {
        checkFooBoxIsOk();
    }

    public void testRecursiveExtFunction() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureFunctionByInnerFunction() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureLocalFunctionByInnerFunction() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureLocalFunctionByInnerFunctionInConstructor() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureThisInConstructor() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureFunctionAsArgument() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureLocalFunction() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureLocalLiteralFunction() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureThisInLocalFunction() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureValToScopeWithSameNameDeclaration() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureVarToScopeWithSameNameDeclaration() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureLocalInNestedObject() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureThisAndReceiver() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureGenericTypeValue() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureInObject() throws Exception {
        checkFooBoxIsOk();
    }

    public void testWithManyClosuresInNestedFunctionsAndObjects() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureReceiverInLocalExtFunByLocalExtFun() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureArrayListInstance() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureThisByUsingMethodFromParentClass() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureThisInFunctionWhichNamedSameAsParentClass() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureInFewFunctionWithDifferentName() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureLambdaVarInLambda() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureThisInExtLambdaInsideMethod() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureInWithInsideWith() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureInNestedLambdasInObject() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureThisInLambdaInsideMethod() throws Exception {
        checkFooBoxIsOk();
    }
}
