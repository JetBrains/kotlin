package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public class StringTest extends AbstractExpressionTest {

    final private static String MAIN = "string/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void stringConstant() throws Exception {
        testFooBoxIsTrue("stringConstant.kt");
    }

    @Test
    public void stringAssignment() throws Exception {
        testFooBoxIsTrue("stringAssignment.kt");
    }
}
