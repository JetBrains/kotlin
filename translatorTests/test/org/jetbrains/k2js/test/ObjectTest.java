package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class ObjectTest extends TranslationTest {

    private static final String MAIN = "object/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }


    public void testObjectWithMethods() throws Exception {
        testFooBoxIsTrue("objectWithMethods.kt");
    }


    public void testObjectDeclaration() throws Exception {
        testFooBoxIsTrue("objectDeclaration.kt");
    }


    public void testObjectInMethod() throws Exception {
        testFooBoxIsTrue("objectInMethod.kt");
    }
}