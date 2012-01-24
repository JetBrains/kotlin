package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 */
public final class WebDemoExamples2Test extends TranslationTest {

    final private static String MAIN = "webDemoExamples2/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void bottles() throws Exception {
        testWithMain("bottles", "2", "2");
        testWithMain("bottles", "");
    }

    //TODO: a couple of classes not supported
//    @Test
//    public void life() throws Exception {
//        testWithMain("life", "", "2");
//    }

    @Test
    public void builder() throws Exception {
        testWithMain("builder", "");
        testWithMain("builder", "1", "over9000");
    }
}
