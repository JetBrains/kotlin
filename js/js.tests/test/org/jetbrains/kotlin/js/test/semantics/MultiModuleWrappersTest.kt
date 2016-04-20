/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.test.semantics

import org.jetbrains.kotlin.js.test.MultipleModulesTranslationTest
import org.jetbrains.kotlin.js.test.WithModuleKind
import org.jetbrains.kotlin.serialization.js.ModuleKind

class MultiModuleWrappersTest() : MultipleModulesTranslationTest("multiModuleWrappers/") {
    private var overridenTestName = ""

    @WithModuleKind(ModuleKind.AMD) fun testAmd() {
        runTest("simple")
    }

    @WithModuleKind(ModuleKind.COMMON_JS) fun testCommonJs() {
        runTest("simple")
    }

    @WithModuleKind(ModuleKind.UMD) fun testUmd() {
        runTest("simple")
    }

    @WithModuleKind(ModuleKind.PLAIN) fun testPlain() {
        runTest("simple")
    }

    @WithModuleKind(ModuleKind.AMD) fun testAmdModuleWithNonIdentifierName() {
        runTest("moduleWithNonIdentifierName")
    }

    @WithModuleKind(ModuleKind.COMMON_JS) fun testCommonJsModuleWithNonIdentifierName() {
        runTest("moduleWithNonIdentifierName")
    }

    @WithModuleKind(ModuleKind.UMD) fun testUmdModuleWithNonIdentifierName() {
        runTest("moduleWithNonIdentifierName")
    }

    @WithModuleKind(ModuleKind.PLAIN) fun testPlainModuleWithNonIdentifierName() {
        runTest("moduleWithNonIdentifierName")
    }

    fun runTest(name: String) {
        overridenTestName = name
        doTest("${pathToTestDir()}/cases/$name")
    }

    override fun getTestName(lowercaseFirstLetter: Boolean) = overridenTestName

    override fun getOutputPath() = "${super.getOutputPath()}/${moduleKind.name.toLowerCase()}/"
}
