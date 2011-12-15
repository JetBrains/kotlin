package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class ExampleTest extends TranslationTest {

    final private static String MAIN = "examples/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void runBoxTest(String filename) throws Exception {
        testFunctionOutput(filename, "Anonymous", "box", "OK");
    }
}
