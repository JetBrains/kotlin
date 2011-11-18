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
}
