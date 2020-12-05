/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

import kotlin.internal.PureReifiable

// Array Utils copied from K/N

internal fun checkCopyOfRangeArguments(fromIndex: Int, toIndex: Int, size: Int) {
    if (toIndex > size)
        throw IndexOutOfBoundsException("toIndex ($toIndex) is greater than size ($size).")
    if (fromIndex > toIndex)
        throw IllegalArgumentException("fromIndex ($fromIndex) is greater than toIndex ($toIndex).")
}


// TODO: internal
/**
 * Returns a string representation of the contents of the subarray of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <T> Array<out T>.subarrayContentToString(offset: Int, length: Int): String {
    val sb = StringBuilder(2 + length * 3)
    sb.append("[")
    var i = 0
    while (i < length) {
        if (i > 0) sb.append(", ")
        sb.append(this[offset + i])
        i++
    }
    sb.append("]")
    return sb.toString()
}


/**
 * Returns a hash code based on the contents of this array as if it is [List].
 * Nested arrays are treated as lists too.
 *
 * If any of arrays contains itself on any nesting level the behavior is undefined.
 */
@SinceKotlin("1.1")
@UseExperimental(ExperimentalUnsignedTypes::class)
internal fun <T> Array<out T>?.contentDeepHashCodeImpl(): Int {
    if (this == null) return 0
    var result = 1
    for (element in this) {
        val elementHash = when (element) {
            null            -> 0

            is Array<*>     -> element.contentDeepHashCode()

            is ByteArray    -> element.contentHashCode()
            is ShortArray   -> element.contentHashCode()
            is IntArray     -> element.contentHashCode()
            is LongArray    -> element.contentHashCode()
            is FloatArray   -> element.contentHashCode()
            is DoubleArray  -> element.contentHashCode()
            is CharArray    -> element.contentHashCode()
            is BooleanArray -> element.contentHashCode()

            is UByteArray   -> element.contentHashCode()
            is UShortArray  -> element.contentHashCode()
            is UIntArray    -> element.contentHashCode()
            is ULongArray   -> element.contentHashCode()

            else            -> element.hashCode()
        }

        result = 31 * result + elementHash
    }
    return result
}

@Suppress("UNCHECKED_CAST")
internal actual fun <T> arrayOfNulls(reference: Array<T>, size: Int): Array<T> = arrayOfNulls<Any>(size) as Array<T>

internal actual fun copyToArrayImpl(collection: Collection<*>): Array<Any?> {
    val array = Array<Any?>(collection.size)
    val iterator = collection.iterator()
    var index = 0
    while (iterator.hasNext())
        array[index++] = iterator.next()
    return array
}

@Suppress("UNCHECKED_CAST")
internal actual fun <T> copyToArrayImpl(collection: Collection<*>, array: Array<T>): Array<T> {
    if (array.size < collection.size)
        return copyToArrayImpl(collection) as Array<T>

    val iterator = collection.iterator()
    var index = 0
    while (iterator.hasNext()) {
        array[index++] = iterator.next() as T
    }
    if (index < array.size) {
        return array.copyOf(index) as Array<T>
    }
    return array
}
