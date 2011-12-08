package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public final class ForTest extends AbstractExpressionTest {

    final private static String MAIN = "for/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void forIteratesOverArray() throws Exception {
        testFooBoxIsTrue("forIteratesOverArray.kt");
    }

    @Test
    public void forOnEmptyArray() throws Exception {
        testFooBoxIsTrue("forOnEmptyArray.kt");
    }
}