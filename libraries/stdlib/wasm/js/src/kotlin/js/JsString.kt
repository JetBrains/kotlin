/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.wasm.internal.JsPrimitive

/** JavaScript primitive string */
@JsPrimitive("string")
@ExperimentalWasmJsInterop
public actual external class JsString internal constructor() : JsAny

@ExperimentalWasmJsInterop
public actual fun String.toJsString(): JsString =
    this.internalStr
