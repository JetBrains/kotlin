/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.LowPriorityInOverloadResolution

/** JavaScript Array */
@ExperimentalWasmJsInterop
@JsName("Array")
public actual external class JsArray<T : JsAny?> : JsAny {
    public val length: Int
}

@ExperimentalWasmJsInterop
@LowPriorityInOverloadResolution
public actual fun <T : JsAny?> JsArray(): JsArray<T> =
    JsArray()

@ExperimentalWasmJsInterop
public actual val JsArray<*>.length: Int
    inline get() = length

@ExperimentalWasmJsInterop
public actual operator fun <T : JsAny?> JsArray<T>.get(index: Int): T? =
    jsArrayGet(this, index)

@ExperimentalWasmJsInterop
public actual operator fun <T : JsAny?> JsArray<T>.set(index: Int, value: T) {
    jsArraySet(this, index, value)
}

@Suppress("RedundantNullableReturnType", "UNUSED_PARAMETER")
@OptIn(ExperimentalWasmJsInterop::class)
private fun <T : JsAny?> jsArrayGet(array: JsArray<T>, index: Int): T? =
    js("array[index]")

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalWasmJsInterop::class)
private fun <T : JsAny?> jsArraySet(array: JsArray<T>, index: Int, value: T) {
    js("array[index] = value")
}

/** Returns a new [Array] containing all the elements of this [JsArray]. */
@ExperimentalWasmJsInterop
public actual fun <T : JsAny?> JsArray<T>.toArray(): Array<T> {
    @Suppress("UNCHECKED_CAST", "TYPE_PARAMETER_AS_REIFIED")
    return Array(this.length) { this[it] as T }
}

/** Returns a new [JsArray] containing all the elements of this [Array]. */
@ExperimentalWasmJsInterop
public actual fun <T : JsAny?> Array<T>.toJsArray(): JsArray<T> {
    val destination = JsArray<T>()
    for (i in this.indices) {
        destination[i] = this[i]
    }
    return destination
}

/** Returns a new [List] containing all the elements of this [JsArray]. */
@ExperimentalWasmJsInterop
public actual fun <T : JsAny?> JsArray<T>.toList(): List<T> {
    @Suppress("UNCHECKED_CAST")
    return List(length) { this[it] as T }
}

/** Returns a new [JsArray] containing all the elements of this [List]. */
@ExperimentalWasmJsInterop
public actual fun <T : JsAny?> List<T>.toJsArray(): JsArray<T> {
    val destination = JsArray<T>()
    for (i in this.indices) {
        destination[i] = this[i]
    }
    return destination
}
