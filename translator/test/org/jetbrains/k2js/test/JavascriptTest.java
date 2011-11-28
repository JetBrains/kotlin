package org.jetbrains.k2js.test;

import org.junit.Test;

import java.util.Arrays;

/**
 * @author Talanov Pavel
 */
public final class JavascriptTest extends TranslationTest {

    final private static String MAIN = "javascript/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void enclosingThis() throws Exception {
        runRhinoTest(Arrays.asList(kotlinLibraryPath(), cases("enclosingThis.js")),
                new RhinoFunctionResultChecker("Anonymous", "box", "OK"));
    }
}
