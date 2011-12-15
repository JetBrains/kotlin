package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 */
public final class WebDemoExamplesTest extends TranslationTest {

    final private static String MAIN = "webDemoExamples/";

    @Override
    protected String mainDirectory() {
        return MAIN;
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

    @Test
    public void ranges() throws Exception {
        checkOutput("ranges.kt", "OK\n" +
                " 1 2 3 4 5\n" +
                "Out: array has only 3 elements. x = 4\n" +
                "Yes: array contains aaa\n" +
                "No: array doesn't contains ddd\n", "4");

        checkOutput("ranges.kt", " 1 2 3 4 5\n" +
                "Out: array has only 3 elements. x = 10\n" +
                "Yes: array contains aaa\n" +
                "No: array doesn't contains ddd\n", "10");
    }

    @Test
    public void forLoop() throws Exception {
        checkOutput("forLoop.kt", "a\n" +
                "b\n" +
                "c\n" +
                "\n" +
                "a\n" +
                "b\n" +
                "c\n", "a", "b", "c");
        checkOutput("forLoop.kt", "123\n\n123\n", "123");
    }

    @Test
    public void isCheck() throws Exception {
        checkOutput("isCheck.kt", "3\nnull\n");
    }

    @Test
    public void patternMatching() throws Exception {
        checkOutput("patternMatching.kt", "Greeting\n" +
                "One\n" +
                "Not a string\n" +
                "Unknown\n");
    }


}
