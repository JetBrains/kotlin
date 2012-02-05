package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public class IntrinsicTest extends AbstractExpressionTest {

    final private static String MAIN = "intrinsic/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testIntrinsicPlusAssign() throws Exception {
        testFooBoxIsTrue("plusAssign.kt");
    }


    public void testMinusAssignOnProperty() throws Exception {
        testFooBoxIsTrue("minusAssignOnProperty.kt");
    }

}
