package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class ExtensionPropertyTest extends TranslationTest {
    final private static String MAIN = "extensionProperty/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testSimplePropertyWithGetter() throws Exception {
        testFooBoxIsTrue("simplePropertyWithGetter.kt");
    }

    public void testPropertyWithGetterAndSetter() throws Exception {
        testFooBoxIsTrue("propertyWithGetterAndSetter.kt");
    }

    public void testAbsExtension() throws Exception {
        testFooBoxIsTrue("absExtension.kt");
    }
}
