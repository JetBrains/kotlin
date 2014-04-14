package org.jetbrains.k2js.test.semantics;

import org.jetbrains.k2js.test.SingleFileTranslationTest;

public class DataClassTest extends SingleFileTranslationTest {
    public DataClassTest() {
        super("dataClass/");
    }

    public void testComponents() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCopy() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEquals() throws Exception {
        checkFooBoxIsOk();
    }

    public void testHashcode() throws Exception {
        checkFooBoxIsOk();
    }

    public void testTostring() throws Exception {
        checkFooBoxIsOk();
    }

    public void testKeyrole() throws Exception {
        checkFooBoxIsOk();
    }
}
