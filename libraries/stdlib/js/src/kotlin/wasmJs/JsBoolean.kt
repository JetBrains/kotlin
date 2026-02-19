/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/** JavaScript primitive boolean */
@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
public actual typealias JsBoolean = Boolean

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
@Suppress("NOTHING_TO_INLINE")
public actual inline fun JsBoolean.toBoolean(): Boolean = unsafeCast<Boolean>()

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
@Suppress("NOTHING_TO_INLINE")
public actual inline fun Boolean.toJsBoolean(): JsBoolean = unsafeCast<JsBoolean>()
