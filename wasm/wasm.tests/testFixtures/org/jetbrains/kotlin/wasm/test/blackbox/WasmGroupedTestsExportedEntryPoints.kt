/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

/** The wasm-js export of a grouped batch: `test.mjs` calls it to run the result-collecting driver. */
internal fun wasmJsGroupedTestsEntryPointSource(runAllFunctionName: String): String =
    """
    @JsExport
    fun runGroupedTests() {
        $runAllFunctionName()
    }
    """.trimIndent()

/**
 * The WASI export of a grouped batch: every WASI VM invokes it to run the result-collecting driver.
 *
 * TODO KT-87841: Unify this generated startTest code with `wasm/wasm.tests/_additionalFilesForTests/wasiAdditionalFiles/wasiBoxTestRun.kt`
 */
internal fun wasmWasiGroupedTestsEntryPointSource(runAllFunctionName: String): String =
    """
    @kotlin.wasm.WasmExport
    fun startTest() {
        $runAllFunctionName()
    }
    """.trimIndent()
