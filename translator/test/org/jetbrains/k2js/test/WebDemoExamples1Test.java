package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class WebDemoExamples1Test extends TranslationTest {

    final private static String MAIN = "webDemoExamples1/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }


    public void testPrintArg() throws Exception {
        checkOutput("printArg.kt", "Hello, world!", "Hello, world!");
    }


    public void testWhileLoop() throws Exception {
        checkOutput("whileLoop.kt", "guest1\nguest2\nguest3\nguest4\n", "guest1", "guest2", "guest3", "guest4");
    }


    public void testIfAsExpression() throws Exception {
        checkOutput("ifAsExpression.kt", "20\n", "10", "20");
    }


    public void testObjectOrientedHello() throws Exception {
        checkOutput("objectOrientedHello.kt", "Hello, Pavel!\n", "Pavel");
    }


    public void testMultiLanguageHello() throws Exception {
        checkOutput("multiLanguageHello.kt", "Salut!\n", "FR");
    }


    public void testNullChecks() throws Exception {
        checkOutput("nullChecks.kt", "No number supplied");
        checkOutput("nullChecks.kt", "6", "2", "3");
    }


    public void testRanges() throws Exception {
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


    public void testForLoop() throws Exception {
        checkOutput("forLoop.kt", "a\n" +
                "b\n" +
                "c\n" +
                "\n" +
                "a\n" +
                "b\n" +
                "c\n", "a", "b", "c");
        checkOutput("forLoop.kt", "123\n\n123\n", "123");
    }


    public void testIsCheck() throws Exception {
        checkOutput("isCheck.kt", "3\nnull\n");
    }


    public void testPatternMatching() throws Exception {
        checkOutput("patternMatching.kt", "Greeting\n" +
                "One\n" +
                "Not a string\n" +
                "Unknown\n");
    }
}
