package org.jetbrains.k2js.test;

import junit.framework.Test;
import org.jetbrains.annotations.NotNull;

/**
 * @author Pavel Talanov
 */
public final class SimpleTestSuite extends TranslationTest {
    final private static String MAIN = "simple/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void runBoxTest(String filename) throws Exception {
        testFooBoxIsTrue(filename);
    }

    private String name;

    public SimpleTestSuite(String name) {
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
                "/testFiles/simple/cases/", true, new TranslatorTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name) {
                return (new SimpleTestSuite(name));
            }
        });
    }
}
