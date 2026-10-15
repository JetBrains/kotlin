/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.services.TestServices

/**
 * To check klib compatibility, it's enough to execute an image on any single most reliable engine:
 * V8 for Wasm/JS and Node.js for WASI.
 * Other engines (SpiderMonkey, JavaScriptCore, WasmEdge, Wasmtime) are not configured for KLIB-compatibility tests at all
 */
class WasmCompilationSetsGroupingStageBoxWithSingleVmRunner(
    testServices: TestServices,
) : WasmCompilationSetsGroupingStageBoxRunner(testServices) {
    override val wasmBoxRunner: WasmBoxRunner
        get() = WasmBoxRunner(firstNonGroupingTestServices, executeWithV8Only = true)

    override val wasiBoxRunner: WasiBoxRunner
        get() = WasiBoxRunner(firstNonGroupingTestServices, executeWithNodeJsOnly = true)
}
