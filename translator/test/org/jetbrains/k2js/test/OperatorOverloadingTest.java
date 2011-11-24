package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public class OperatorOverloadingTest extends TranslationTest {

    final private static String MAIN = "operatorOverloading/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void plusOverload() throws Exception {
        testFooBoxIsTrue("plusOverload.kt");
    }

    @Test
    public void postfixInc() throws Exception {
        testFooBoxIsTrue("postfixIncOverload.kt");
    }

    @Test
    public void prefixInc() throws Exception {
        testFooBoxIsTrue("prefixDecOverload.kt");
    }

    @Test
    public void prefixIncReturnsCorrectValue() throws Exception {
        testFooBoxIsTrue("prefixIncReturnsCorrectValue.kt");
    }

    @Test
    public void overloadedCallOnProperty() throws Exception {
        testFooBoxIsTrue("overloadedCallOnProperty.kt");
    }

    @Test
    public void postfixOnProperty() throws Exception {
        testFooBoxIsTrue("postfixOnProperty.kt");
    }

    @Test
    public void operatorOverloadOnPropertyCallGetterAndSetterOnlyOnce() throws Exception {
        testFooBoxIsTrue("operatorOverloadOnPropertyCallGetterAndSetterOnlyOnce.kt");
    }

    @Test
    public void unaryOnIntProperty() throws Exception {
        testFooBoxIsTrue("unaryOnIntProperty.kt");
    }

    @Test
    public void unaryOnIntPropertyAsStatement() throws Exception {
        testFooBoxIsTrue("unaryOnIntProperty2.kt");
    }
}
