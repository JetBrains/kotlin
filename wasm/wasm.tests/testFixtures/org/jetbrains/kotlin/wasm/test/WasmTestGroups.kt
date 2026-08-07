/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.junit.jupiter.api.Tag

/*
 * Tests of the `:wasm:wasm.tests` module are split into several Gradle test tasks by the JUnit 5 tags declared below.
 *
 * There are two ways of putting a tag on a test:
 * - annotating the most common supertype of all tests belonging to the group, so generated test suites inherit the
 *   tag automatically. It is used for the groups which consist of a whole hierarchy of tests: [WasmIcTest],
 *   [WasmJsMultiModuleTest] and [WasmFirCompilerExtraTest];
 * - passing the annotation to the corresponding `testClass` call in `GenerateWasmTests.kt`, so it is put only on the
 *   generated suite itself. It is used for the groups which consist of a single test runner: [WasmJsBoxTest],
 *   [WasmJsBoxInlinedTest], [WasmJsSplittingTest], [WasmWasiBoxTest] and [WasmWasiBoxInlinedTest]. Their base classes
 *   are supertypes of tests from other groups, so the tag can not be put on them.
 *
 * The groups are disjoint: [WasmFirCompilerExtraTest] wins over [WasmIcTest], and everything which is left
 * unannotated is run by the `wasmMiscTest` task.
 */

const val WASM_IC_TEST_TAG = "wasmIc"
const val WASM_JS_BOX_TEST_TAG = "wasmJsBox"
const val WASM_JS_SPLITTING_TEST_TAG = "wasmJsSplitting"
const val WASM_JS_MULTI_MODULE_TEST_TAG = "wasmJsMultiModule"
const val WASM_WASI_BOX_TEST_TAG = "wasmWasiBox"
const val WASM_FIR_COMPILER_EXTRA_TEST_TAG = "wasmFirCompilerExtra"

/**
 * Tests of incremental compilation. Executed by the `wasmIcTest` Gradle task.
 */
@Tag(WASM_IC_TEST_TAG)
annotation class WasmIcTest

/**
 * Codegen box tests for wasm-js (`WasmJsCodegenBoxTestGenerated`). Executed by the `wasmJsBoxTest` Gradle task.
 */
@Tag(WASM_JS_BOX_TEST_TAG)
annotation class WasmJsBoxTest

/**
 * Codegen box tests for wasm-js in the splitting mode (`WasmJsCodegenSplittingTestGenerated`).
 * Executed by the `wasmJsSplittingTest` Gradle task.
 */
@Tag(WASM_JS_SPLITTING_TEST_TAG)
annotation class WasmJsSplittingTest

/**
 * Tests of the closed world multimodule mode for wasm-js. Executed by the `wasmJsMultiModuleTest` Gradle task.
 */
@Tag(WASM_JS_MULTI_MODULE_TEST_TAG)
annotation class WasmJsMultiModuleTest

/**
 * Codegen box tests for wasm-wasi (`WasmWasiCodegenBoxTestGenerated`). Executed by the `wasmWasiBoxTest` Gradle task.
 */
@Tag(WASM_WASI_BOX_TEST_TAG)
annotation class WasmWasiBoxTest

/**
 * Tests of the extra compiler configurations (single module and closed world multimodule modes).
 * Executed by the `wasmFirCompilerExtraTest` Gradle task.
 *
 * This tag takes precedence over [WasmIcTest]: a test annotated with both of them is run only by the
 * `wasmFirCompilerExtraTest` task. Multimodule codegen tests are carved out of this group by [WasmJsMultiModuleTest].
 */
@Tag(WASM_FIR_COMPILER_EXTRA_TEST_TAG)
annotation class WasmFirCompilerExtraTest
