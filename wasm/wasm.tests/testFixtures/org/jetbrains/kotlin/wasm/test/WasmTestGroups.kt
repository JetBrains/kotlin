/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.junit.jupiter.api.Tag

/*
 * Tests of the `:wasm:wasm.tests` module are split into several Gradle test tasks by the JUnit 5 tags declared below.
 *
 * The tags are supposed to be put on the most common supertype of all tests belonging to the group, so generated test
 * suites inherit them automatically. The groups are disjoint: [WasmFirCompilerExtraTest] wins over the other three,
 * and everything which is left unannotated is run by the `wasmMiscTest` task.
 */

const val WASM_IC_TEST_TAG = "wasmIc"
const val WASM_JS_TEST_TAG = "wasmJs"
const val WASM_WASI_TEST_TAG = "wasmWasi"
const val WASM_FIR_COMPILER_EXTRA_TEST_TAG = "wasmFirCompilerExtra"

/**
 * Tests of incremental compilation. Executed by the `wasmIcTest` Gradle task.
 */
@Tag(WASM_IC_TEST_TAG)
annotation class WasmIcTest

/**
 * Tests which execute the compiled Wasm code in a JS engine. Executed by the `wasmJsTest` Gradle task.
 *
 * Note that wasm-js tests which don't run the produced binary (diagnostic tests, IR text tests, KLIB dump tests)
 * are not a part of this group and belong to the `wasmMiscTest` task instead.
 */
@Tag(WASM_JS_TEST_TAG)
annotation class WasmJsTest

/**
 * Tests which execute the compiled Wasm code in a WASI runtime. Executed by the `wasmWasiTest` Gradle task.
 *
 * Note that wasm-wasi tests which don't run the produced binary (diagnostic tests) are not a part of this group
 * and belong to the `wasmMiscTest` task instead.
 */
@Tag(WASM_WASI_TEST_TAG)
annotation class WasmWasiTest

/**
 * Tests of the extra compiler configurations (single module and closed world multimodule modes).
 * Executed by the `wasmFirCompilerExtraTest` Gradle task.
 *
 * This tag takes precedence over [WasmIcTest], [WasmJsTest] and [WasmWasiTest]: a test annotated with it is run
 * only by the `wasmFirCompilerExtraTest` task.
 */
@Tag(WASM_FIR_COMPILER_EXTRA_TEST_TAG)
annotation class WasmFirCompilerExtraTest
