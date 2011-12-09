package org.jetbrains.k2js.test;

import org.junit.Test;

import java.util.Arrays;

/**
 * @author Talanov Pavel
 */
public final class SystemTest extends JavaClassesTest {

    final private static String MAIN = "system/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void checkOutput(String filename, String expectedResult, String... args) throws Exception {
        translateFile(filename);
        runRhinoTest(generateFilenameList(getOutputFilePath(filename)),
                new RhinoSystemOutputChecker(expectedResult, Arrays.asList(args)));
    }

    @Test
    public void systemPrint() throws Exception {
        checkOutput("systemPrint.kt", "Hello, world!");
    }


}
