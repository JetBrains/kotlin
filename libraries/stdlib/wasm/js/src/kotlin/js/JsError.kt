/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.wasm.internal.ExternalInterfaceType

@SinceKotlin("2.2")
@JsName("Error")
@ExperimentalWasmJsInterop
public actual external class JsError : JsAny {
    internal val message: String
    internal var name: String
    internal val stack: ExternalInterfaceType
    internal val cause: JsError?
    internal var kotlinException: JsReference<Throwable>?
}

