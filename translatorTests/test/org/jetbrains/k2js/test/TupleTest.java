package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class TupleTest extends TranslationTest {

    final private static String MAIN = "tuple/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testTwoElements() throws Exception {
        testFooBoxIsTrue("twoElements.kt");
    }

    public void testMultipleMembers() throws Exception {
        testFooBoxIsTrue("multipleMembers.kt");
    }


}

