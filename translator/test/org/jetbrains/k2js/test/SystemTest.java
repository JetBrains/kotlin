package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public final class SystemTest extends JavaClassesTest {

    final private static String MAIN = "system/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void systemPrint() throws Exception {
        checkOutput("systemPrint.kt", "Hello, world!");
    }

    @Test
    public void printArg() throws Exception {
        checkOutput("printArg.kt", "Hello, world!", "Hello, world!");
    }

    @Test
    public void whileLoop() throws Exception {
        checkOutput("whileLoop.kt", "guest1\nguest2\nguest3\nguest4\n", "guest1", "guest2", "guest3", "guest4");
    }

    @Test
    public void ifAsExpression() throws Exception {
        checkOutput("ifAsExpression.kt", "20\n", "10", "20");
    }

    @Test
    public void objectOrientedHello() throws Exception {
        checkOutput("objectOrientedHello.kt", "Hello, Pavel!\n", "Pavel");
    }

    @Test
    public void multiLanguageHello() throws Exception {
        checkOutput("multiLanguageHello.kt", "Salut!\n", "FR");
    }

    @Test
    public void nullChecks() throws Exception {
        checkOutput("nullChecks.kt", "No number supplied");
        checkOutput("nullChecks.kt", "6", "2", "3");
    }


}
