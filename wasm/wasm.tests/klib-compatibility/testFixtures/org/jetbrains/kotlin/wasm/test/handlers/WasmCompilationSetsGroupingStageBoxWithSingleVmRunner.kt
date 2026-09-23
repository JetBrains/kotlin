/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.services.TestServices

/**
 * A [WasmCompilationSetsGroupingStageBoxRunner] that executes the box on a single VM per target:
 * V8 for Wasm/JS and Node.js for WASI.
 *
 * KLIB-compatibility tests set up only these engines, so the other ones (SpiderMonkey, JavaScriptCore, WasmEdge, Wasmtime)
 * must not be referenced there. It's enough to execute an image on any single reliable engine.
 */
class WasmCompilationSetsGroupingStageBoxWithSingleVmRunner(
    testServices: TestServices,
) : WasmCompilationSetsGroupingStageBoxRunner(testServices) {
    override val wasmBoxRunner: WasmBoxRunner
        get() = WasmBoxRunner(firstNonGroupingTestServices, executeWithV8Only = true)

    override val wasiBoxRunner: WasiBoxRunner
        get() = WasiBoxRunner(firstNonGroupingTestServices, executeWithNodeJsOnly = true)
}
