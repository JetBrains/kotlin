/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.test.grouping.GroupedTestsExportedEntryPointGenerator
import org.jetbrains.kotlin.wasm.test.handlers.WasmBoxRunnerBase
import org.jetbrains.kotlin.wasm.test.handlers.WasmWasiFolderGroupingStageBoxRunner

/**
 * wasm-js: the driver is reached through a `@JsExport`ed `runGroupedTests()`, which the `test.mjs` glue written by
 * [WasmBoxRunnerBase.saveAdditionalFilesAndRun] calls as `jsModule.runGroupedTests()`.
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
 * wasm-wasi: the driver is reached through a `@kotlin.wasm.WasmExport`ed `startTest()`. The standalone WASI VMs
 * (WasmEdge/Wasmtime) invoke that export directly; under Node.js the `test.mjs` written by
 * [WasmWasiFolderGroupingStageBoxRunner] calls `jsModule.startTest()`.
 *
 * `wasiBoxTestRun.kt` gives every test with a `box()` a `startTest()` of its own, and sharing the name with this one is
 * safe because the two never end up in the same binary. Not because exports are filtered — codegen exports any
 * `@WasmExport` declaration it visits, from any module — but because the helper never enters the link at all: it
 * travels in the per-test KLIBs, which a grouped batch links as ordinary `-libraries`, whose declarations are only
 * deserialized when something references them, and nothing references the helper (the launcher calls each `box()` by
 * its FQN). Verified on a linked dev binary: the grouped module contains no `runBoxTest` anywhere in its bytes and
 * exports a single `startTest` — the driver's — while an isolated binary (the helper's KLIB being its `-Xinclude` main
 * module, deserialized eagerly) exports `startTest` and `runBoxTest`. That is what lets the standalone VMs, which can
 * invoke nothing but a bare export name, still reach the driver for a grouped batch; a misresolved entry cannot pass
 * silently either way, since a batch whose output carries no result block fails every test via the missing-block
 * guard. The name is *not* a safe signal on the JVM side, though — see
 * [org.jetbrains.kotlin.wasm.test.handlers.startUnitTestsWasiScript] for why Node dispatch keys off the driver marker
 * instead of probing the exports.
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
