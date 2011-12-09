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

    @Test
    public void virtualExtension() throws Exception {
        testFooBoxIsTrue("virtualExtension.kt");
    }

    @Test
    public void virtualExtensionOverride() throws Exception {
        testFooBoxIsTrue("virtualExtensionOverride.kt");
    }

    @Test
    public void extensionLiteralPassedToFunction() throws Exception {
        testFooBoxIsTrue("extensionLiteralPassedToFunction.kt");
    }
}
