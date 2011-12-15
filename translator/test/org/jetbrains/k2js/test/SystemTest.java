package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 */
public final class SystemTest extends JavaClassesTest {

    final private static String MAIN = "system/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void systemPrint() throws Exception {
        checkOutput("print.kt", "Hello, world!");
    }

    @Test
    public void systemPrintln() throws Exception {
        checkOutput("println.kt", "Hello, world!\n3\n\n");
    }
}
