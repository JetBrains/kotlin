/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/** Concat regular Array's and TypedArray's into an Array.
 */
@PublishedApi
internal fun <T> arrayConcat(vararg args: T): T {
    val len = args.size
    val typed = js("Array(len)").unsafeCast<Array<T>>()
    for (i in 0 .. (len - 1)) {
        val arr = args[i]
        if (arr !is Array<*>) {
            typed[i] = js("[]").slice.call(arr)
        } else {
            typed[i] = arr
        }
    }
    return js("[]").concat.apply(js("[]"), typed);
}

/** Concat primitive arrays. Main use: prepare vararg arguments.
 */
@PublishedApi
internal fun <T> primitiveArrayConcat(vararg args: T): T {
    var size = 0
    for (i in 0 .. (args.size - 1)) {
        size += args[i].unsafeCast<Array<Any?>>().size
    }
    val a = args[0]
    val result = js("new a.constructor(size)").unsafeCast<Array<Any?>>()
    if (a.asDynamic().`$type$` != null) {
        withType(a.asDynamic().`$type$`, result)
    }

    size = 0
    for (i in 0 .. (args.size - 1)) {
        val arr = args[i].unsafeCast<Array<Any?>>()
        for (j in 0 .. (arr.size - 1)) {
            result[size++] = arr[j]
        }
    }
    return result.unsafeCast<T>()
}

@PublishedApi
internal inline fun withType(type: String, array: dynamic): dynamic {
    array.`$type$` = type
    return array
}
