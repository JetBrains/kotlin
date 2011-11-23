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

    @Test
    public void breakWhile() throws Exception {
        testFooBoxIsTrue("breakWhile.kt");
    }

    @Test
    public void breakDoWhile() throws Exception {
        testFooBoxIsTrue("breakDoWhile.kt");
    }

    @Test
    public void continueWhile() throws Exception {
        testFooBoxIsTrue("continueWhile.kt");
    }

    @Test
    public void continueDoWhile() throws Exception {
        testFooBoxIsTrue("continueDoWhile.kt");
    }
}
