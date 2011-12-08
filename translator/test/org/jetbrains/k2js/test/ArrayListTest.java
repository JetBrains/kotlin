package org.jetbrains.k2js.test;

import org.junit.Test;

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
}
