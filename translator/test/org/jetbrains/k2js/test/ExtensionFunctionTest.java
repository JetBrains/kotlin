package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public final class ExtensionFunctionTest extends TranslationTest {
    final private static String MAIN = "extensionFunction/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void intExtension() throws Exception {
        testFooBoxIsTrue("intExtension.kt");
    }

    @Test
    public void extensionWithImplicitReceiver() throws Exception {
        testFooBoxIsTrue("extensionWithImplicitReceiver.kt");
    }

    @Test
    public void extensionFunctionOnExpression() throws Exception {
        testFooBoxIsTrue("extensionFunctionOnExpression.kt");
    }

    @Test
    public void extensionUsedInsideClass() throws Exception {
        testFooBoxIsTrue("extensionUsedInsideClass.kt");
    }
}
