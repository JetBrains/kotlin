package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public class MultiNamespaceTest extends TranslationTest {
    final private static String MAIN = "multiNamespace/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testFunctionsVisibleFromOtherNamespace() throws Exception {
        testFooBoxIsTrue("functionsVisibleFromOtherNamespace");
    }

    public void testClassesInheritedFromOtherNamespace() throws Exception {
        testFooBoxIsTrue("classesInheritedFromOtherNamespace");
    }

    @Override
    public void testFooBoxIsTrue(String dirName) throws Exception {
        testMultiFile(dirName, "foo", "box", true);
    }
}

