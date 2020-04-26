/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:ExcludedFromCodegen
@file:Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY", "unused")

package kotlin.wasm.internal

external fun wasm_unreachable(): Nothing

fun wasm_float_nan(): Float =
    implementedAsIntrinsic

fun wasm_double_nan(): Double =
    implementedAsIntrinsic

fun <From, To> wasm_struct_narrow(a: From): To =
    implementedAsIntrinsic