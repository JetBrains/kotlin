package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 */
public class ObjectTest extends TranslationTest {

    private static final String MAIN = "object/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void objectWithMethods() throws Exception {
        testFooBoxIsTrue("objectWithMethods.kt");
    }
}