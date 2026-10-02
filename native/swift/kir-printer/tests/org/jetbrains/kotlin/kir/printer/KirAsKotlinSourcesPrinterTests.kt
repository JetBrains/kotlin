/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.printer

import org.jetbrains.kotlin.kir.KirModule
import org.jetbrains.kotlin.kir.builder.buildFunction
import org.jetbrains.kotlin.kir.builder.buildModule
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.Test
import java.io.File

class KirAsKotlinSourcesPrinterTests {

    @Test
    fun `should print empty module`() {
        runTest(buildModule {}, "testData/empty_module")
    }

    @Test
    fun `should print top level function`() {
        val module = buildModule {
            functions.add(buildFunction { name = "foo" })
        }

        runTest(module, "testData/top_level_function")
    }

    @Test
    fun `should print multiple functions`() {
        val module = buildModule {
            functions.add(buildFunction { name = "foo1" })
            functions.add(buildFunction { name = "foo2" })
        }

        runTest(module, "testData/multiple_functions")
    }

    private fun runTest(module: KirModule, goldenDataFile: String) {
        val expected = File(KtTestUtil.getHomeDirectory()).resolve("$goldenDataFile.golden.kt")
        val actual = KirPrinter().print(module)
        JUnit5Assertions.assertEqualsToFile(expected, actual)
    }
}
