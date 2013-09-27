package org.jetbrains.k2js.test.semantics;

import org.jetbrains.k2js.test.SingleFileTranslationTest;

public class DefaultArgumentsTest extends SingleFileTranslationTest {

    public DefaultArgumentsTest() {
        super("defaultArguments/");
    }

    public void testConstructorCallWithDefArg1() throws Exception {
        checkFooBoxIsOk();
    }

    public void testConstructorCallWithDefArg2() throws Exception {
        checkFooBoxIsOk();
    }

    public void testDefArgsWithSuperCall() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEnumWithDefArg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEnumWithOneDefArg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEnumWithTwoDefArgs() throws Exception {
        checkFooBoxIsOk();
    }

    public void testExtensionFunWithDefArgs() throws Exception {
        checkFooBoxIsOk();
    }

    public void testFunInAbstractClassWithDefArg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testOverloadFunWithDefArg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testOverrideValWithDefaultValue() throws Exception {
        checkFooBoxIsOk();
    }

    public void testVirtualCallWithDefArg() throws Exception {
        checkFooBoxIsOk();
    }



}
