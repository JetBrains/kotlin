package org.jetbrains.k2js.test;

import org.junit.Test;
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

    @Test
    public void ifElseAsExpression() throws Exception {
        testFooBoxIsTrue("ifElseAsExpression.kt");
    }

    @Test
    public void ifElse() throws Exception {
        testFunctionOutput("if.kt", "foo", "box", 5);
    }

    @Test
    public void ifElseIf() throws Exception {
        testFunctionOutput("elseif.kt", "foo", "box", 5);
    }

    @Test(expected = JavaScriptException.class)
    public void ifElseAsExpressionWithThrow() throws Exception {
        testFooBoxIsTrue("ifAsExpressionWithThrow.kt");
    }
}
