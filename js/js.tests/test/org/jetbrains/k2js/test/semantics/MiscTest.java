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

import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.translate.context.Namer;
import org.mozilla.javascript.JavaScriptException;

/**
 * @author Pavel Talanov
 *         <p/>
 *         A messy class where all new tests go before they are sorted which never happens.
 */
public final class MiscTest extends AbstractExpressionTest {

    public MiscTest() {
        super("misc/");
    }

    public void testLocalPropertys() throws Exception {
        runFunctionOutputTest("localProperty.jet", "foo", "box", 50);
    }

    public void testIntRange() throws Exception {
        fooBoxTest();
    }


    public void testSafecallComputesExpressionOnlyOnce() throws Exception {
        fooBoxTest();
    }

    public void testClassWithoutNamespace() throws Exception {
        runFunctionOutputTest("classWithoutNamespace.kt", Namer.getRootNamespaceName(), "box", true);
    }

    public void testIfElseAsExpressionWithThrow() throws Exception {
        try {
            fooBoxTest();
            fail();
        }
        catch (JavaScriptException e) {
        }
    }

    public void testKt1052_2() throws Exception {
        checkFooBoxIsTrue("KT-1052-2.kt", EcmaVersion.all());
    }

    public void testKt2314() throws Exception {
        checkFooBoxIsTrue("KT-2314.kt", EcmaVersion.all());
    }

    public void testKt1052() throws Exception {
        checkOutput("KT-1052.kt", "true\n");
    }


    public void testKt740_1() throws Exception {
        checkFooBoxIsTrue("KT-740.kt", EcmaVersion.all());
    }

    public void testKt740_2() throws Exception {
        checkFooBoxIsOk("KT-740-2.kt");
    }

    public void testKt1361_1() throws Exception {
        checkFooBoxIsTrue("KT-1361-1.kt", EcmaVersion.all());
    }

    public void testKt1361_2() throws Exception {
        checkFooBoxIsTrue("KT-1361-2.kt", EcmaVersion.all());
    }

    public void testKt817() throws Exception {
        checkFooBoxIsTrue("KT-817.kt", EcmaVersion.all());
    }

    public void testKt740_3() throws Exception {
        checkFooBoxIsOk("KT-740-3.kt");
    }

    public void testFunInConstructor() throws Exception {
        fooBoxTest();
    }

    public void testFunInConstructorBlock() throws Exception {
        fooBoxTest();
    }

    public void testPropertyAsFunCalledOnConstructor() throws Exception {
        fooBoxTest();
    }

    public void testNamespacePropertyCalledAsFun() throws Exception {
        fooBoxTest();
    }

    public void testExtensionLiteralCreatedAtNamespaceLevel() throws Exception {
        fooBoxTest();
    }

    public void testTemporaryVariableCreatedInNamespaceInitializer() throws Exception {
        fooBoxTest();
    }

    public void testWhenReturnedWithoutBlock() throws Exception {
        fooBoxTest();
    }

    public void testElvis() throws Exception {
        fooBoxTest();
    }

    public void testExtensionLiteralCalledInsideExtensionFunction() throws Exception {
        fooBoxTest();
    }

    public void testKt1865() throws Exception {
        checkFooBoxIsTrue("KT-1865.kt", EcmaVersion.all());
    }

    public void testMainFunInNestedNamespace() throws Exception {
        checkOutput("mainFunInNestedNamespace.kt", "ayee");
    }


    public void testPropertiesWithExplicitlyDefinedAccessorsWithoutBodies() throws Exception {
        fooBoxTest();
    }


    public void testExclExcl() throws Exception {
        fooBoxTest();
    }

    public void testExclExclResultIsComputedOnce() throws Exception {
        fooBoxTest();
    }

    public void testNamespaceLevelVarInPackage() throws Exception {
        fooBoxIsValue("OK");
    }

    //TODO: see http://youtrack.jetbrains.com/issue/KT-2564
    @SuppressWarnings("UnusedDeclaration")
    public void TODO_testNamespaceLevelVarInRoot() throws Exception {
        runFunctionOutputTest("namespaceLevelVarInRoot.kt", Namer.getRootNamespaceName(), "box", "OK");
    }

    public void testLazyPropertyGetterNotCalledOnStart() throws Exception {
        checkOutput("lazyProperty.kt", "Hello, world! Gotcha 3");
    }

    public void testLocalVarAsFunction() throws Exception {
        fooBoxIsValue("OK");
    }

    //TODO:see http://youtrack.jetbrains.com/issue/KT-2565
    @SuppressWarnings("UnusedDeclaration")
    public void TODO_testFunctionExpression() throws Exception {
        fooBoxIsValue("OK");
    }

    public void testExclExclThrows() throws Exception {
        try {
            fooBoxTest();
            fail();
        }
        catch (JavaScriptException e) {
        }
    }
}
