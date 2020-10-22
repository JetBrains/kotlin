/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:ExcludedFromCodegen
@file:Suppress("unused")

package kotlin.wasm.internal

@WasmOp(WasmOp.UNREACHABLE)
internal fun wasm_unreachable(): Nothing =
    implementedAsIntrinsic

internal fun wasm_float_nan(): Float =
    implementedAsIntrinsic

internal fun wasm_double_nan(): Double =
    implementedAsIntrinsic

internal fun <From, To> wasm_ref_cast(a: From): To =
    implementedAsIntrinsic