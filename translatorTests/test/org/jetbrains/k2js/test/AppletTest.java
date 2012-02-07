package org.jetbrains.k2js.test;

import org.jetbrains.k2js.facade.K2JSTranslatorApplet;
import org.junit.Assert;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * @author Pavel Talanov
 */
public final class AppletTest extends TranslationTest {

    final private static String MAIN = "applet/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testSimple() throws Exception {
        evaluateCodeWithParameters("fun main(args : Array<String>) {}", " a 3 1   2134", "");
    }


    public void testSystemOutTest() throws Exception {
        evaluateCodeWithParameters("class A() {} fun main(args : Array<String>) { var a = A();}", "", "");
    }

    private void evaluateCodeWithParameters(String kotlinCode, String paramString, String expectedResult)
            throws Exception {
        String programCode = (new K2JSTranslatorApplet())
                .translateToJS(kotlinCode, paramString);
        Assert.assertEquals(evaluateStringWithRhino(programCode), expectedResult);
    }


    private String evaluateStringWithRhino(String programCode) throws Exception {
        Context context = Context.enter();
        Scriptable scope = context.initStandardObjects();
        runFileWithRhino(kotlinLibraryPath(), context, scope);
        Object result = context.evaluateString(scope, programCode, "program code", 1, null);
        Context.exit();
        assert result instanceof String : "Must evaluate to string! Evaluated to: " + result;
        return (String) result;
    }
}
