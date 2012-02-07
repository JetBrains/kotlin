package org.jetbrains.k2js.test;

import org.mozilla.javascript.JavaScriptException;

/**
 * @author Pavel Talanov
 */
//TODO: remove class
public class ConditionalTest extends AbstractExpressionTest {

    final private static String MAIN = "conditional/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testIfElseAsExpressionWithThrow() throws Exception {
        try {
            testFooBoxIsTrue("ifAsExpressionWithThrow.kt");
            fail();
        } catch (JavaScriptException e) {

        }
    }
}
