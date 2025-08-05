/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.wasm.internal.JsPrimitive
import kotlin.wasm.internal.kotlinToJsStringAdapter
import kotlin.wasm.internal.ExcludedFromCodegen

/** JavaScript primitive string */
@JsPrimitive("string")
@ExperimentalWasmJsInterop
public actual open external class JsString internal constructor() : JsAny

@ExcludedFromCodegen
@ExperimentalWasmJsInterop
internal external class JsStringRef internal constructor() : JsString

@ExperimentalWasmJsInterop
public actual fun String.toJsString(): JsString =
    kotlinToJsStringAdapter(this)!!
