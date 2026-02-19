/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.InlineOnly
import kotlin.internal.LowPriorityInOverloadResolution

/** JavaScript Array */
@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
public actual typealias JsArray<T> = Array<T>

@InlineOnly
@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
public actual inline fun <T : JsAny?> JsArray(): JsArray<T> =
    Array<JsAny?>(0) { null!! }.unsafeCast<JsArray<T>>()

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
public actual val JsArray<*>.length: Int
    inline get() = unsafeCast<Array<*>>().size

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun <T : JsAny?> JsArray<T>.get(index: Int): T? = asDynamic()[index]

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun <T : JsAny?> JsArray<T>.set(index: Int, value: T) {
    asDynamic()[index] = value
}

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T : JsAny?> JsArray<T>.toArray(): Array<T> = unsafeCast<Array<T>>()

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T : JsAny?> Array<T>.toJsArray(): JsArray<T> = unsafeCast<JsArray<T>>()

/** Returns a new [List] containing all the elements of this [JsArray]. */
@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
@LowPriorityInOverloadResolution
@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T : JsAny?> JsArray<T>.toList(): List<T> =
    unsafeCast<Array<T>>().asList()

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T : JsAny?> List<T>.toJsArray(): JsArray<T> =
    toTypedArray().toJsArray()