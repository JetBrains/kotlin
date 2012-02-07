package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class RangeTest extends TranslationTest {

    final private static String MAIN = "range/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }


    public void testExplicitRange() throws Exception {
        testFooBoxIsTrue("explicitRange.kt");
    }


    public void testRangeSugarSyntax() throws Exception {
        testFooBoxIsTrue("rangeSugarSyntax.kt");
    }


    public void testIntInRange() throws Exception {
        testFooBoxIsTrue("intInRange.kt");
    }

}
