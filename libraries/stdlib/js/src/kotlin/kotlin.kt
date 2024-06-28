/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER", "NOTHING_TO_INLINE")

package kotlin

/**
 * Returns an empty array of the specified type [T].
 */
public inline fun <T> emptyArray(): Array<T> = js("[]")

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 */
public actual fun <T> lazy(initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [mode] parameter is ignored. */
public actual fun <T> lazy(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [lock] parameter is ignored.
 */
@Deprecated("Synchronization on Any? object is not supported.", ReplaceWith("lazy(initializer)"))
@DeprecatedSinceKotlin(warningSince = "1.9")
public actual fun <T> lazy(lock: Any?, initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)


internal fun fillFrom(src: dynamic, dst: dynamic): dynamic {
    val srcLen: Int = src.length
    val dstLen: Int = dst.length
    var index: Int = 0
    val arr = dst.unsafeCast<Array<Any?>>()
    while (index < srcLen && index < dstLen) arr[index] = src[index++]
    return dst
}


internal fun arrayCopyResize(source: dynamic, newSize: Int, defaultValue: Any?): dynamic {
    val result = source.slice(0, newSize).unsafeCast<Array<Any?>>()
    copyArrayType(source, result)
    var index: Int = source.length
    if (newSize > index) {
        result.asDynamic().length = newSize
        while (index < newSize) result[index++] = defaultValue
    }
    return result
}

internal fun <T> arrayPlusCollection(array: dynamic, collection: Collection<T>): dynamic {
    val result = array.slice().unsafeCast<Array<T>>()
    result.asDynamic().length = result.size + collection.size
    copyArrayType(array, result)
    var index: Int = array.length
    for (element in collection) result[index++] = element
    return result
}

internal inline fun copyArrayType(from: dynamic, to: dynamic) {
    if (from.`$type$` !== undefined) {
        to.`$type$` = from.`$type$`
    }
}
