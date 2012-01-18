package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 *         <p/>
 *         This class contains tests that do not fall in any particular category
 *         most probably because that functionality has very little support
 */
public class MiscTest extends AbstractExpressionTest {
    final private static String MAIN = "misc/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void namespaceProperties() throws Exception {
        testFunctionOutput("localProperty.jet", "foo", "box", 50);
    }

    @Test
    public void intRange() throws Exception {
        // checkOutput("intRange.kt", " ");
        testFooBoxIsTrue("intRange.kt");
    }


    @Test
    public void safecallComputesExpressionOnlyOnce() throws Exception {
        testFooBoxIsTrue("safecallComputesExpressionOnlyOnce.kt");
    }
}
