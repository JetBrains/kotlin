/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

// a package is omitted to get declarations directly under the module

@PublishedApi
external internal fun <T> Array(size: Int): Array<T>

@JsName("newArray")
fun <T> newArray(size: Int, initValue: T) = fillArrayVal(Array<T>(size), initValue)

@JsName("newArrayF")
inline fun <T> arrayWithFun(size: Int, init: (Int) -> T) = fillArrayFun(Array<T>(size), init)

@JsName("fillArray")
inline fun <T> fillArrayFun(array: Array<T>, init: (Int) -> T): Array<T> {
    for (i in 0..array.size - 1) {
        array[i] = init(i)
    }
    return array
}

@JsName("booleanArray")
fun booleanArray(size: Int, init: dynamic): Array<Boolean> {
    val result: dynamic = Array<Boolean>(size)
    result.`$type$` = "BooleanArray"
    return when (init) {
        null, true -> fillArrayVal(result, false)
        false -> result
        else -> fillArrayFun<Boolean>(result, init)
    }
}

@JsName("booleanArrayF")
inline fun booleanArrayWithFun(size: Int, init: (Int) -> Boolean): Array<Boolean> = fillArrayFun(booleanArray(size, false), init)

@JsName("charArray")
@Suppress("UNUSED_PARAMETER")
fun charArray(size: Int, init: dynamic): Array<Char> {
    val result = js("new Uint16Array(size)")
    result.`$type$` = "CharArray"
    return when (init) {
        null, true, false -> result // For consistency
        else -> fillArrayFun<Char>(result, init)
    }
}

@JsName("charArrayF")
inline fun charArrayWithFun(size: Int, init: (Int) -> Char): Array<Char> {
    val array = charArray(size, null)
    for (i in 0..array.size - 1) {
        @Suppress("UNUSED_VARIABLE") // used in js block
        val value = init(i)
        js("array[i] = value;")
    }
    return array
}

@JsName("untypedCharArrayF")
inline fun untypedCharArrayWithFun(size: Int, init: (Int) -> Char): Array<Char> {
    val array = Array<Char>(size)
    for (i in 0..array.size - 1) {
        @Suppress("UNUSED_VARIABLE") // used in js block
        val value = init(i)
        js("array[i] = value;")
    }
    return array
}

@JsName("longArray")
fun longArray(size: Int, init: dynamic): Array<Long> {
    val result: dynamic = Array<Long>(size)
    result.`$type$` = "LongArray"
    return when (init) {
        null, true -> fillArrayVal(result, 0L)
        false -> result
        else -> fillArrayFun<Long>(result, init)
    }
}

@JsName("longArrayF")
inline fun longArrayWithFun(size: Int, init: (Int) -> Long): Array<Long> = fillArrayFun(longArray(size, false), init)

private fun <T> fillArrayVal(array: Array<T>, initValue: T): Array<T> {
    for (i in 0..array.size - 1) {
        array[i] = initValue
    }
    return array
}