package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 */
public final class ExtensionPropertyTest extends TranslationTest {
    final private static String MAIN = "extensionProperty/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void simplePropertyWithGetter() throws Exception {
        testFooBoxIsTrue("simplePropertyWithGetter.kt");
    }


    @Test
    public void propertyWithGetterAndSetter() throws Exception {
        testFooBoxIsTrue("propertyWithGetterAndSetter.kt");
    }
}
