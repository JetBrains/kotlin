package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 */
public class BasicClassTest extends TranslationTest {

    final private static String MAIN = "class/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void classInstantiation() throws Exception {
        testFooBoxIsTrue("classInstantiation.kt");
    }

    @Test
    public void methodDeclarationAndCall() throws Exception {
        testFooBoxIsTrue("methodDeclarationAndCall.kt");
    }

    @Test
    public void constructorWithParameter() throws Exception {
        testFooBoxIsTrue("constructorWithParameter.kt");
    }

    @Test
    public void constructorWithPropertiesAsParameters() throws Exception {
        testFooBoxIsTrue("constructorWithPropertiesAsParameters.kt");
    }

    @Test
    public void incrementProperty() throws Exception {
        testFunctionOutput("incrementProperty.kt", "foo", "box", "OK");
    }

    @Test
    public void SimpleInitializer() throws Exception {
        testFooBoxIsTrue("simpleInitializer.kt");
    }

    @Test
    public void complexExpressionAsConstructorParameter() throws Exception {
        testFooBoxIsTrue("complexExpressionAsConstructorParameter.kt");
    }

    @Test
    public void propertyAccess() throws Exception {
        testFooBoxIsTrue("propertyAccess.kt");
    }

    @Test
    public void propertiesAsParametersInitialized() throws Exception {
        testFooBoxIsTrue("propertiesAsParametersInitialized.kt");
    }

    @Test
    public void classWithoutNamespace() throws Exception {
        testFunctionOutput("classWithoutNamespace.kt", "Anonymous", "box", true);
    }
}
