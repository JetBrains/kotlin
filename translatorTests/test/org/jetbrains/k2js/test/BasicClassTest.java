package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
//TODO: remove class
public class BasicClassTest extends TranslationTest {

    final private static String MAIN = "class/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testClassWithoutNamespace() throws Exception {
        testFunctionOutput("classWithoutNamespace.kt", "Anonymous", "box", true);
    }
}
