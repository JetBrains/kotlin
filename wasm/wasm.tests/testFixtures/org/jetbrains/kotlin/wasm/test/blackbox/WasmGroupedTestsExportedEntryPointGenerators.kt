/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.test.grouping.GroupedTestsExportedEntryPointGenerator
import org.jetbrains.kotlin.wasm.test.handlers.WasmBoxRunnerBase
import org.jetbrains.kotlin.wasm.test.handlers.WasmWasiFolderGroupingStageBoxRunner

/** wasm-js: the `test.mjs` glue written by [WasmBoxRunnerBase.saveAdditionalFilesAndRun] calls this export. */
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
 * Generates the `startTest` export used by [WasmWasiFolderGroupingStageBoxRunner] to launch a grouped WASI batch.
 * The export invokes the generated driver, which runs every proxy launcher and emits the structured test results.
 *
 * The standalone `wasiBoxTestRun.kt` helper also declares `startTest`, but grouped launchers call each test's `box()`
 * directly by its fully qualified name, so that helper is not linked into the grouped binary.
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
