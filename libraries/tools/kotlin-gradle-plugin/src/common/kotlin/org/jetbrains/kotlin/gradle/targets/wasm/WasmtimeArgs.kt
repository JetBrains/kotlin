/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm

/**
 * Name under which the compiler exports the unit test runner entry point of a Kotlin/Wasm test binary.
 *
 * All lowercase, since it has to be both a JavaScript identifier (wasm-js) and a WIT label (WASI test binaries are
 * Component Model components, or become ones); see `wasmUnitTestsExportName` in the Wasm backend.
 */
internal const val WASM_UNIT_TESTS_ENTRY_POINT = "startunittests"

internal fun wasmtimeInvokeArgs(functionName: String) = listOf(
    "--invoke",
    functionName
)
