/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.unsafe.ComponentModelInternalApi
import kotlin.wasm.unsafe.componentModelRealloc

// internal because it should not be directly callable, just needs to be exported for component model support
@OptIn(ComponentModelInternalApi::class, ExperimentalWasmInterop::class)
@Suppress("FunctionName", "unused")
@UsedFromCompilerGeneratedCode
@WasmExport
internal fun cabi_realloc(ptr: Int, oldSize: Int, align: Int, newSize: Int): Int =
    componentModelRealloc(ptr, oldSize, newSize)
