package org.jetbrains.k2js.test;

import org.mozilla.javascript.JavaScriptException;

/**
 * @author Pavel Talanov
 */
public class ConditionalTest extends AbstractExpressionTest {

    final private static String MAIN = "conditional/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testIfElseAsExpression() throws Exception {
        testFooBoxIsTrue("ifElseAsExpression.kt");
    }

    public void testIfElse() throws Exception {
        testFunctionOutput("if.kt", "foo", "box", 5);
    }

    public void testIfElseIf() throws Exception {
        testFunctionOutput("elseif.kt", "foo", "box", 5);
    }

    public void testIfElseAsExpressionWithThrow() throws Exception {
        try {
            testFooBoxIsTrue("ifAsExpressionWithThrow.kt");
            fail();
        } catch (JavaScriptException e) {

        }
    }
}
