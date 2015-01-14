/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.test.semantics;

import org.jetbrains.kotlin.js.test.SingleFileTranslationTest;

public final class ExtensionFunctionTest extends SingleFileTranslationTest {

    public ExtensionFunctionTest() {
        super("extensionFunction/");
    }

    public void testIntExtension() throws Exception {
        fooBoxTest();
    }

    public void testExtensionWithImplicitReceiver() throws Exception {
        fooBoxTest();
    }

    public void testExtensionFunctionOnExpression() throws Exception {
        fooBoxTest();
    }

    public void testExtensionUsedInsideClass() throws Exception {
        fooBoxTest();
    }

    public void testVirtualExtension() throws Exception {
        fooBoxTest();
    }

    public void testVirtualExtensionOverride() throws Exception {
        fooBoxTest();
    }

    public void testExtensionLiteralPassedToFunction() throws Exception {
        fooBoxTest();
    }

    public void testExtensionInsideFunctionLiteral() throws Exception {
        fooBoxTest();
    }

    public void testGenericExtension() throws Exception {
        checkFooBoxIsOk("generic.kt");
    }

    public void testExtensionFunctionCalledFromExtensionFunction() throws Exception {
        fooBoxTest();
    }

    public void testExtensionOnClassWithExplicitAndImplicitReceiver() throws Exception {
        fooBoxTest();
    }

    public void testExtensionPropertyOnClassWithExplicitAndImplicitReceiver() throws Exception {
        fooBoxTest();
    }
    
    public void testExtensionFunctionCalledFromFor() throws Exception {
        fooBoxTest();
    }

    public void testImplicitReceiverInExtension() throws Exception {
        fooBoxTest();
    }

    public void testExtensionForSuperclass() throws Exception {
        fooBoxTest();
    }

    public void testSuperClassMemberInExtension() throws Exception {
        fooBoxTest();
    }
}
