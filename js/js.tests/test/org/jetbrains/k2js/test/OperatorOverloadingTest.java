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

    final private static String MAIN = "operatorOverloading/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }


    public void testPlusOverload() throws Exception {
        testFooBoxIsTrue("plusOverload.kt");
    }


    public void testPostfixInc() throws Exception {
        testFooBoxIsTrue("postfixIncOverload.kt");
    }


    public void testPrefixDecOverload() throws Exception {
        testFooBoxIsTrue("prefixDecOverload.kt");
    }


    public void testPrefixIncReturnsCorrectValue() throws Exception {
        testFooBoxIsTrue("prefixIncReturnsCorrectValue.kt");
    }


    public void testOverloadedCallOnProperty() throws Exception {
        testFooBoxIsTrue("overloadedCallOnProperty.kt");
    }


    public void testPostfixOnProperty() throws Exception {
        testFooBoxIsTrue("postfixOnProperty.kt");
    }


    public void testOperatorOverloadOnPropertyCallGetterAndSetterOnlyOnce() throws Exception {
        testFooBoxIsTrue("operatorOverloadOnPropertyCallGetterAndSetterOnlyOnce.kt");
    }


    public void testUnaryOnIntProperty() throws Exception {
        testFooBoxIsTrue("unaryOnIntProperty.kt");
    }


    public void testUnaryOnIntPropertyAsStatement() throws Exception {
        testFooBoxIsTrue("unaryOnIntProperty2.kt");
    }


    public void testBinaryDivOverload() throws Exception {
        testFooBoxIsTrue("binaryDivOverload.kt");
    }


    public void testPlusAssignNoReassign() throws Exception {
        testFooBoxIsTrue("plusAssignNoReassign.kt");
    }

    public void testNotOverload() throws Exception {
        testFooBoxIsTrue("notOverload.kt");
    }


    public void testCompareTo() throws Exception {
        testFooBoxIsTrue("compareTo.kt");
    }


    public void testPlusAndMinusAsAnExpression() throws Exception {
        testFooBoxIsTrue("plusAndMinusAsAnExpression.kt");
    }


    public void testUsingModInCaseModAssignNotAvailable() throws Exception {
        testFooBoxIsTrue("usingModInCaseModAssignNotAvailable.kt");
    }

    public void testOverloadPlusAssignArrayList() throws Exception {
        testFooBoxIsOk("overloadPlusAssignArrayList.kt");
    }
}
