/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.wasm.internal.JsPrimitive
import kotlin.wasm.internal.externRefToKotlinBooleanAdapter
import kotlin.wasm.internal.kotlinBooleanToExternRefAdapter

/** JavaScript primitive boolean */
@JsPrimitive("boolean")
public external class JsBoolean internal constructor() : JsAny

public fun JsBoolean.toBoolean(): Boolean =
    externRefToKotlinBooleanAdapter(this)

public fun Boolean.toJsBoolean(): JsBoolean =
    kotlinBooleanToExternRefAdapter(this)