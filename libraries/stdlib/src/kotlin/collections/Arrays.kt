/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ArraysKt")


package kotlin.collections

import kotlin.contracts.*


/**
 * Returns a single list of all elements from all arrays in the given array.
 * @sample samples.collections.Arrays.Transformations.flattenArray
 */
public fun <T> Array<out Array<out T>>.flatten(): List<T> {
    val result = ArrayList<T>(sumBy { it.size })
    for (element in this) {
        result.addAll(element)
    }
    return result
}

/**
 * Returns a pair of lists, where
 * *first* list is built from the first values of each pair from this array,
 * *second* list is built from the second values of each pair from this array.
 * @sample samples.collections.Arrays.Transformations.unzipArray
 */
public fun <T, R> Array<out Pair<T, R>>.unzip(): Pair<List<T>, List<R>> {
    val listT = ArrayList<T>(size)
    val listR = ArrayList<R>(size)
    for (pair in this) {
        listT.add(pair.first)
        listR.add(pair.second)
    }
    return listT to listR
}

/**
 * Returns `true` if this nullable array is either null or empty.
 * @sample samples.collections.Arrays.Usage.arrayIsNullOrEmpty
 */
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
public inline fun Array<*>?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }

    return this == null || this.isEmpty()
}

/**
 * Returns this array if it's not empty
 * or the result of calling [defaultValue] function if the array is empty.
 *
 * @sample samples.collections.Arrays.Usage.arrayIfEmpty
 */
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
@Suppress("UPPER_BOUND_CANNOT_BE_ARRAY")
public inline fun <C, R> C.ifEmpty(defaultValue: () -> R): R where C : Array<*>, C : R =
    if (isEmpty()) defaultValue() else this


@SinceKotlin("1.3")
@PublishedApi
@kotlin.jvm.JvmName("contentDeepEquals")
@kotlin.js.JsName("contentDeepEqualsImpl")
internal fun <T> Array<out T>.contentDeepEqualsImpl(other: Array<out T>): Boolean {
    if (this === other) return true
    if (this.size != other.size) return false

    for (i in indices) {
        val v1 = this[i]
        val v2 = other[i]

        if (v1 === v2) {
            continue
        } else if (v1 == null || v2 == null) {
            return false
        }

        when {
            v1 is Array<*>     && v2 is Array<*>     -> if (!v1.contentDeepEquals(v2)) return false
            v1 is ByteArray    && v2 is ByteArray    -> if (!v1.contentEquals(v2)) return false
            v1 is ShortArray   && v2 is ShortArray   -> if (!v1.contentEquals(v2)) return false
            v1 is IntArray     && v2 is IntArray     -> if (!v1.contentEquals(v2)) return false
            v1 is LongArray    && v2 is LongArray    -> if (!v1.contentEquals(v2)) return false
            v1 is FloatArray   && v2 is FloatArray   -> if (!v1.contentEquals(v2)) return false
            v1 is DoubleArray  && v2 is DoubleArray  -> if (!v1.contentEquals(v2)) return false
            v1 is CharArray    && v2 is CharArray    -> if (!v1.contentEquals(v2)) return false
            v1 is BooleanArray && v2 is BooleanArray -> if (!v1.contentEquals(v2)) return false

            v1 is UByteArray   && v2 is UByteArray   -> if (!v1.contentEquals(v2)) return false
            v1 is UShortArray  && v2 is UShortArray  -> if (!v1.contentEquals(v2)) return false
            v1 is UIntArray    && v2 is UIntArray    -> if (!v1.contentEquals(v2)) return false
            v1 is ULongArray   && v2 is ULongArray   -> if (!v1.contentEquals(v2)) return false

            else -> if (v1 != v2) return false
        }

    }
    return true
}

@SinceKotlin("1.3")
@PublishedApi
@kotlin.jvm.JvmName("contentDeepToString")
@kotlin.js.JsName("contentDeepToStringImpl")
internal fun <T> Array<out T>.contentDeepToStringImpl(): String {
    val length = size.coerceAtMost((Int.MAX_VALUE - 2) / 5) * 5 + 2 // in order not to overflow Int.MAX_VALUE
    return buildString(length) {
        contentDeepToStringInternal(this, mutableListOf())
    }
}

@UseExperimental(ExperimentalUnsignedTypes::class)
private fun <T> Array<out T>.contentDeepToStringInternal(result: StringBuilder, processed: MutableList<Array<*>>) {
    if (this in processed) {
        result.append("[...]")
        return
    }
    processed.add(this)
    result.append('[')

    for (i in indices) {
        if (i != 0) {
            result.append(", ")
        }
        val element = this[i]
        when (element) {
            null            -> result.append("null")
            is Array<*>     -> element.contentDeepToStringInternal(result, processed)
            is ByteArray    -> result.append(element.contentToString())
            is ShortArray   -> result.append(element.contentToString())
            is IntArray     -> result.append(element.contentToString())
            is LongArray    -> result.append(element.contentToString())
            is FloatArray   -> result.append(element.contentToString())
            is DoubleArray  -> result.append(element.contentToString())
            is CharArray    -> result.append(element.contentToString())
            is BooleanArray -> result.append(element.contentToString())

            is UByteArray   -> result.append(element.contentToString())
            is UShortArray  -> result.append(element.contentToString())
            is UIntArray    -> result.append(element.contentToString())
            is ULongArray   -> result.append(element.contentToString())

            else            -> result.append(element.toString())
        }
    }

    result.append(']')
    processed.removeAt(processed.lastIndex)
}