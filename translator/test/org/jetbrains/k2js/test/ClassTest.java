package org.jetbrains.k2js.test;

import org.jetbrains.k2js.translate.TranslationContext;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.FileReader;

import static org.junit.Assert.assertTrue;

/**
 * @author Talanov Pavel
 */
public final class ClassTest extends TranslationTest {

    private final String KOTLIN_JS_LIB = TEST_DIR + "kotlin_lib.js";

    @Override
    protected void runWithRhino(String inputFile, String namespaceName,
                              String functionName, Object expectedResult) throws Exception {
        Context cx = Context.enter();
        FileReader testFileReader = new FileReader(inputFile);
        FileReader libReader = new FileReader(KOTLIN_JS_LIB);
        try {
            Scriptable scope = cx.initStandardObjects();
            cx.evaluateReader(scope, libReader, "lib", 1, null);
            cx.evaluateReader(scope, testFileReader, "test case", 1, null);
            Object result = extractAndCallFunctionObject(namespaceName, functionName, cx, scope);
            assertTrue(result.equals(expectedResult));
            String report = namespaceName + "." + functionName + "() = " + Context.toString(result);
            System.out.println(report);

        } finally {
            Context.exit();
            testFileReader.close();
        }
    }

    @Test
    public void kotlinJsLibRunsWithRhino() throws Exception {
        runWithRhino(TEST_DIR + "testKotlinLib.js", "foo", "box", true);
    }

    @Test
    public void classInstantiation() throws Exception {
        testFooBoxIsTrue("classInstantiation.kt");
    }

    @Test
    public void methodDeclarationAndCall() throws Exception {
        testFooBoxIsTrue("methodDeclarationAndCall.kt");
    }

}
