package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class OperationTest extends AbstractExpressionTest {
    final private static String MAIN = "operation/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }


    public void testPrefixIntOperations() throws Exception {
        testFooBoxIsTrue("prefixIntOperations.kt");
    }


    public void testPostfixIntOperations() throws Exception {
        testFooBoxIsTrue("postfixIntOperations.kt");
    }


    public void testNotBoolean() throws Exception {
        testFooBoxIsTrue("notBoolean.kt");
    }


    public void testPositiveAndNegativeNumbers() throws Exception {
        testFooBoxIsTrue("positiveAndNegativeNumbers.kt");
    }


    public void testAssign() throws Exception {
        testFunctionOutput("assign.kt", "foo", "f", 2.0);
    }


    public void testComparison() throws Exception {
        testFooBoxIsTrue("comparison.kt");
    }
}

