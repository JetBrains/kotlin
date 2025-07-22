/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/** JavaScript primitive number for WasmJs interop */
@ExperimentalWasmJsInterop
@SinceKotlin("2.2")
public expect class JsNumber : JsAny

@ExperimentalWasmJsInterop
@SinceKotlin("2.2")
public expect fun JsNumber.toDouble(): Double

@ExperimentalWasmJsInterop
@SinceKotlin("2.2")
public expect fun Double.toJsNumber(): JsNumber

@ExperimentalWasmJsInterop
@SinceKotlin("2.2")
public expect fun JsNumber.toInt(): Int

@ExperimentalWasmJsInterop
@SinceKotlin("2.2")
public expect fun Int.toJsNumber(): JsNumber