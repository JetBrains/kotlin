/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.wasm.internal.JsPrimitive
import kotlin.wasm.internal.externRefToKotlinDoubleAdapter
import kotlin.wasm.internal.externRefToKotlinIntAdapter
import kotlin.wasm.internal.kotlinDoubleToExternRefAdapter
import kotlin.wasm.internal.kotlinIntToExternRefAdapter

/** JavaScript primitive number */
@JsPrimitive("number")
@ExperimentalWasmJsInterop
public actual external class JsNumber internal constructor() : JsAny

@ExperimentalWasmJsInterop
public actual fun JsNumber.toDouble(): Double =
    externRefToKotlinDoubleAdapter(this)

@ExperimentalWasmJsInterop
public actual fun Double.toJsNumber(): JsNumber =
    kotlinDoubleToExternRefAdapter(this)

@ExperimentalWasmJsInterop
public actual fun JsNumber.toInt(): Int =
    externRefToKotlinIntAdapter(this)

@ExperimentalWasmJsInterop
public actual fun Int.toJsNumber(): JsNumber =
    kotlinIntToExternRefAdapter(this)
