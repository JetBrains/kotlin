package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 */
public class IntrinsicTest extends AbstractExpressionTest {

    final private static String MAIN = "intrinsic/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void intrinsicPlusAssign() throws Exception {
        testFooBoxIsTrue("plusAssign.kt");
    }

    @Test
    public void minusAssignOnProperty() throws Exception {
        testFooBoxIsTrue("minusAssignOnProperty.kt");
    }

}
