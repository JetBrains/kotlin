/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/** JavaScript Array */
@JsName("Array")
public external class JsArray<T : JsAny?> : JsAny {
    public val length: Int
}

@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T : JsAny?> JsArray<T>.get(index: Int): T? = asDynamic()[index]

@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T : JsAny?> JsArray<T>.set(index: Int, value: T) {
    asDynamic()[index] = value
}

@Suppress("NOTHING_TO_INLINE")
public inline fun <T : JsAny?> JsArray<T>.toArray(): Array<T> = unsafeCast<Array<T>>()

@Suppress("NOTHING_TO_INLINE")
public inline fun <T : JsAny?> Array<T>.toJsArray(): JsArray<T> = unsafeCast<JsArray<T>>()

@Suppress("NOTHING_TO_INLINE")
/** Returns a new [List] containing all the elements of this [JsArray]. */
public inline fun <T : JsAny?> JsArray<T>.toList(): List<T> =
    unsafeCast<Array<T>>().asList()

@Suppress("NOTHING_TO_INLINE")
public inline fun <T : JsAny?> List<T>.toJsArray(): JsArray<T> =
    toTypedArray().toJsArray()