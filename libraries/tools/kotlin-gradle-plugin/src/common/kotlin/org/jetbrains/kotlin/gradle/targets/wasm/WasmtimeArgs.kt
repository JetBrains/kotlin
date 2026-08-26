/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm

/**
 * Name under which the compiler exports the unit test runner entry point of a WASI test binary.
 *
 * Kebab-case, since WASI binaries are Component Model components (or become ones), whose export names have to be WIT
 * labels; see `wasmWasiUnitTestsExportName` in the Wasm backend.
 */
internal const val WASI_UNIT_TESTS_ENTRY_POINT = "start-unit-tests"

internal fun wasmtimeInvokeArgs(functionName: String) = listOf(
    "--invoke",
    functionName
)
