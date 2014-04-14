package org.jetbrains.k2js.test.semantics;

import org.jetbrains.k2js.test.SingleFileTranslationTest;

public class DataClassTest extends SingleFileTranslationTest {
    public DataClassTest() {
        super("dataClass/");
    }

    public void testSimple() throws Exception {
        checkFooBoxIsOk();
    }
}
