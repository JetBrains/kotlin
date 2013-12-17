/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

public class FunctionTest extends AbstractExpressionTest {

    public FunctionTest() {
        super("function/");
    }

    public void testFunctionUsedBeforeDeclaration() throws Exception {
        fooBoxTest();
    }

    public void testFunctionWithTwoParametersCall() throws Exception {
        fooBoxTest();
    }

    public void testWhenFunction() throws Exception {
        checkFooBoxIsOk();
    }

    public void testFunctionLiteral() throws Exception {
        fooBoxTest();
    }

    public void testAdderClosure() throws Exception {
        fooBoxTest();
    }

    public void testLoopClosure() throws Exception {
        fooBoxTest();
    }

    public void testFunctionLiteralAsParameter() throws Exception {
        fooBoxTest();
    }

    public void testClosureWithParameter() throws Exception {
        checkFooBoxIsOk();
    }

    public void testClosureWithParameterAndBoxing() throws Exception {
        checkFooBoxIsOk();
    }

    public void testEnclosingThis() throws Exception {
        checkFooBoxIsOk();
    }


    public void testImplicitItParameter() throws Exception {
        fooBoxTest();
    }


    public void testDefaultParameters() throws Exception {
        fooBoxTest();
    }


    public void testFunctionLiteralAsLastParameter() throws Exception {
        fooBoxTest();
    }


    public void testNamedArguments() throws Exception {
        fooBoxTest();
    }


    public void testExpressionAsFunction() throws Exception {
        fooBoxTest();
    }


    public void testVararg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testKT921() throws Exception {

        checkOutput("KT-921.kt", "1, end\n" +
                                 "1, 2, end\n" +
                                 "1, 2, 3, end\n" +
                                 "2, 3, end\n" +
                                 "!\n" +
                                 "3, end\n" +
                                 "!\n" +
                                 "end\n" +
                                 "!");
    }

    public void testFunctionInsideFunction() throws Exception {
        fooBoxTest();
    }

    public void testCallFunInInit() throws Exception {
        fooBoxTest();
    }
}
