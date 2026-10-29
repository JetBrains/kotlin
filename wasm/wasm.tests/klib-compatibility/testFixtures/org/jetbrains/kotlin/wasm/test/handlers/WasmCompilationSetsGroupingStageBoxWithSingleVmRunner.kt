/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.services.TestServices

/**
 * To check klib compatibility, it's enough to execute an image on any single most reliable engine:
 * V8 for Wasm/JS and Wasmtime for WASI, which already is the only engine of [WasiBoxRunner].
 * Other engines (SpiderMonkey, JavaScriptCore) are not configured for KLIB-compatibility tests at all
 */
class WasmCompilationSetsGroupingStageBoxWithSingleVmRunner(
    testServices: TestServices,
) : WasmCompilationSetsGroupingStageBoxRunner(testServices) {
    override val wasmBoxRunner: WasmBoxRunner
        get() = WasmBoxRunner(firstNonGroupingTestServices, executeWithV8Only = true)

    // NOTE: Wasmtime is the only VM that WASI tests are run on, if we add another VM, consider overwriting this method to run on a single VM only
}
