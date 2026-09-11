/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

/** A runner-specific directory name paired with the compilation payload executed for that mode. */
internal data class WasmCompilationMode<T>(
    val directoryName: String,
    val compilation: T,
)

/** Returns the available Wasm modes in execution order, without coupling mode policy to an artifact model. */
internal fun <T> wasmCompilationModes(
    compilation: T,
    dceCompilation: T?,
    optimisedCompilation: T?,
): List<WasmCompilationMode<T>> = buildList {
    add(WasmCompilationMode("dev", compilation))
    dceCompilation?.let { add(WasmCompilationMode("dce", it)) }
    optimisedCompilation?.let { add(WasmCompilationMode("optimized", it)) }
}
