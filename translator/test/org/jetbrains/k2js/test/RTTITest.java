package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public class RTTITest extends TranslationTest {

    final private static String MAIN = "rtti/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testIsSameClass() throws Exception {
        testFooBoxIsTrue("isSameClass.kt");
    }

    public void testNotIsOtherClass() throws Exception {
        testFooBoxIsTrue("notIsOtherClass.kt");
    }
}
