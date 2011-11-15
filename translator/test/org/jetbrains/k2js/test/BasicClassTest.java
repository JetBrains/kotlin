package org.jetbrains.k2js.test;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * @author Talanov Pavel
 */
public class BasicClassTest extends AbstractClassTest {

    final private static String MAIN = "class/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void kotlinJsLibRunsWithRhino() throws Exception {
        Context context = Context.enter();
        Scriptable scope = context.initStandardObjects();
        runFileWithRhino(kotlinLibraryPath(), context, scope);
        Context.exit();
    }

    @Test
    public void classInstantiation() throws Exception {
        testFooBoxIsTrue("classInstantiation.kt");
    }

    @Test
    public void methodDeclarationAndCall() throws Exception {
        testFooBoxIsTrue("methodDeclarationAndCall.kt");
    }

    //TODO: test excluded. Wait for bugfix and implement functionality
    @Test
    public void constructorWithParameter() throws Exception {
        testFooBoxIsTrue("constructorWithParameter.kt");
    }

    @Test
    public void incrementProperty() throws Exception {
        performTest("incrementProperty.kt", "foo", "box", "OK");
    }

    @Test
    public void SimpleInitializer() throws Exception {
        testFooBoxIsTrue("simpleInitializer.kt");
    }

    @Test
    public void complexExpressionAsConstructorParameter() throws Exception {
        testFooBoxIsTrue("complexExpressionAsConstructorParameter.kt");
    }
}
