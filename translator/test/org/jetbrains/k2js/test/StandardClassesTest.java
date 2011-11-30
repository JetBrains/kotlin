package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public class StandardClassesTest extends TranslationTest {
    final private static String MAIN = "standardClasses/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void array() throws Exception {
        testFooBoxIsTrue("array.kt");
    }

    @Test
    public void arrayAccess() throws Exception {
        testFooBoxIsTrue("arrayAccess.kt");
    }
}
