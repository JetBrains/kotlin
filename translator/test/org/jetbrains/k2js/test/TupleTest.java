package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 */
public final class TupleTest extends TranslationTest {

    final private static String MAIN = "tuple/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void twoElements() throws Exception {
        testFooBoxIsTrue("twoElements.kt");
    }

    @Test
    public void multipleMembers() throws Exception {
        testFooBoxIsTrue("multipleMembers.kt");
    }


}

