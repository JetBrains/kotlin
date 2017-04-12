/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("UNUSED_PARAMETER", "NOTHING_TO_INLINE")

package kotlin

/**
 * Returns an empty array of the specified type [T].
 */
public inline fun <T> emptyArray(): Array<T> = js("[]")

@library
public fun <T> arrayOf(vararg elements: T): Array<T> = definedExternally

@library
public fun doubleArrayOf(vararg elements: Double): DoubleArray = definedExternally

@library
public fun floatArrayOf(vararg elements: Float): FloatArray = definedExternally

@library
public fun longArrayOf(vararg elements: Long): LongArray = definedExternally

@library
public fun intArrayOf(vararg elements: Int): IntArray = definedExternally

@library
public fun charArrayOf(vararg elements: Char): CharArray = definedExternally

@library
public fun shortArrayOf(vararg elements: Short): ShortArray = definedExternally

@library
public fun byteArrayOf(vararg elements: Byte): ByteArray = definedExternally

@library
public fun booleanArrayOf(vararg elements: Boolean): BooleanArray = definedExternally

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 */
public fun <T> lazy(initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [mode] parameter is ignored. */
public fun <T> lazy(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [lock] parameter is ignored.
 */
public fun <T> lazy(lock: Any?, initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)


internal fun <T> arrayOfNulls(reference: Array<out T>, size: Int): Array<T> {
    return arrayOfNulls<Any>(size).unsafeCast<Array<T>>()
}

internal fun fillFrom(src: dynamic, dst: dynamic): dynamic {
    val srcLen: Int = src.length
    val dstLen: Int = dst.length
    var index: Int = 0
    while (index < srcLen && index < dstLen) dst[index] = src[index++]
    return dst
}


internal fun arrayCopyResize(source: dynamic, newSize: Int, defaultValue: Any?): dynamic {
    val result = source.slice(0, newSize)
    copyArrayType(source, result)
    var index: Int = source.length
    if (newSize > index) {
        result.length = newSize
        while (index < newSize) result[index++] = defaultValue
    }
    return result
}

internal fun <T> arrayPlusCollection(array: dynamic, collection: Collection<T>): dynamic {
    val result = array.slice()
    result.length += collection.size
    copyArrayType(array, result)
    var index: Int = array.length
    for (element in collection) result[index++] = element
    return result
}

internal fun <T> fillFromCollection(dst: dynamic, startIndex: Int, collection: Collection<T>): dynamic {
    var index = startIndex
    for (element in collection) dst[index++] = element
    return dst
}

internal inline fun copyArrayType(from: dynamic, to: dynamic) {
    if (from.`$type$` !== undefined) {
        to.`$type$` = from.`$type$`
    }
}

// no singleton map implementation in js, return map as is
internal inline fun <K, V> Map<K, V>.toSingletonMapOrSelf(): Map<K, V> = this

internal inline fun <K, V> Map<out K, V>.toSingletonMap(): Map<K, V> = this.toMutableMap()

internal inline fun <T> Array<out T>.copyToArrayOfAny(isVarargs: Boolean): Array<out Any?> =
        if (isVarargs)
            // no need to copy vararg array in JS
            this
        else
            this.copyOf()

// temporary for shared code, until we have an annotation like JvmSerializable
internal interface Serializable