package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public final class RangeTest extends TranslationTest {

    final private static String MAIN = "range/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void explicitRange() throws Exception {
        testFooBoxIsTrue("explicitRange.kt");
    }


}
