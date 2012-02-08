package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public class StringTest extends AbstractExpressionTest {

    final private static String MAIN = "string/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testStringConstant() throws Exception {
        testFooBoxIsTrue("stringConstant.kt");
    }

    public void testStringAssignment() throws Exception {
        testFooBoxIsTrue("stringAssignment.kt");
    }

    public void testIntInTemplate() throws Exception {
        testFunctionOutput("intInTemplate.kt", "foo", "box", "my age is 3");
    }

    public void testStringInTemplate() throws Exception {
        testFunctionOutput("stringInTemplate.kt", "foo", "box", "oHelloo");
    }

    public void testMultipleExpressionInTemplate() throws Exception {
        testFunctionOutput("multipleExpressionsInTemplate.kt", "foo", "box", "left = 3\nright = 2\nsum = 5\n");
    }

    public void testToStringMethod() throws Exception {
        testFooBoxIsTrue("objectToStringCallInTemplate.kt");
    }
}
