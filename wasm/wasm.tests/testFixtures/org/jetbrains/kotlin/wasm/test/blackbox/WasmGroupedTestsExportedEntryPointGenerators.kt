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
 * `runGroupedTests()` function (the JS glue in `WasmBoxRunnerBase` calls `jsModule.runGroupedTests()`).
 */
object WasmJsGroupedTestsExportedEntryPointGenerator : GroupedTestsExportedEntryPointGenerator() {
    /**
     * Generates exported `runGroupedTests()` to be invoked by [WasmBoxRunnerBase.saveAdditionalFilesAndRun]
     */
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
 * `@kotlin.wasm.WasmExport`ed `startTest()` function, which the standalone WASI VMs (WasmEdge/Wasmtime) invoke
 * directly and the Node.js WASI launcher calls via `jsModule.startTest()`.
 */
object WasmWasiGroupedTestsExportedEntryPointGenerator : GroupedTestsExportedEntryPointGenerator() {
    /**
     * Generates exported `startTest()` to be invoked with `startUnitTestsWasiScript()` in [WasmWasiFolderGroupingStageBoxRunner]
     */
    override fun generateExportedEntryPointSource(runAllFunctionName: String): String =
        """
        @kotlin.wasm.WasmExport
        fun startTest() {
            $runAllFunctionName()
        }
        """.trimIndent()
}
