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

package org.jetbrains.kotlin.idea.quickfix

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.stubs.createFacet
import org.junit.Test

class QuickFixMultiModuleTest : AbstractQuickFixMultiModuleTest() {
    private fun doMultiPlatformTest(
            headerName: String = "header",
            vararg impls: Pair<String, TargetPlatformKind<*>> = arrayOf("jvm" to TargetPlatformKind.Jvm[JvmTarget.JVM_1_6]),
            withTests: Boolean = false
    ) {
        val header = module(headerName, hasTestRoot = withTests)
        header.createFacet(TargetPlatformKind.Common)

        impls.forEach { (implName, implKind) ->
            val implModule = module(implName, hasTestRoot = withTests)
            implModule.createFacet(implKind)
            implModule.enableMultiPlatform()
            implModule.addDependency(header)
        }

        doQuickFixTest()
    }

    private fun doTestHeaderWithJvmAndJs() {
        doMultiPlatformTest(impls = *arrayOf("jvm" to TargetPlatformKind.Jvm[JvmTarget.JVM_1_6], "js" to TargetPlatformKind.JavaScript))
    }

    @Test
    fun testAbstract() {
        doMultiPlatformTest(impls = "js" to TargetPlatformKind.JavaScript)
    }

    @Test
    fun testClass() {
        doMultiPlatformTest()
    }

    @Test
    fun testEnum() {
        doMultiPlatformTest(impls = "js" to TargetPlatformKind.JavaScript)
    }

    @Test
    fun testFunction() {
        doMultiPlatformTest()
    }

    @Test
    fun testInterface() {
        doMultiPlatformTest()
    }

    @Test
    fun testObject() {
        doMultiPlatformTest()
    }

    @Test
    fun testPackage() {
        doMultiPlatformTest()
    }

    @Test
    fun testPackageIncorrect() {
        doMultiPlatformTest()
    }

    @Test
    fun testPackageIncorrectEmpty() {
        doMultiPlatformTest()
    }

    @Test
    fun testNested() {
        doMultiPlatformTest()
    }

    @Test
    fun testProperty() {
        doMultiPlatformTest()
    }

    @Test
    fun testSealed() {
        doMultiPlatformTest(impls = "js" to TargetPlatformKind.JavaScript)
    }

    @Test
    fun testWithTest() {
        doMultiPlatformTest(headerName = "common", withTests = true)
    }

    @Test
    fun testMemberFunToExtensionByHeader() = doTestHeaderWithJvmAndJs()

    @Test
    fun testMemberFunToExtensionByImpl() = doTestHeaderWithJvmAndJs()

    @Test
    fun testMemberValToExtensionByHeader() = doTestHeaderWithJvmAndJs()

    @Test
    fun testMemberValToExtensionByHeaderWithInapplicableImpl() = doTestHeaderWithJvmAndJs()

    @Test
    fun testMemberValToExtensionByImpl() = doTestHeaderWithJvmAndJs()

    @Test
    fun testAddOperatorByHeader() = doTestHeaderWithJvmAndJs()

    @Test
    fun testAddOperatorByImpl() = doTestHeaderWithJvmAndJs()
}