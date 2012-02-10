package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 *         <p/>
 *         This class contains tests that do not fall in any particular category
 *         most probably because that functionality has very little support
 */
public final class MiscTest extends AbstractExpressionTest {
    final private static String MAIN = "misc/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testLocalPropertys() throws Exception {
        testFunctionOutput("localProperty.jet", "foo", "box", 50);
    }

    public void testIntRange() throws Exception {
        // checkOutput("intRange.kt", " ");
        testFooBoxIsTrue("intRange.kt");
    }


    public void testSafecallComputesExpressionOnlyOnce() throws Exception {
        testFooBoxIsTrue("safecallComputesExpressionOnlyOnce.kt");
    }
}
