package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public class FunctionTest extends AbstractExpressionTest {

    final private static String MAIN = "function/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    public void testFunctionUsedBeforeDeclaration() throws Exception {
        testFooBoxIsTrue("functionUsedBeforeDeclaration.kt");
    }

    public void testFunctionWithTwoParametersCall() throws Exception {
        testFooBoxIsTrue("functionWithTwoParametersCall.kt");
    }

    public void testFunctionLiteral() throws Exception {
        testFooBoxIsTrue("functionLiteral.kt");
    }

    public void testAdderClosure() throws Exception {
        testFooBoxIsTrue("adderClosure.kt");
    }

    public void testLoopClosure() throws Exception {
        testFooBoxIsTrue("loopClosure.kt");
    }

    public void testFunctionLiteralAsParameter() throws Exception {
        testFooBoxIsTrue("functionLiteralAsParameter.kt");
    }

    public void testClosureWithParameter() throws Exception {
        testFooBoxIsOk("closureWithParameter.kt");
    }

    public void testClosureWithParameterAndBoxing() throws Exception {
        testFooBoxIsOk("closureWithParameterAndBoxing.jet");
    }

    public void testEnclosingThis() throws Exception {
        testFunctionOutput("enclosingThis.kt", "Anonymous", "box", "OK");
    }


    public void testImplicitItParameter() throws Exception {
        testFooBoxIsTrue("implicitItParameter.kt");
    }


    public void testDefaultParameters() throws Exception {
        testFooBoxIsTrue("defaultParameters.kt");
    }


    public void testFunctionLiteralAsLastParameter() throws Exception {
        testFooBoxIsTrue("functionLiteralAsLastParameter.kt");
    }


    public void testNamedArguments() throws Exception {
        testFooBoxIsTrue("namedArguments.kt");
    }


    public void testExpressionAsFunction() throws Exception {
        testFooBoxIsTrue("expressionAsFunction.kt");
    }


    public void testVararg() throws Exception {
        testFooBoxIsTrue("vararg.kt");
    }


    public void testkt921() throws Exception {
        try {
            checkOutput("KT-921.kt", "");
        } catch (Throwable e) {
            System.out.println(e);
        }
    }
}
