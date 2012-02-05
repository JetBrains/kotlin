package org.jetbrains.k2js.test;

import org.mozilla.javascript.JavaScriptException;

/**
 * @author Pavel Talanov
 */
public final class ArrayListTest extends JavaClassesTest {

    final private static String MAIN = "arrayList/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testEmptyList() throws Exception {
        testFooBoxIsTrue("emptyList.kt");
    }

    public void testAccess() throws Exception {
        testFooBoxIsTrue("access.kt");
    }

    public void testIsEmpty() throws Exception {
        testFooBoxIsTrue("isEmpty.kt");
    }

    public void testArrayAccess() throws Exception {
        testFooBoxIsTrue("arrayAccess.kt");
    }

    public void testIterate() throws Exception {
        testFooBoxIsTrue("iterate.kt");
    }

    public void testRemove() throws Exception {
        testFooBoxIsTrue("remove.kt");
    }

    public void testIndexOOB() throws Exception {
        try {
            testFooBoxIsTrue("indexOOB.kt");
            fail();
        } catch (JavaScriptException e) {

        }
    }
}
