package org.jetbrains.k2js.test;

import org.jetbrains.k2js.K2JSTranslatorApplet;
import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public final class AppletTest extends TranslationTest {

    final private static String MAIN = "applet/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void simpleTest() throws Exception {
        (new K2JSTranslatorApplet()).translate("fun main(args : Array<String>) {}", " a 3 1   2134");
    }
}
