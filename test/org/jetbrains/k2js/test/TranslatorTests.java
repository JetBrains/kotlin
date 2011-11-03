package org.jetbrains.k2js.test;

import org.jetbrains.k2js.K2JSTranslator;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import java.io.FileReader;
import static org.junit.Assert.*;


/**
 * @author Talanov Pavel
 */
public class TranslatorTests {

    private final static String TEST_DIR = "test_files/test_cases/";

    private void performTest(String inputFile) throws Exception {
        K2JSTranslator.Arguments args = new K2JSTranslator.Arguments();
        args.src = TEST_DIR + inputFile;
        args.outputDir = getOutputFilename(TEST_DIR + inputFile);
        K2JSTranslator.translate(args);
        runWithRhino(args.outputDir);
    }

    private String getOutputFilename(String inputFile) {
        return inputFile.substring(0, inputFile.lastIndexOf('.')) + ".js";
    }

    private void runWithRhino(String inputFile) throws Exception {
        Context cx = Context.enter();
        FileReader reader = new FileReader(inputFile);
        try {
            Scriptable scope = cx.initStandardObjects();
            cx.evaluateReader(scope, reader, "test case", 1, null);

            Object foo = scope.get("foo", scope);
            assertTrue(foo instanceof NativeObject);
            NativeObject namespaceObject = (NativeObject)foo;
            Object box = namespaceObject.get("box", namespaceObject);
            assertTrue(box instanceof Function);
            Object functionArgs[] = {};
            Function function = (Function) box;
            Object result = function.call(cx, scope, scope, functionArgs);
            String report = "foo.box() = " + Context.toString(result);
            System.out.println(report);

        } finally {
            Context.exit();
            reader.close();
        }
    }

    @Test
    public void test() throws Exception {
        performTest("test.kt");
    }

    @Test
    public void testAssign() throws Exception {
        performTest("assign.jet");
    }

    @Test
    public void namespaceProperties() throws Exception {
        performTest("localProperty.jet");
    }

}
