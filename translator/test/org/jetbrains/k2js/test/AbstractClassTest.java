package org.jetbrains.k2js.test;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.FileReader;

import static org.junit.Assert.assertTrue;

/**
 * @author Talanov Pavel
 */
public abstract class AbstractClassTest extends TranslationTest {

    private final String KOTLIN_JS_LIB = TEST_FILES + "kotlin_lib.js";

    protected String kotlinLibraryPath() {
        return KOTLIN_JS_LIB;
    }

    protected void runFileWithRhino(String inputFile, Context context, Scriptable scope) throws Exception {
        FileReader reader = new FileReader(inputFile);
        context.evaluateReader(scope, reader, inputFile, 1, null);
        reader.close();
    }

    @Override
    protected void runWithRhino(String inputFile, String namespaceName,
                                String functionName, Object expectedResult) throws Exception {
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();
        runFileWithRhino(kotlinLibraryPath(), cx, scope);
        runFileWithRhino(inputFile, cx, scope);
        Object result = extractAndCallFunctionObject(namespaceName, functionName, cx, scope);
        assertTrue("Result is not what expected!", result.equals(expectedResult));
        String report = namespaceName + "." + functionName + "() = " + Context.toString(result);
        System.out.println(report);
        Context.exit();
    }

}
