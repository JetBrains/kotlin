package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
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
        testFooBoxIsOk("closureWithParameter.kt");
    }

    @Test
    public void closureWithParameterAndBoxing() throws Exception {
        testFooBoxIsOk("closureWithParameterAndBoxing.jet");
    }

    @Test
    public void enclosingThis() throws Exception {
        testFunctionOutput("enclosingThis.kt", "Anonymous", "box", "OK");
    }

    @Test
    public void implicitItParameter() throws Exception {
        testFooBoxIsTrue("implicitItParameter.kt");
    }

    @Test
    public void defaultParameters() throws Exception {
        testFooBoxIsTrue("defaultParameters.kt");
    }

    @Test
    public void functionLiteralAsLastParameter() throws Exception {
        testFooBoxIsTrue("functionLiteralAsLastParameter.kt");
    }

    @Test
    public void namedArguments() throws Exception {
        testFooBoxIsTrue("namedArguments.kt");
    }

    @Test
    public void kt921() throws Exception {
        try {
            checkOutput("KT-921.kt", "");
        } catch (Throwable e) {
            System.out.println(e);
        }
    }


}
