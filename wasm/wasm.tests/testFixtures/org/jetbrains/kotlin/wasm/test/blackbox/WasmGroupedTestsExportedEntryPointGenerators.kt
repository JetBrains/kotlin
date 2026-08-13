/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.test.grouping.GroupedTestsExportedEntryPointGenerator

object WasmJsGroupedTestsExportedEntryPointGenerator : GroupedTestsExportedEntryPointGenerator() {
    override fun generateExportedEntryPointSource(runAllFunctionName: String): String =
        """
        @JsExport
        fun runGroupedTests() {
            $runAllFunctionName()
        }
        """.trimIndent()
}

object WasmWasiGroupedTestsExportedEntryPointGenerator : GroupedTestsExportedEntryPointGenerator() {
    override fun generateExportedEntryPointSource(runAllFunctionName: String): String =
        """
        @kotlin.wasm.WasmExport
        fun startTest() {
            $runAllFunctionName()
        }
        """.trimIndent()
}
