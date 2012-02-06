package org.jetbrains.k2js.test;

import junit.framework.Test;
import org.jetbrains.annotations.NotNull;

/**
 * @author Pavel Talanov
 */
//TODO: this class has strange behaviour. Should be refactored.
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

    public Suite() {
        this("dummy", "dummy", new SingleFileTester() {
            @Override
            public void performTest(@NotNull Suite test, @NotNull String filename) throws Exception {
                //do nothing
            }
        });
    }

    //NOTE: just to avoid warning
    public void testNothing() {
    }

    @Override
    protected String mainDirectory() {
        return testMain;
    }

    public void runTest() throws Exception {
        tester.performTest(this, name);
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

    protected static interface SingleFileTester {
        void performTest(@NotNull Suite test, @NotNull String filename) throws Exception;
    }
}
