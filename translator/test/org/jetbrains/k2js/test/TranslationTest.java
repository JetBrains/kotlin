package org.jetbrains.k2js.test;

import org.jetbrains.k2js.K2JSTranslator;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.FileReader;
import java.util.Arrays;
import java.util.List;


/**
 * @author Pavel Talanov
 */
public abstract class TranslationTest {

    private final static String TEST_FILES = "translator/testFiles/";
    final private static String CASES = "cases/";
    final private static String OUT = "out/";
    final private static String KOTLIN_JS_LIB = TEST_FILES + "kotlin_lib.js";

    protected abstract String mainDirectory();

    protected String kotlinLibraryPath() {
        return KOTLIN_JS_LIB;
    }

    private String casesDirectoryName() {
        return CASES;
    }

    private String outDirectoryName() {
        return OUT;
    }

    private String testFilesPath() {
        return TEST_FILES + suiteDirectoryName() + mainDirectory();
    }

    protected String suiteDirectoryName() {
        return "";
    }

    private String getOutputPath() {
        return testFilesPath() + outDirectoryName();
    }

    private String getInputPath() {
        return testFilesPath() + casesDirectoryName();
    }

    protected void testFunctionOutput(String filename, String namespaceName,
                                      String functionName, Object expectedResult) throws Exception {
        translateFile(filename);
        runRhinoTest(generateFilenameList(getOutputFilePath(filename)),
                new RhinoFunctionResultChecker(namespaceName, functionName, expectedResult));
    }

    protected void translateFile(String filename) throws Exception {
        (new K2JSTranslator()).translateFile(getInputFilePath(filename), getOutputFilePath(filename));
    }

    protected List<String> generateFilenameList(String inputFile) {
        return Arrays.asList(kotlinLibraryPath(), inputFile);
    }

    //TODO: refactor filename generation logic
    protected String getOutputFilePath(String filename) {
        return getOutputPath() + convertToDotJsFile(filename);
    }

    private String convertToDotJsFile(String filename) {
        return filename.substring(0, filename.lastIndexOf('.')) + ".js";
    }

    private String getInputFilePath(String filename) {
        return getInputPath() + filename;
    }

    protected String cases(String filename) {
        return getInputFilePath(filename);
    }

    protected void runFileWithRhino(String inputFile, Context context, Scriptable scope) throws Exception {
        FileReader reader = new FileReader(inputFile);
        context.evaluateReader(scope, reader, inputFile, 1, null);
        reader.close();
    }

    protected void runRhinoTest(List<String> fileNames, RhinoResultChecker checker) throws Exception {
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();
        for (String filename : fileNames) {
            runFileWithRhino(filename, cx, scope);
        }
        checker.runChecks(cx, scope);
        Context.exit();
    }

    protected void testFooBoxIsTrue(String filename) throws Exception {
        testFunctionOutput(filename, "foo", "box", true);
    }

    protected void testFooBoxIsOk(String filename) throws Exception {
        testFunctionOutput(filename, "foo", "box", "OK");
    }

    protected void checkOutput(String filename, String expectedResult, String... args) throws Exception {
        translateFile(filename);
        runRhinoTest(generateFilenameList(getOutputFilePath(filename)),
                new RhinoSystemOutputChecker(expectedResult, Arrays.asList(args)));
    }

}
