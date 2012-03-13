/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.k2js.test.semantics;

/**
 * @author Pavel Talanov
 */
public class FunctionTest extends AbstractExpressionTest {

    public FunctionTest() {
        super("function/");
    }

    public void testFunctionUsedBeforeDeclaration() throws Exception {
        checkFooBoxIsTrue("functionUsedBeforeDeclaration.kt");
    }

    public void testFunctionWithTwoParametersCall() throws Exception {
        checkFooBoxIsTrue("functionWithTwoParametersCall.kt");
    }

    public void testFunctionLiteral() throws Exception {
        checkFooBoxIsTrue("functionLiteral.kt");
    }

    public void testAdderClosure() throws Exception {
        checkFooBoxIsTrue("adderClosure.kt");
    }

    public void testLoopClosure() throws Exception {
        checkFooBoxIsTrue("loopClosure.kt");
    }

    public void testFunctionLiteralAsParameter() throws Exception {
        checkFooBoxIsTrue("functionLiteralAsParameter.kt");
    }

    public void testClosureWithParameter() throws Exception {
        checkFooBoxIsOk("closureWithParameter.kt");
    }

    public void testClosureWithParameterAndBoxing() throws Exception {
        checkFooBoxIsOk("closureWithParameterAndBoxing.jet");
    }

    public void testEnclosingThis() throws Exception {
        runFunctionOutputTest("enclosingThis.kt", "Anonymous", "box", "OK");
    }


    public void testImplicitItParameter() throws Exception {
        checkFooBoxIsTrue("implicitItParameter.kt");
    }


    public void testDefaultParameters() throws Exception {
        checkFooBoxIsTrue("defaultParameters.kt");
    }


    public void testFunctionLiteralAsLastParameter() throws Exception {
        checkFooBoxIsTrue("functionLiteralAsLastParameter.kt");
    }


    public void testNamedArguments() throws Exception {
        checkFooBoxIsTrue("namedArguments.kt");
    }


    public void testExpressionAsFunction() throws Exception {
        checkFooBoxIsTrue("expressionAsFunction.kt");
    }


    public void testVararg() throws Exception {
        checkFooBoxIsTrue("vararg.kt");
    }

    //TODO: disabled. Probable cause : vars in closures.
//
//    public void testKT921() throws Exception {
//
//        checkOutput("KT-921.kt", "1,\n" +
//                                 "1, 2,\n" +
//                                 "2, ");
//    }

    public void testFunctionInsideFunction() throws Exception {
        checkFooBoxIsTrue("functionInsideFunction.kt");
    }
}
