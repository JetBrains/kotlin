package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public final class WhileTest extends AbstractExpressionTest {

    private static final String MAIN = "while/";

    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void whileSimpleTest() throws Exception {
        testFooBoxIsTrue("while.kt");
    }

    @Test
    public void doWhileSimpleTest() throws Exception {
        testFooBoxIsTrue("doWhile.kt");
    }

    @Test
    public void doWhileExecutesAtLeastOnce() throws Exception {
        testFooBoxIsTrue("doWhile2.kt");
    }

    @Test
    public void whileDoesntExecuteEvenOnceIfConditionIsFalse() throws Exception {
        testFooBoxIsTrue("while2.kt");
    }
}
