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

import org.jetbrains.k2js.test.SingleFileTranslationTest;

public final class NamespaceTest extends SingleFileTranslationTest {
    public NamespaceTest() {
        super("namespace/");
    }

    public void testNestedNamespace() throws Exception {
        runFunctionOutputTest("nestedNamespace.kt", "foo.bar", "box", true);
    }

    public void testDeeplyNestedNamespace() throws Exception {
        runFunctionOutputTest("deeplyNestedNamespace.kt", "foo1.foo2.foo3.foo5.foo6.foo7.foo8", "box", true);
    }

    public void testDeeplyNestedNamespaceFunctionCalled() throws Exception {
        runFunctionOutputTest("deeplyNestedNamespaceFunctionCalled.kt", "foo1.foo2.foo3.foo5.foo6.foo7.foo8", "box", true
        );
    }

    public void testClassCreatedInDeeplyNestedNamespace() throws Exception {
        runFunctionOutputTest("classCreatedInDeeplyNestedNamespace.kt", "foo1.foo2.foo3.foo5.foo6.foo7.foo8", "box", true
        );
    }

    public void testInitializersOfNestedNamespacesExecute() throws Exception {
        runFunctionOutputTest("initializersOfNestedNamespacesExecute.kt", "foo1.foo2.foo3.foo5.foo6.foo7.foo8", "box", true
        );
    }
}
