/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/** JavaScript Array for WasmJs Interop */
@ExperimentalWasmJsInterop
public expect class JsArray<T : JsAny?> : JsAny

@ExperimentalWasmJsInterop
public expect fun <T: JsAny?> JsArray(): JsArray<T>

@ExperimentalWasmJsInterop
public expect val JsArray<*>.length: Int

@ExperimentalWasmJsInterop
public expect operator fun <T : JsAny?> JsArray<T>.get(index: Int): T?

@ExperimentalWasmJsInterop
public expect operator fun <T : JsAny?> JsArray<T>.set(index: Int, value: T)

/** Returns a new [Array] containing all the elements of this [JsArray]. */
@ExperimentalWasmJsInterop
public expect fun <T : JsAny?> JsArray<T>.toArray(): Array<T>

/** Returns a new [JsArray] containing all the elements of this [Array]. */
@ExperimentalWasmJsInterop
public expect fun <T : JsAny?> Array<T>.toJsArray(): JsArray<T>

/** Returns a new [List] containing all the elements of this [JsArray]. */
@ExperimentalWasmJsInterop
public expect fun <T : JsAny?> JsArray<T>.toList(): List<T>

/** Returns a new [JsArray] containing all the elements of this [List]. */
@ExperimentalWasmJsInterop
public expect fun <T : JsAny?> List<T>.toJsArray(): JsArray<T>