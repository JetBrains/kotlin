package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public class RTTITest extends TranslationTest {

    final private static String MAIN = "rtti/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void isSameClass() throws Exception {
        testFooBoxIsTrue("isSameClass.kt");
    }

    @Test
    public void notIsOtherClass() throws Exception {
        testFooBoxIsTrue("notIsOtherClass.kt");
    }
}
