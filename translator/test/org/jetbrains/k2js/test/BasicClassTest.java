package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
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

    //TODO: wait for bugfix and implement properties as constructor parameter declaration
    @Test
    public void constructorWithParameter() throws Exception {
        testFooBoxIsTrue("constructorWithParameter.kt");
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
}
