package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Pavel Talanov
 */
public final class MultiFileTest extends TranslationTest {

    final private static String MAIN = "multiFile/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void functionsVisibleFromOtherFile() throws Exception {
        testFooBoxIsTrue("functionsVisibleFromOtherFile");
    }

    @Test
    public void classesInheritedFromOtherFile() throws Exception {
        testFooBoxIsTrue("classesInheritedFromOtherFile");
    }

    @Override
    protected void testFooBoxIsTrue(String dirName) throws Exception {
        testMultiFile(dirName, "foo", "box", true);
    }
}
