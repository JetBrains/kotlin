/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.InlineOnly
import kotlin.internal.LowPriorityInOverloadResolution

/** JavaScript Array */
public actual typealias JsArray<T> = Array<T>

@InlineOnly
public actual inline fun <T : JsAny?> JsArray(): JsArray<T> =
    Array<JsAny?>(0) { null!! }.unsafeCast<JsArray<T>>()

public actual val JsArray<*>.length: Int
    inline get() = unsafeCast<Array<*>>().size

@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun <T : JsAny?> JsArray<T>.get(index: Int): T? = asDynamic()[index]

@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun <T : JsAny?> JsArray<T>.set(index: Int, value: T) {
    asDynamic()[index] = value
}

@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T : JsAny?> JsArray<T>.toArray(): Array<T> = unsafeCast<Array<T>>()

@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T : JsAny?> Array<T>.toJsArray(): JsArray<T> = unsafeCast<JsArray<T>>()

/** Returns a new [List] containing all the elements of this [JsArray]. */
@LowPriorityInOverloadResolution
@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T : JsAny?> JsArray<T>.toList(): List<T> =
    unsafeCast<Array<T>>().asList()

@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T : JsAny?> List<T>.toJsArray(): JsArray<T> =
    toTypedArray().toJsArray()