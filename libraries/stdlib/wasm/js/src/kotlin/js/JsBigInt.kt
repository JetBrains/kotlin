/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.wasm.internal.*
import kotlin.wasm.internal.JsPrimitive

/** JavaScript primitive bigint */
@JsPrimitive("bigint")
public external class JsBigInt internal constructor() : JsAny

public fun JsBigInt.toLong(): Long =
    externRefToKotlinLongAdapter(this)

public fun Long.toJsBigInt(): JsBigInt =
    kotlinLongToExternRefAdapter(this)