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
        testWithMain("bottles", "");
    }


}
