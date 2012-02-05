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


    public void testPrefixInc() throws Exception {
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


    public void testPlusAssignReassign() throws Exception {
        testFooBoxIsTrue("plusAssignReassign.kt");
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
