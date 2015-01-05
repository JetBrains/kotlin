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

import org.jetbrains.kotlin.js.test.SingleFileTranslationTest;

public final class OperatorOverloadingTest extends SingleFileTranslationTest {

    public OperatorOverloadingTest() {
        super("operatorOverloading/");
    }

    public void testPlusOverload() throws Exception {
        fooBoxTest();
    }


    public void testPostfixInc() throws Exception {
        fooBoxTest();
    }


    public void testPrefixDecOverload() throws Exception {
        fooBoxTest();
    }


    public void testPrefixIncReturnsCorrectValue() throws Exception {
        fooBoxTest();
    }


    public void testOverloadedCallOnProperty() throws Exception {
        fooBoxTest();
    }


    public void testPostfixOnProperty() throws Exception {
        fooBoxTest();
    }


    public void testOperatorOverloadOnPropertyCallGetterAndSetterOnlyOnce() throws Exception {
        fooBoxTest();
    }


    public void testUnaryOnIntProperty() throws Exception {
        fooBoxTest();
    }


    public void testUnaryOnIntPropertyAsStatement() throws Exception {
        fooBoxTest();
    }


    public void testBinaryDivOverload() throws Exception {
        fooBoxTest();
    }


    public void testPlusAssignNoReassign() throws Exception {
        fooBoxTest();
    }

    public void testNotOverload() throws Exception {
        fooBoxTest();
    }


    public void testCompareTo() throws Exception {
        fooBoxTest();
    }

    public void testCompareToByName() throws Exception {
        fooBoxTest();
    }


    public void testPlusAndMinusAsAnExpression() throws Exception {
        fooBoxTest();
    }


    public void testUsingModInCaseModAssignNotAvailable() throws Exception {
        fooBoxTest();
    }

    public void testOverloadPlusAssignArrayList() throws Exception {
        checkFooBoxIsOk("overloadPlusAssignArrayList.kt");
    }

    public void testOverloadPlusAssignViaExtensionFunction() throws Exception {
        fooBoxTest();
    }

    public void testOverloadPlusViaExtensionFunction() throws Exception {
        fooBoxTest();
    }

    public void testOverloadPlusAssignViaPlusExtensionFunction() throws Exception {
        fooBoxTest();
    }

    public void testOverloadUnaryOperationsViaExtensionFunctions() throws Exception {
        fooBoxTest();
    }

    public void testOverloadByLambda() throws Exception {
        checkFooBoxIsOk();
    }
}
