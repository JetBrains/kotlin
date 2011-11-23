package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
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
}
