package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 */
public final class StdlibTest extends TranslationTest {
    final private static String MAIN = "stdlib/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    //TODO: test!
    @Test
    public void filter() throws Exception {
        // testFooBoxIsTrue("Filter.kt");
    }
}
