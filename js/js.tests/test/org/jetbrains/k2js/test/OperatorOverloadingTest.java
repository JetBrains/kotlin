/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class OperatorOverloadingTest extends TranslationTest {

    public OperatorOverloadingTest() {
        super("operatorOverloading/");
    }

    public void testPlusOverload() throws Exception {
        checkFooBoxIsTrue("plusOverload.kt");
    }


    public void testPostfixInc() throws Exception {
        checkFooBoxIsTrue("postfixIncOverload.kt");
    }


    public void testPrefixDecOverload() throws Exception {
        checkFooBoxIsTrue("prefixDecOverload.kt");
    }


    public void testPrefixIncReturnsCorrectValue() throws Exception {
        checkFooBoxIsTrue("prefixIncReturnsCorrectValue.kt");
    }


    public void testOverloadedCallOnProperty() throws Exception {
        checkFooBoxIsTrue("overloadedCallOnProperty.kt");
    }


    public void testPostfixOnProperty() throws Exception {
        checkFooBoxIsTrue("postfixOnProperty.kt");
    }


    public void testOperatorOverloadOnPropertyCallGetterAndSetterOnlyOnce() throws Exception {
        checkFooBoxIsTrue("operatorOverloadOnPropertyCallGetterAndSetterOnlyOnce.kt");
    }


    public void testUnaryOnIntProperty() throws Exception {
        checkFooBoxIsTrue("unaryOnIntProperty.kt");
    }


    public void testUnaryOnIntPropertyAsStatement() throws Exception {
        checkFooBoxIsTrue("unaryOnIntProperty2.kt");
    }


    public void testBinaryDivOverload() throws Exception {
        checkFooBoxIsTrue("binaryDivOverload.kt");
    }


    public void testPlusAssignNoReassign() throws Exception {
        checkFooBoxIsTrue("plusAssignNoReassign.kt");
    }

    public void testNotOverload() throws Exception {
        checkFooBoxIsTrue("notOverload.kt");
    }


    public void testCompareTo() throws Exception {
        checkFooBoxIsTrue("compareTo.kt");
    }


    public void testPlusAndMinusAsAnExpression() throws Exception {
        checkFooBoxIsTrue("plusAndMinusAsAnExpression.kt");
    }


    public void testUsingModInCaseModAssignNotAvailable() throws Exception {
        checkFooBoxIsTrue("usingModInCaseModAssignNotAvailable.kt");
    }

    public void testOverloadPlusAssignArrayList() throws Exception {
        checkFooBoxIsOk("overloadPlusAssignArrayList.kt");
    }
}
