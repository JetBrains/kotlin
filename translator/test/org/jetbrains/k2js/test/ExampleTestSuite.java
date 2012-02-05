package org.jetbrains.k2js.test;

import junit.framework.Test;
import org.jetbrains.annotations.NotNull;

public final class ExampleTestSuite extends TranslationTest {


    final private static String MAIN = "examples/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void runBoxTest(String filename) throws Exception {
        testFunctionOutput(filename, "Anonymous", "box", "OK");
    }

    private String name;

    public ExampleTestSuite(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void runTest() throws Exception {
        runBoxTest(getName());
    }

    public static Test suite() {
        return TranslatorTestCaseBuilder.suiteForDirectory("translator",
                "/testFiles/examples/cases/", true, new TranslatorTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name) {
                return (new ExampleTestSuite(name));
            }
        });
    }
}
