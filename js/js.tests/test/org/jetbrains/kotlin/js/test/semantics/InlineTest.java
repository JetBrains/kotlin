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

import org.jetbrains.kotlin.js.inline.exception.InlineRecursionException;
import org.jetbrains.kotlin.js.test.SingleFileTranslationWithDirectivesTest;

public final class InlineTest extends SingleFileTranslationWithDirectivesTest {
    public InlineTest() {
        super("inline/");
    }

    public void testInlineSimpleAssignment() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInlineGenericSimple() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInlineIntSimple() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInlineInc() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInlineCallNoInline() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInlineFunctionInLambda() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInlineLambdaNoCapture() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInlineLambdaWithCapture() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInlineChain() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInlineChainWithFewStatements() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCallInlineFunctionOnTopLevelSimple() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCallInlineFunctionOnTopLevel() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInlineIf() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInlineNoReturn() throws Exception {
        checkFooBoxIsOk();
    }

    public void testStatementsAfterReturn() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLambdaReassignment() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLambdaReassignmentWithCapture() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInlineMethod() throws Exception {
        checkFooBoxIsOk();
    }

    public void testThisImplicitlyCaptured() throws Exception {
        checkFooBoxIsOk();
    }

    public void testAstCopy() throws Exception {
        checkFooBoxIsOk();
    }

    public void testNoInlineLambda() throws Exception {
        checkFooBoxIsOk();
    }

    public void testlambdaInLambda() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInlineDefaultArgument() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLocalInlineFunction() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLocalInlineFunctionDeclaredInLambda() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLocalInlineExtensionFunction() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLocalInlineFunctionNameClash() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLocalInlineFunctionComplex() throws Exception {
        checkFooBoxIsOk();
    }

    public void testArrayLiteralAliasing() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLocalInlineFunctionReference() throws Exception {
        checkFooBoxIsOk();
    }

    public void testThisLiteralAliasing() throws Exception {
        checkFooBoxIsOk();
    }

    public void testIdentityEquals() throws Exception {
        checkFooBoxIsOk();
    }

    public void testVararg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testMutualRecursion() throws Exception {
        try {
            checkFooBoxIsOk();
        } catch (InlineRecursionException e) {
            return;
        }

        throw new AssertionError("No exception was thrown for mutual recursion of inline functions");
    }

    public void testInlineOrder() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCallableReference() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCallableReferenceOfLocalInline() throws Exception {
        checkFooBoxIsOk();
    }

    public void testAnonymousObjectInlineMethod() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClassObject() throws Exception {
        checkFooBoxIsOk();
    }

    public void testExtension() throws Exception {
        checkFooBoxIsOk();
    }

    public void testExtensionWithManyArguments() throws Exception {
        checkFooBoxIsOk();
    }

    public void testParams() throws Exception {
        checkFooBoxIsOk();
    }

    public void testRootConstructor() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSeveralClosures() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSeveralUsage() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleDouble() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleInt() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleEnum() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleLambda() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleObject() throws Exception {
        checkFooBoxIsOk();
    }

    public void testIncrementProperty() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleReturnFunctionWithResultUnused() throws Exception {
        checkFooBoxIsOk();
    }
}
