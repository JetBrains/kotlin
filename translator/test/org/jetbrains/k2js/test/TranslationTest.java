package org.jetbrains.k2js.test;

import org.jetbrains.k2js.K2JSTranslator;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import java.io.FileReader;
import static org.junit.Assert.*;


/**
 * @author Talanov Pavel
 */
public class TranslationTest {

    protected final static String TEST_DIR = "test_files/test_cases/";

    protected void performTest(String inputFile, String namespaceName,
                             String functionName, Object expectedResult) throws Exception {
        K2JSTranslator.Arguments args = new K2JSTranslator.Arguments();
        args.src = TEST_DIR + inputFile;
        args.outputDir = getOutputFilename(TEST_DIR + inputFile);
        K2JSTranslator.translate(args);
        runWithRhino(args.outputDir, namespaceName, functionName, expectedResult);
    }

    private String getOutputFilename(String inputFile) {
        return inputFile.substring(0, inputFile.lastIndexOf('.')) + ".js";
    }

    protected void runWithRhino(String inputFile, String namespaceName,
                              String functionName, Object expectedResult) throws Exception {
        Context cx = Context.enter();
        FileReader reader = new FileReader(inputFile);
        try {
            Scriptable scope = cx.initStandardObjects();
            cx.evaluateReader(scope, reader, "test case", 1, null);
            Object result = extractAndCallFunctionObject(namespaceName, functionName, cx, scope);
            assertTrue(result.equals(expectedResult));
            String report = namespaceName + "." + functionName + "() = " + Context.toString(result);
            System.out.println(report);

        } finally {
            Context.exit();
            reader.close();
        }
    }

    protected Object extractAndCallFunctionObject(String namespaceName, String functionName,
                                                Context cx, Scriptable scope) {
        Object foo = scope.get(namespaceName, scope);
        assertTrue(foo instanceof NativeObject);
        NativeObject namespaceObject = (NativeObject)foo;
        Object box = namespaceObject.get(functionName, namespaceObject);
        assertTrue(box instanceof Function);
        Object functionArgs[] = {};
        Function function = (Function)box;
        return function.call(cx, scope, scope, functionArgs);
    }

    protected void testFooBoxIsTrue(String filename) throws Exception {
        performTest(filename, "foo", "box", true);
    }
}
