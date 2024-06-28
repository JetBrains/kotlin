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
public external class JsNumber internal constructor() : JsAny

public fun JsNumber.toDouble(): Double =
    externRefToKotlinDoubleAdapter(this)

public fun Double.toJsNumber(): JsNumber =
    kotlinDoubleToExternRefAdapter(this)

public fun JsNumber.toInt(): Int =
    externRefToKotlinIntAdapter(this)

public fun Int.toJsNumber(): JsNumber =
    kotlinIntToExternRefAdapter(this)