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
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testInlineGenericSimple() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testInlineIntSimple() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testInlineInc() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testInlineCallNoInline() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testInlineFunctionInLambda() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testInlineLambdaNoCapture() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testInlineLambdaWithCapture() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testInlineChain() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testInlineChainWithFewStatements() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testCallInlineFunctionOnTopLevelSimple() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testCallInlineFunctionOnTopLevel() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testInlineIf() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testInlineNoReturn() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testStatementsAfterReturn() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testLambdaReassignment() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testLambdaReassignmentWithCapture() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testInlineMethod() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testThisImplicitlyCaptured() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testAstCopy() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testNoInlineLambda() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testlambdaInLambda() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testInlineDefaultArgument() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testLocalInlineFunction() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testLocalInlineFunctionDeclaredInLambda() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testLocalInlineExtensionFunction() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testLocalInlineFunctionNameClash() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testLocalInlineFunctionComplex() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testArrayLiteralAliasing() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testLocalInlineFunctionReference() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testThisLiteralAliasing() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testIdentityEquals() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testVararg() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testMutualRecursion() throws Exception {
        try {
            checkFooBoxIsOkWithInlineDirectives();
        } catch (Exception e) {
            assert e.getCause() instanceof InlineRecursionException;
        }
    }

    public void testInlineOrder() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testCallableReference() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testCallableReferenceOfLocalInline() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testAnonymousObjectInlineMethod() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testLabelNameClashing() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testClassObject() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testExtension() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testExtensionWithManyArguments() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testParams() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testRootConstructor() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testSeveralClosures() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testSeveralUsage() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testSimpleDouble() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testSimpleInt() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testSimpleEnum() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testSimpleLambda() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testSimpleObject() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testIncrementProperty() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }

    public void testSimpleReturnFunctionWithResultUnused() throws Exception {
        checkFooBoxIsOkWithInlineDirectives();
    }
}
