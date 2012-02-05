package org.jetbrains.k2js.test;

import org.jetbrains.k2js.facade.K2JSTranslator;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//TODO: spread the test* methods amongst classes that actually use them

/**
 * @author Pavel Talanov
 */
public abstract class TranslationTest extends BaseTest {

    private final static String TEST_FILES = "translator/testFiles/";
    private static final String CASES = "cases/";
    private static final String OUT = "out/";
    private static final String KOTLIN_JS_LIB = TEST_FILES + "kotlin_lib.js";
    private static final String EXPECTED = "expected/";

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

    private String getExpectedPath() {
        return testFilesPath() + expectedDirectoryName();
    }

    private String expectedDirectoryName() {
        return EXPECTED;
    }

    protected void testFunctionOutput(String filename, String namespaceName,
                                      String functionName, Object expectedResult) throws Exception {
        translateFile(filename);
        runRhinoTest(generateFilenameList(getOutputFilePath(filename)),
                new RhinoFunctionResultChecker(namespaceName, functionName, expectedResult));
    }

    protected void testMultiFile(String dirName, String namespaceName,
                                 String functionName, Object expectedResult) throws Exception {
        translateFilesInDir(dirName);
        runRhinoTest(generateFilenameList(getOutputFilePath(dirName + ".kt")),
                new RhinoFunctionResultChecker(namespaceName, functionName, expectedResult));
    }

    protected void translateFile(String filename) throws Exception {
        K2JSTranslator.testTranslateFile(getInputFilePath(filename), getOutputFilePath(filename));
    }

    protected void translateFilesInDir(String dirName) throws Exception {
        String dirPath = getInputFilePath(dirName);
        File dir = new File(dirPath);
        List<String> fullFilePaths = new ArrayList<String>();
        for (String fileName : dir.list()) {
            fullFilePaths.add(getInputFilePath(dirName) + "\\" + fileName);
        }
        assert dir.isDirectory();
        K2JSTranslator.testTranslateFiles(fullFilePaths, getOutputFilePath(dirName + ".kt"));
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

    private String expected(String testName) {
        return getExpectedPath() + testName + ".out";
    }

    protected void runFileWithRhino(String inputFile, Context context, Scriptable scope) throws Exception {
        FileReader reader = new FileReader(inputFile);
        context.evaluateReader(scope, reader, inputFile, 1, null);
        reader.close();
    }

    protected void runRhinoTest(List<String> fileNames, RhinoResultChecker checker) throws Exception {
        Context context = Context.enter();
        Scriptable scope = context.initStandardObjects();
        for (String filename : fileNames) {
            runFileWithRhino(filename, context, scope);
        }
        checker.runChecks(context, scope);
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

    protected void testWithMain(String testName, String testId, String... args) throws Exception {
        checkOutput(testName + ".kt", readFile(expected(testName + testId)), args);
    }

    private static String readFile(String path) throws IOException {
        FileInputStream stream = new FileInputStream(new File(path));
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            /* Instead of using default, pass in a decoder. */
            return Charset.defaultCharset().decode(bb).toString();
        } finally {
            stream.close();
        }
    }


}
