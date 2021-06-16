/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.uast.UFile
import org.jetbrains.uast.test.common.kotlin.UastResolveApiTestBase
import org.jetbrains.uast.test.env.kotlin.AbstractFE1UastTest
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit3RunnerWithInners::class)
class FE1UastResolveApiTest : AbstractFE1UastTest() {
    override fun check(testName: String, file: UFile) {
        // Bogus
    }

    @TestMetadata("plugins/uast-kotlin/testData")
    @TestDataPath("\$PROJECT_ROOT")
    class Legacy : AbstractFE1UastTest(), UastResolveApiTestBase {
        override var testDataDir = File("plugins/uast-kotlin/testData")

        override val isFirUastPlugin: Boolean = false

        override fun check(testName: String, file: UFile) {
            // Bogus
        }

        @TestMetadata("MethodReference.kt")
        fun testMethodReference() {
            doTest("MethodReference", ::checkCallbackForMethodReference)
        }

        @TestMetadata("Imports.kt")
        fun testImports() {
            doTest("Imports", ::checkCallbackForImports)
        }

        @TestMetadata("ReceiverFun.kt")
        fun testReceiverFun() {
            doTest("ReceiverFun", ::checkCallbackForReceiverFun)
        }
    }
}
