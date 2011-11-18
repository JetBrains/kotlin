package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public class ConditionalTest extends AbstractExpressionTest {
    final private static String MAIN = "conditional/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void ifElseAsExpression() throws Exception {
        testFooBoxIsTrue("ifElseAsExpression.kt");
    }

    @Test
    public void ifElse() throws Exception {
        testFunctionOutput("if.kt", "foo", "box", 5);
    }

    //TODO: test fails due to isStatement issue, include when issue is solved or implement another solution
//    @Test
//    public void ifElseIf() throws Exception {
//        testFunctionOutput("elseif.kt", "foo", "box", 5);
//    }
}
