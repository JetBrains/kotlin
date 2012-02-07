package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class MultiFileTest extends TranslationTest {

    final private static String MAIN = "multiFile/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testFunctionsVisibleFromOtherFile() throws Exception {
        testFooBoxIsTrue("functionsVisibleFromOtherFile");
    }

    public void testClassesInheritedFromOtherFile() throws Exception {
        testFooBoxIsTrue("classesInheritedFromOtherFile");
    }

    @Override
    public void testFooBoxIsTrue(String dirName) throws Exception {
        testMultiFile(dirName, "foo", "box", true);
    }
}
