package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public class BasicClassTest extends TranslationTest {

    final private static String MAIN = "class/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }


    public void testClassInstantiation() throws Exception {
        testFooBoxIsTrue("classInstantiation.kt");
    }

    public void testMethodDeclarationAndCall() throws Exception {
        testFooBoxIsTrue("methodDeclarationAndCall.kt");
    }

    public void testConstructorWithParameter() throws Exception {
        testFooBoxIsTrue("constructorWithParameter.kt");
    }

    public void testConstructorWithPropertiesAsParameters() throws Exception {
        testFooBoxIsTrue("constructorWithPropertiesAsParameters.kt");
    }

    public void testIncrementProperty() throws Exception {
        testFunctionOutput("incrementProperty.kt", "foo", "box", "OK");
    }

    public void testSimpleInitializer() throws Exception {
        testFooBoxIsTrue("simpleInitializer.kt");
    }

    public void testComplexExpressionAsConstructorParameter() throws Exception {
        testFooBoxIsTrue("complexExpressionAsConstructorParameter.kt");
    }

    public void testPropertyAccess() throws Exception {
        testFooBoxIsTrue("propertyAccess.kt");
    }

    public void testPropertiesAsParametersInitialized() throws Exception {
        testFooBoxIsTrue("propertiesAsParametersInitialized.kt");
    }

    public void testClassWithoutNamespace() throws Exception {
        testFunctionOutput("classWithoutNamespace.kt", "Anonymous", "box", true);
    }
}
