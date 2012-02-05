package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class WhileTest extends AbstractExpressionTest {

    private static final String MAIN = "while/";

    protected String mainDirectory() {
        return MAIN;
    }

    public void testWhileSimpleTest() throws Exception {
        testFooBoxIsTrue("while.kt");
    }

    public void testDoWhileSimpleTest() throws Exception {
        testFooBoxIsTrue("doWhile.kt");
    }

    public void testDoWhileExecutesAtLeastOnce() throws Exception {
        testFooBoxIsTrue("doWhile2.kt");
    }

    public void testWhileDoesntExecuteEvenOnceIfConditionIsFalse() throws Exception {
        testFooBoxIsTrue("while2.kt");
    }

    public void testBreakWhile() throws Exception {
        testFooBoxIsTrue("breakWhile.kt");
    }


    public void testBreakDoWhile() throws Exception {
        testFooBoxIsTrue("breakDoWhile.kt");
    }


    public void testContinueWhile() throws Exception {
        testFooBoxIsTrue("continueWhile.kt");
    }


    public void testContinueDoWhile() throws Exception {
        testFooBoxIsTrue("continueDoWhile.kt");
    }
}
