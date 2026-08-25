/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// HACK (KT-87723): `wasm-tools component new` requires the module to export `cabi_realloc`. The stdlib has such an
// export (`kotlin.wasm.internal.cabi_realloc`, kept alive via `BackendWasmSymbols.cabiRealloc`), but the bootstrap
// compiler that links the stdlib test binary does not have that yet, so the export is dropped from the linked test
// module. Re-exporting it from the test compilation itself works around that.
// Remove this file (a duplicate export is a compilation error) once the bootstrap compiler keeps the stdlib export.

@file:OptIn(
    kotlin.wasm.unsafe.ComponentModelInternalApi::class,
    kotlin.wasm.ExperimentalWasmInterop::class,
)

package test

import kotlin.wasm.WasmExport
import kotlin.wasm.unsafe.componentModelRealloc

@WasmExport("cabi_realloc")
@Suppress("unused", "FunctionName")
internal fun cabi_realloc_test_hack(ptr: Int, oldSize: Int, align: Int, newSize: Int): Int =
    componentModelRealloc(ptr, oldSize, newSize)
