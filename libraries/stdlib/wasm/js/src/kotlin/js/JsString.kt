/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.wasm.internal.JsPrimitive
import kotlin.wasm.internal.kotlinToJsStringAdapter

/** JavaScript primitive string */
@JsPrimitive("string")
public open external class JsString internal constructor() : JsAny

internal external class JsStringRef internal constructor(ref: JsAny) : JsString

public fun String.toJsString(): JsString =
    kotlinToJsStringAdapter(this)!!