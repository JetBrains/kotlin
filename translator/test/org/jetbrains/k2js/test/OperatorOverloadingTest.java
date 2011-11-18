package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public class OperatorOverloadingTest extends IncludeLibraryTest {

    final private static String MAIN = "operatorOverloading/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void plusOverload() throws Exception {
        testFooBoxIsTrue("plusOverload.kt");
    }
}
