/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

@ExperimentalWasmJsInterop
@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmMemoryInternal(): kotlin.wasm.unsafe.WebAssembly.Memory =
    implementedAsIntrinsic
