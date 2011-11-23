package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public class OperationTest extends AbstractExpressionTest {
    final private static String MAIN = "operation/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void prefixIntOperations() throws Exception {
        testFooBoxIsTrue("prefixIntOperations.kt");
    }

    @Test
    public void postfixIntOperations() throws Exception {
        testFooBoxIsTrue("postfixIntOperations.kt");
    }

    @Test
    public void notBoolean() throws Exception {
        testFooBoxIsTrue("notBoolean.kt");
    }

    @Test
    public void positiveAndNegativeNumbers() throws Exception {
        testFooBoxIsTrue("positiveAndNegativeNumbers.kt");
    }

    @Test
    public void assign() throws Exception {
        testFunctionOutput("assign.jet", "foo", "f", 2.0);
    }

    @Test
    public void comparison() throws Exception {
        testFooBoxIsTrue("comparison.kt");
    }
}
