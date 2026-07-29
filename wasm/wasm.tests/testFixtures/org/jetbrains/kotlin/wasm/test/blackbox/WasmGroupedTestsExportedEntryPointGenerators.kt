/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.test.grouping.GroupedTestsExportedEntryPointGenerator
import org.jetbrains.kotlin.wasm.test.handlers.WasmWasiFolderGroupingStageBoxRunner
import org.jetbrains.kotlin.wasm.test.handlers.WasmBoxRunnerBase

/**
 * wasm-js flavor of [GroupedTestsExportedEntryPointGenerator]: the batch driver is reached through a `@JsExport`ed
 * `runGroupedTests()` function, which the `test.mjs` glue written by [WasmBoxRunnerBase.saveAdditionalFilesAndRun]
 * calls as `jsModule.runGroupedTests()`.
 */
object WasmJsGroupedTestsExportedEntryPointGenerator : GroupedTestsExportedEntryPointGenerator() {
    override fun generateExportedEntryPointSource(runAllFunctionName: String): String =
        """
        @JsExport
        fun runGroupedTests() {
            $runAllFunctionName()
        }
        """.trimIndent()
}

/**
 * wasm-wasi flavor of [GroupedTestsExportedEntryPointGenerator]: the batch driver is reached through a
 * `@kotlin.wasm.WasmExport`ed `startTest()` function. The standalone WASI VMs (WasmEdge/Wasmtime) invoke that
 * export directly, while under Node.js the `test.mjs` written by [WasmWasiFolderGroupingStageBoxRunner] calls
 * `jsModule.startTest()`.
 */
object WasmWasiGroupedTestsExportedEntryPointGenerator : GroupedTestsExportedEntryPointGenerator() {
    override fun generateExportedEntryPointSource(runAllFunctionName: String): String =
        """
        @kotlin.wasm.WasmExport
        fun startTest() {
            $runAllFunctionName()
        }
        """.trimIndent()
}
