package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class ForTest extends AbstractExpressionTest {

    final private static String MAIN = "for/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testForIteratesOverArray() throws Exception {
        testFooBoxIsTrue("forIteratesOverArray.kt");
    }

    public void testForOnEmptyArray() throws Exception {
        testFooBoxIsTrue("forOnEmptyArray.kt");
    }
}