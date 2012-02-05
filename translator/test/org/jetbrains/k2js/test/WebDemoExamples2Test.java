package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class WebDemoExamples2Test extends TranslationTest {

    final private static String MAIN = "webDemoExamples2/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testBottles() throws Exception {
        testWithMain("bottles", "2", "2");
        testWithMain("bottles", "");
    }

    public void testLife() throws Exception {
        testWithMain("life", "", "2");
    }

    public void testBuilder() throws Exception {
        testWithMain("builder", "");
        testWithMain("builder", "1", "over9000");
    }

    //TODO: comparator LinkedList dependencies
//    @Test
//    public void maze() throws Exception {
//        testWithMain("maze", "");
//    }
}
