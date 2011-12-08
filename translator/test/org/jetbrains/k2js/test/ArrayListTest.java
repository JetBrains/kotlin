package org.jetbrains.k2js.test;

import org.junit.Test;
import org.mozilla.javascript.JavaScriptException;

/**
 * @author Talanov Pavel
 */
public final class ArrayListTest extends JavaClassesTest {

    final private static String MAIN = "arrayList/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void emptyList() throws Exception {
        testFooBoxIsTrue("emptyList.kt");
    }

    @Test
    public void access() throws Exception {
        testFooBoxIsTrue("access.kt");
    }

    @Test
    public void isEmpty() throws Exception {
        testFooBoxIsTrue("isEmpty.kt");
    }

    @Test
    public void arrayAccess() throws Exception {
        testFooBoxIsTrue("arrayAccess.kt");
    }

    @Test
    public void iterate() throws Exception {
        testFooBoxIsTrue("iterate.kt");
    }

    @Test
    public void remove() throws Exception {
        testFooBoxIsTrue("remove.kt");
    }

    @Test(expected = JavaScriptException.class)
    public void indexOOB() throws Exception {
        testFooBoxIsTrue("indexOOB.kt");
    }
}
