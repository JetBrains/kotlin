package org.jetbrains.k2js.test;

import org.jetbrains.k2js.K2JSTranslator;
import org.junit.Before;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import java.io.FileReader;
import java.util.List;

import static org.junit.Assert.assertTrue;


/**
 * @author Talanov Pavel
 */
public abstract class TranslationTest {

    final protected static String TEST_FILES = "testFiles/";
    final private static String CASES = "cases/";
    final private static String OUT = "out/";
    final private String KOTLIN_JS_LIB = TEST_FILES + "kotlin_lib.js";

    protected String testFilesDirectory;
    protected String testCasesDirectory;
    protected String outputDirectory;

    @Before
    public void setUpClass() {
        testCasesDirectory = CASES;
        outputDirectory = OUT;
        testFilesDirectory = TEST_FILES + mainDirectory();
    }

    protected abstract String mainDirectory();

    protected String kotlinLibraryPath() {
        return KOTLIN_JS_LIB;
    }

    private String getOutputDirectory() {
        return testFilesDirectory + outputDirectory;
    }

    private String getInputDirectory() {
        return testFilesDirectory + testCasesDirectory;
    }

    protected void performTest(String filename, String namespaceName,
                               String functionName, Object expectedResult) throws Exception {
        K2JSTranslator.Arguments args = new K2JSTranslator.Arguments();
        args.src = getInputFilePath(filename);
        args.outputDir = getOutputFilePath(filename);
        K2JSTranslator.translate(args);
        runWithRhino(generateFilenameList(args.outputDir), namespaceName, functionName, expectedResult);
    }

    abstract protected List<String> generateFilenameList(String inputfile);

    private String getOutputFilePath(String filename) {
        return getOutputDirectory() + convertToDotJsFile(filename);
    }

    private String convertToDotJsFile(String filename) {
        return filename.substring(0, filename.lastIndexOf('.')) + ".js";
    }

    private String getInputFilePath(String filename) {
        return getInputDirectory() + filename;
    }

    protected void runFileWithRhino(String inputFile, Context context, Scriptable scope) throws Exception {
        FileReader reader = new FileReader(inputFile);
        context.evaluateReader(scope, reader, inputFile, 1, null);
        reader.close();
    }

    protected void runWithRhino(List<String> filenames, String namespaceName,
                                String functionName, Object expectedResult) throws Exception {
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();
        for (String filename : filenames) {
            runFileWithRhino(filename, cx, scope);
        }
        Object result = extractAndCallFunctionObject(namespaceName, functionName, cx, scope);
        assertTrue("Result is not what expected!", result.equals(expectedResult));
        String report = namespaceName + "." + functionName + "() = " + Context.toString(result);
        System.out.println(report);
        Context.exit();
    }

    protected Object extractAndCallFunctionObject(String namespaceName, String functionName,
                                                  Context cx, Scriptable scope) {
        Object foo = scope.get(namespaceName, scope);
        assertTrue(foo instanceof NativeObject);
        NativeObject namespaceObject = (NativeObject) foo;
        Object box = namespaceObject.get(functionName, namespaceObject);
        assertTrue("Function " + functionName + " not defined in namespace " + namespaceName, box instanceof Function);
        Object functionArgs[] = {};
        Function function = (Function) box;
        return function.call(cx, scope, scope, functionArgs);
    }

    protected void testFooBoxIsTrue(String filename) throws Exception {
        performTest(filename, "foo", "box", true);
    }
}
