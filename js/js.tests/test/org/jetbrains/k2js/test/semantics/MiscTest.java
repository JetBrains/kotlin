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

import org.jetbrains.k2js.translate.context.Namer;
import org.mozilla.javascript.JavaScriptException;

/**
 * A messy class where all new tests go before they are sorted which never happens.
 */
public final class MiscTest extends AbstractExpressionTest {

    public MiscTest() {
        super("misc/");
    }

    public void testLocalProperty() throws Exception {
        fooBoxIsValue(50);
    }

    public void testIntRange() throws Exception {
        fooBoxTest();
    }


    public void testSafecallComputesExpressionOnlyOnce() throws Exception {
        fooBoxTest();
    }

    public void testClassWithoutPackage() throws Exception {
        runFunctionOutputTest("classWithoutPackage.kt", Namer.getRootPackageName(), TEST_FUNCTION, true);
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
        checkFooBoxIsTrue("KT-1052-2.kt");
    }

    public void testKt2314() throws Exception {
        checkFooBoxIsTrue("KT-2314.kt");
    }

    public void testKt1052() throws Exception {
        checkOutput("KT-1052.kt", "true\n");
    }

    public void testKt740_1() throws Exception {
        checkFooBoxIsTrue("KT-740.kt");
    }

    public void testKt740_2() throws Exception {
        checkFooBoxIsOk("KT-740-2.kt");
    }

    public void testKt1361_1() throws Exception {
        checkFooBoxIsTrue("KT-1361-1.kt");
    }

    public void testKt1361_2() throws Exception {
        checkFooBoxIsTrue("KT-1361-2.kt");
    }

    public void testKt817() throws Exception {
        checkFooBoxIsTrue("KT-817.kt");
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

    public void testPackagePropertyCalledAsFun() throws Exception {
        fooBoxTest();
    }

    public void testExtensionLiteralCreatedAtPackageLevel() throws Exception {
        fooBoxTest();
    }

    public void testTemporaryVariableCreatedInPackageInitializer() throws Exception {
        fooBoxTest();
    }

    public void testWhenReturnedWithoutBlock() throws Exception {
        fooBoxTest();
    }

    public void testElvis() throws Exception {
        fooBoxTest();
    }

    public void testElvisReturnSimple() throws Exception {
        checkFooBoxIsOk();
    }

    public void testElvisReturnNested() throws Exception {
        checkFooBoxIsOk();
    }

    public void testElvisWithThrow() throws Exception {
        checkFooBoxIsOk();
    }

    public void testExtensionLiteralCalledInsideExtensionFunction() throws Exception {
        fooBoxTest();
    }

    public void testKt1865() throws Exception {
        checkFooBoxIsTrue("KT-1865.kt");
    }

    public void testMainFunInNestedPackage() throws Exception {
        checkOutput("mainFunInNestedPackage.kt", "ayee");
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

    public void testPackageLevelVarInPackage() throws Exception {
        fooBoxIsValue("OK");
    }

    public void testPackageLevelVarInRoot() throws Exception {
        runFunctionOutputTest("packageLevelVarInRoot.kt", Namer.getRootPackageName(), TEST_FUNCTION, "OK");
    }

    public void testLazyPropertyGetterNotCalledOnStart() throws Exception {
        checkOutput("lazyProperty.kt", "Hello, world! Gotcha 3");
    }

    public void testLocalVarAsFunction() throws Exception {
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

    public void testInheritFromJetIterator() throws Exception {
        fooBoxTest();
    }

    public void testStringInterpolationEvaluationOrder() throws Exception {
        fooBoxTest();
    }

    public void testKt5058() throws Exception {
        checkFooBoxIsTrue("KT-5058.kt");
    }

    public void testRightAssocForGeneratedConditionalOperator() throws Exception {
        checkFooBoxIsOk();
    }
}
