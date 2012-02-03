package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 */
public final class ObjectTest extends TranslationTest {

    private static final String MAIN = "object/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void objectWithMethods() throws Exception {
        testFooBoxIsTrue("objectWithMethods.kt");
    }

    @Test
    public void objectDeclaration() throws Exception {
        testFooBoxIsTrue("objectDeclaration.kt");
    }

    @Test
    public void objectInMethod() throws Exception {
        testFooBoxIsTrue("objectInMethod.kt");
    }
}