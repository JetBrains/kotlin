package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public final class PropertyAccessorTest extends AbstractClassTest {

    final private static String MAIN = "propertyAccess/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void accessToInstanceProperty() throws Exception {
        testFooBoxIsTrue("accessToInstanceProperty.kt");
    }

    @Test
    public void twoClassesWithProperties() throws Exception {
        testFooBoxIsTrue("twoClassesWithProperties.kt");
    }

    @Test
    public void setter() throws Exception {
        testFunctionOutput("setter.kt", "foo", "f", 99.0);
    }

    @Test
    public void customGetter() throws Exception {
        testFooBoxIsTrue("customGetter.kt");
    }

    @Test
    public void customSetter() throws Exception {
        testFooBoxIsTrue("customSetter.kt");
    }

    @Test
    public void safeCall() throws Exception {
        testFooBoxIsTrue("safeCall.kt");
    }

    //TODO test
//    @Test
//    public void namespaceCustomAccessors() throws Exception {
//        testFooBoxIsTrue("namespaceCustomAccessors.kt");
//    }

}
