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

import org.mozilla.javascript.JavaScriptException;

/**
 * @author Pavel Talanov
 *         <p/>
 *         This class contains tests that do not fall in any particular category
 *         most probably because that functionality has very little support
 */
public final class MiscTest extends AbstractExpressionTest {


    public MiscTest() {
        super("misc/");
    }

    public void testLocalPropertys() throws Exception {
        runFunctionOutputTest("localProperty.jet", "foo", "box", 50);
    }

    public void testIntRange() throws Exception {
        checkFooBoxIsTrue("intRange.kt");
    }


    public void testSafecallComputesExpressionOnlyOnce() throws Exception {
        checkFooBoxIsTrue("safecallComputesExpressionOnlyOnce.kt");
    }

    public void testClassWithoutNamespace() throws Exception {
        runFunctionOutputTest("classWithoutNamespace.kt", "Anonymous", "box", true);
    }

    public void testIfElseAsExpressionWithThrow() throws Exception {
        try {
            checkFooBoxIsTrue("ifAsExpressionWithThrow.kt");
            fail();
        } catch (JavaScriptException e) {
        }
    }

    public void testKt1052_2() throws Exception {
        checkFooBoxIsTrue("KT-1052-2.kt");
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
        checkFooBoxIsTrue("funInConstructor.kt");
    }

    public void testFunInConstructorBlock() throws Exception {
        checkFooBoxIsTrue("funInConstructorBlock.kt");
    }

    public void testPropertyAsFunCalledOnConstructor() throws Exception {
        checkFooBoxIsTrue("propertyAsFunCalledOnConstructor.kt");
    }

    public void testNamespacePropertyCalledAsFun() throws Exception {
        checkFooBoxIsTrue("namespacePropertyCalledAsFun.kt");
    }

    public void testExtensionLiteralCreatedAtNamespaceLevel() throws Exception {
        checkFooBoxIsTrue("extensionLiteralCreatedAtNamespaceLevel.kt");
    }

    public void testTemporaryVariableCreatedInNamespaceInitializer() throws Exception {
        checkFooBoxIsTrue("temporaryVariableCreatedInNamespaceInitializer.kt");
    }
}
