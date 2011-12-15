package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 */
public class StringTest extends AbstractExpressionTest {

    final private static String MAIN = "string/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void stringConstant() throws Exception {
        testFooBoxIsTrue("stringConstant.kt");
    }

    @Test
    public void stringAssignment() throws Exception {
        testFooBoxIsTrue("stringAssignment.kt");
    }

    @Test
    public void intInTemplate() throws Exception {
        testFunctionOutput("intInTemplate.kt", "foo", "box", "my age is 3");
    }

    @Test
    public void stringInTemplate() throws Exception {
        testFunctionOutput("stringInTemplate.kt", "foo", "box", "oHelloo");
    }

    @Test
    public void multipleExpressionInTemplate() throws Exception {
        testFunctionOutput("multipleExpressionsInTemplate.kt", "foo", "box", "left = 3\nright = 2\nsum = 5\n");
    }
}
