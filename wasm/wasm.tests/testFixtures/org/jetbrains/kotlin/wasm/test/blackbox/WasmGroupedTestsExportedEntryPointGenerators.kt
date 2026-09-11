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
 * wasm-wasi: the standalone VMs invoke this export directly, being able to invoke nothing but a bare name; under
 * Node.js the `test.mjs` written by [WasmWasiFolderGroupingStageBoxRunner] calls it.
 *
 * `wasiBoxTestRun.kt` gives every test with a `box()` a `startTest()` of its own, and sharing the name is safe — not
 * because exports are filtered, but because the helper never enters a grouped link: it travels in the per-test KLIBs,
 * which the batch links as ordinary `-libraries`, whose declarations are deserialized only when referenced, and
 * nothing references the helper (the launcher calls each `box()` by its FQN). Verified on a linked binary and enforced
 * per run by `assertDriverOwnsStartTestExport`. On the JVM side the name is not a usable signal, though — see
 * [org.jetbrains.kotlin.wasm.test.handlers.startUnitTestsWasiScript].
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
