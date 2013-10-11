package org.jetbrains.k2js.test.semantics;

import org.jetbrains.k2js.test.SingleFileTranslationTest;

public final class ClassObjectTest extends SingleFileTranslationTest {

    public ClassObjectTest() {
        super("classObject/");
    }

    public void testSimple() throws Exception {
        checkFooBoxIsOk();
    }

    public void testInTrait() throws Exception {
        checkFooBoxIsOk();
    }

    public void testWithExtension() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSetVar() throws Exception {
        checkFooBoxIsOk();
    }
}
