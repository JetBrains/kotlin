/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * Any JavaScript value except null or undefined for WasmJs interop
 */
@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
public expect interface JsAny

/**
 * Cast JsAny to other Js type without runtime check for WasmJs interop
 */
@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
public expect fun <T : JsAny> JsAny.unsafeCast(): T