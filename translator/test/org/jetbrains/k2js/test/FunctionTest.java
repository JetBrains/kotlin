package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public class FunctionTest extends AbstractExpressionTest {

    final private static String MAIN = "function/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void functionUsedBeforeDeclaration() throws Exception {
        testFooBoxIsTrue("functionUsedBeforeDeclaration.kt");
    }

    @Test
    public void functionWithTwoParametersCall() throws Exception {
        testFooBoxIsTrue("functionWithTwoParametersCall.kt");
    }

    @Test
    public void functionLiteral() throws Exception {
        testFooBoxIsTrue("functionLiteral.kt");
    }

    @Test
    public void adderClosure() throws Exception {
        testFooBoxIsTrue("adderClosure.kt");
    }

    @Test
    public void loopClosure() throws Exception {
        testFooBoxIsTrue("loopClosure.kt");
    }

    @Test
    public void functionLiteralAsParameter() throws Exception {
        testFooBoxIsTrue("functionLiteralAsParameter.kt");
    }

    @Test
    public void closureWithParameter() throws Exception {
        testFooBoxIsOk("closureWithParameter.jet");
    }

    @Test
    public void closureWithParameterAndBoxing() throws Exception {
        testFooBoxIsOk("closureWithParameterAndBoxing.jet");
    }


}
