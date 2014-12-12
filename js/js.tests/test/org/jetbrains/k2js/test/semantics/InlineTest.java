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

import org.jetbrains.k2js.inline.exception.InlineRecursionException;
import org.jetbrains.k2js.test.SingleFileTranslationWithDirectivesTest;

public final class InlineTest extends SingleFileTranslationWithDirectivesTest {
    public InlineTest() {
        super("inline/");
    }

    public void testInlineSimpleAssignment() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testInlineGenericSimple() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testInlineIntSimple() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testInlineInc() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testInlineCallNoInline() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testInlineFunctionInLambda() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testInlineLambdaNoCapture() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testInlineLambdaWithCapture() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testInlineChain() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testInlineChainWithFewStatements() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testCallInlineFunctionOnTopLevelSimple() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testCallInlineFunctionOnTopLevel() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testInlineIf() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testInlineNoReturn() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testStatementsAfterReturn() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testLambdaReassignment() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testLambdaReassignmentWithCapture() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testInlineMethod() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testThisImplicitlyCaptured() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testAstCopy() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testNoInlineLambda() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testlambdaInLambda() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testInlineDefaultArgument() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testLocalInlineFunction() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testLocalInlineFunctionDeclaredInLambda() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testLocalInlineExtensionFunction() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testLocalInlineFunctionNameClash() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testLocalInlineFunctionComplex() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testArrayLiteralAliasing() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testLocalInlineFunctionReference() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testThisLiteralAliasing() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testIdentityEquals() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testVararg() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testMutualRecursion() throws Exception {
        try {
            checkFooBoxIsOkWithDirectives();
        } catch (InlineRecursionException e) {
            return;
        }

        throw new AssertionError("No exception was thrown for mutual recursion of inline functions");
    }

    public void testInlineOrder() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testCallableReference() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testCallableReferenceOfLocalInline() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testAnonymousObjectInlineMethod() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testClassObject() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testExtension() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testExtensionWithManyArguments() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testParams() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testRootConstructor() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testSeveralClosures() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testSeveralUsage() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testSimpleDouble() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testSimpleInt() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testSimpleEnum() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testSimpleLambda() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testSimpleObject() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testIncrementProperty() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }

    public void testSimpleReturnFunctionWithResultUnused() throws Exception {
        checkFooBoxIsOkWithDirectives();
    }
}
