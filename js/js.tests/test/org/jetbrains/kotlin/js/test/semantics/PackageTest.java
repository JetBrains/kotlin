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

public final class PackageTest extends SingleFileTranslationTest {
    public PackageTest() {
        super("package/");
    }

    public void testNestedPackage() throws Exception {
        runFunctionOutputTest("nestedPackage.kt", "foo.bar", TEST_FUNCTION, true);
    }

    public void testDeeplyNestedPackage() throws Exception {
        runFunctionOutputTest("deeplyNestedPackage.kt", "foo1.foo2.foo3.foo5.foo6.foo7.foo8", TEST_FUNCTION, true);
    }

    public void testDeeplyNestedPackageFunctionCalled() throws Exception {
        runFunctionOutputTest("deeplyNestedPackageFunctionCalled.kt", "foo1.foo2.foo3.foo5.foo6.foo7.foo8", TEST_FUNCTION, true);
    }

    public void testClassCreatedInDeeplyNestedPackage() throws Exception {
        runFunctionOutputTest("classCreatedInDeeplyNestedPackage.kt", "foo1.foo2.foo3.foo5.foo6.foo7.foo8", TEST_FUNCTION, true);
    }

    public void testInitializersOfNestedPackagesExecute() throws Exception {
        runFunctionOutputTest("initializersOfNestedPackagesExecute.kt", "foo1.foo2.foo3.foo5.foo6.foo7.foo8", TEST_FUNCTION, true);
    }

    public void testMainFunInNestedPackage() throws Exception {
        checkOutput("mainFunInNestedPackage.kt", "ayee");
    }
}
