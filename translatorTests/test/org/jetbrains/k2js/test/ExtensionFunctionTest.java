package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class ExtensionFunctionTest extends TranslationTest {
    final private static String MAIN = "extensionFunction/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testIntExtension() throws Exception {
        testFooBoxIsTrue("intExtension.kt");
    }

    public void testExtensionWithImplicitReceiver() throws Exception {
        testFooBoxIsTrue("extensionWithImplicitReceiver.kt");
    }

    public void testExtensionFunctionOnExpression() throws Exception {
        testFooBoxIsTrue("extensionFunctionOnExpression.kt");
    }

    public void testExtensionUsedInsideClass() throws Exception {
        testFooBoxIsTrue("extensionUsedInsideClass.kt");
    }

    public void testVirtualExtension() throws Exception {
        testFooBoxIsTrue("virtualExtension.kt");
    }

    public void testVirtualExtensionOverride() throws Exception {
        testFooBoxIsTrue("virtualExtensionOverride.kt");
    }

    public void testExtensionLiteralPassedToFunction() throws Exception {
        testFooBoxIsTrue("extensionLiteralPassedToFunction.kt");
    }

    public void testExtensionInsideFunctionLiteral() throws Exception {
        testFooBoxIsTrue("extensionInsideFunctionLiteral.kt");
    }

    public void testGenericExtension() throws Exception {
        testFooBoxIsOk("generic.kt");
    }
}
