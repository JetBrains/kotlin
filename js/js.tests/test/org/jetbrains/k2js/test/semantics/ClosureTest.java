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

    public void testSimpleRecursion() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureFunctionByInnerFunction() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureLocalFunctionByInnerFunction() throws Exception {
        checkFooBoxIsOk();
    }

    // TODO: fix
    public void igonre_testClosureLocalFunctionByInnerFunctionInConstrunctor() throws Exception {
        checkFooBoxIsOk();
    }
}
