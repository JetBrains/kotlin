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

    @Test
    public void rangeSugarSyntax() throws Exception {
        testFooBoxIsTrue("rangeSugarSyntax.kt");
    }

    @Test
    public void intInRange() throws Exception {
        testFooBoxIsTrue("intInRange.kt");
    }


}
