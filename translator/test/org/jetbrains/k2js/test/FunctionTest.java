package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public final class FunctionTest extends TranslationTest {

    @Test
    public void currentTest() throws Exception {
        testFooBoxIsTrue("test.kt");
    }

    @Test
    public void testAssign() throws Exception {
        performTest("assign.jet", "foo", "f", 2.0);
    }

    @Test
    public void namespaceProperties() throws Exception {
        performTest("localProperty.jet", "foo", "box", 50);
    }

    @Test
    public void comparison() throws Exception {
        testFooBoxIsTrue("comparison.kt");
    }

}
