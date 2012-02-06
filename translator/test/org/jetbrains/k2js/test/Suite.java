package org.jetbrains.k2js.test;

import junit.framework.Test;
import junit.framework.TestResult;
import org.jetbrains.annotations.NotNull;

/**
 * @author Pavel Talanov
 */
public final class Suite extends TranslationTest {

    private String name;
    private final SingleFileTester tester;
    private final String testMain;

    public Suite(@NotNull String testName,
                 @NotNull String suiteDirName,
                 @NotNull final SingleFileTester tester) {
        this.name = testName;
        this.tester = tester;
        this.testMain = suiteDirName;
    }

    @Override
    protected String mainDirectory() {
        return testMain;
    }

    public void runTest() throws Exception {
        tester.performTest(this, name);
    }

    public static Test suite() {
        return new Test() {
            @Override
            public int countTestCases() {
                return 0;
            }

            @Override
            public void run(TestResult testResult) {
                //do nothing
            }
        };
    }

    public static Test suiteForDirectory(@NotNull final String mainName, @NotNull final SingleFileTester testMethod) {

        return TranslatorTestCaseBuilder.suiteForDirectory("translator\\testFiles\\",
                mainName + casesDirectoryName(), true, new TranslatorTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String name) {
                return (new Suite(name, mainName, testMethod));
            }
        });
    }

    protected interface SingleFileTester {
        void performTest(@NotNull Suite test, @NotNull String filename) throws Exception;

    }
}
