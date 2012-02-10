package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class PropertyAccessTest extends TranslationTest {

    final private static String MAIN = "propertyAccess/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }


    public void testAccessToInstanceProperty() throws Exception {
        testFooBoxIsTrue("accessToInstanceProperty.kt");
    }


    public void testTwoClassesWithProperties() throws Exception {
        testFooBoxIsTrue("twoClassesWithProperties.kt");
    }


    public void testSetter() throws Exception {
        testFunctionOutput("setter.kt", "foo", "f", 99.0);
    }


    public void testCustomGetter() throws Exception {
        testFooBoxIsTrue("customGetter.kt");
    }


    public void testCustomSetter() throws Exception {
        testFooBoxIsTrue("customSetter.kt");
    }


    public void testSafeCall() throws Exception {
        testFooBoxIsTrue("safeCall.kt");
    }


    public void testSafeCallReturnsNullIfFails() throws Exception {
        testFooBoxIsTrue("safeCallReturnsNullIfFails.kt");
    }


    public void testNamespacePropertyInitializer() throws Exception {
        testFooBoxIsTrue("namespacePropertyInitializer.kt");
    }


    public void testNamespacePropertySet() throws Exception {
        testFooBoxIsTrue("namespacePropertySet.kt");
    }

    public void testNamespaceCustomAccessors() throws Exception {
        testFooBoxIsTrue("namespaceCustomAccessors.kt");
    }


    public void testClassUsesNamespaceProperties() throws Exception {
        testFooBoxIsTrue("classUsesNamespaceProperties.kt");
    }


    public void testSafeAccess() throws Exception {
        testFooBoxIsTrue("safeAccess.kt");
    }

}
