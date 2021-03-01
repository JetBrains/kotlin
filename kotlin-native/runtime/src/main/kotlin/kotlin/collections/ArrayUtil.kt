/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

import kotlin.native.internal.PointsTo
import kotlin.native.internal.ExportForCppRuntime

/**
 * Returns an array of objects of the given type with the given [size], initialized with _uninitialized_ values.
 * Attempts to read _uninitialized_ values from this array work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
@PublishedApi
internal inline fun <E> arrayOfUninitializedElements(size: Int): Array<E> {
    // TODO: special case for size == 0?
    require(size >= 0) { "capacity must be non-negative." }
    @Suppress("TYPE_PARAMETER_AS_REIFIED")
    return Array<E>(size)
}


/**
 * Returns a new array which is a copy of the original array with new elements filled with null values.
 */
internal fun <E> Array<E>.copyOfNulls(newSize: Int): Array<E?>  = copyOfNulls(0, newSize)

internal fun <E> Array<E>.copyOfNulls(fromIndex: Int, toIndex: Int): Array<E?> {
    val newSize = toIndex - fromIndex
    if (newSize < 0) {
        throw IllegalArgumentException("$fromIndex > $toIndex")
    }
    val result = @Suppress("TYPE_PARAMETER_AS_REIFIED") arrayOfNulls<E>(newSize)
    this.copyInto(result, 0, fromIndex, toIndex.coerceAtMost(size))
    return result
}

/**
 * Copies elements of the [collection] into the given [array].
 * If the array is too small, allocates a new one of collection.size size.
 * @return [array] with the elements copied from the collection.
 */
internal fun <E, T> collectionToArray(collection: Collection<E>, array: Array<T>): Array<T> {
    val toArray = if (collection.size > array.size) {
        arrayOfUninitializedElements<T>(collection.size)
    } else {
        array
    }
    var i = 0
    for (v in collection) {
        @Suppress("UNCHECKED_CAST")
        toArray[i] = v as T
        i++
    }
    return toArray
}

/**
 * Creates an array of collection.size size and copies elements of the [collection] into it.
 * @return [array] with the elements copied from the collection.
 */
internal fun <E> collectionToArray(collection: Collection<E>): Array<E>
        = collectionToArray(collection, arrayOfUninitializedElements(collection.size))


/**
 * Resets an array element at a specified index to some implementation-specific _uninitialized_ value.
 * In particular, references stored in this element are released and become available for garbage collection.
 * Attempts to read _uninitialized_ value work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
internal fun <E> Array<E>.resetAt(index: Int) {
    (@Suppress("UNCHECKED_CAST")(this as Array<Any?>))[index] = null
}

@SymbolName("Kotlin_Array_fillImpl")
@PointsTo(0x3000, 0x0000, 0x0000, 0x0000) // array.intestines -> value
internal external fun <T> arrayFill(array: Array<T>, fromIndex: Int, toIndex: Int, value: T)

@SymbolName("Kotlin_ByteArray_fillImpl")
internal external fun arrayFill(array: ByteArray, fromIndex: Int, toIndex: Int, value: Byte)

@SymbolName("Kotlin_ShortArray_fillImpl")
internal external fun arrayFill(array: ShortArray, fromIndex: Int, toIndex: Int, value: Short)

@SymbolName("Kotlin_CharArray_fillImpl")
internal external fun arrayFill(array: CharArray, fromIndex: Int, toIndex: Int, value: Char)

@SymbolName("Kotlin_IntArray_fillImpl")
internal external fun arrayFill(array: IntArray, fromIndex: Int, toIndex: Int, value: Int)

@SymbolName("Kotlin_LongArray_fillImpl")
internal external fun arrayFill(array: LongArray, fromIndex: Int, toIndex: Int, value: Long)

@SymbolName("Kotlin_DoubleArray_fillImpl")
internal external fun arrayFill(array: DoubleArray, fromIndex: Int, toIndex: Int, value: Double)

@SymbolName("Kotlin_FloatArray_fillImpl")
internal external fun arrayFill(array: FloatArray, fromIndex: Int, toIndex: Int, value: Float)

@SymbolName("Kotlin_BooleanArray_fillImpl")
internal external fun arrayFill(array: BooleanArray, fromIndex: Int, toIndex: Int, value: Boolean)

@ExportForCppRuntime
internal fun checkRangeIndexes(fromIndex: Int, toIndex: Int, size: Int) {
    if (fromIndex < 0 || toIndex > size) {
        throw IndexOutOfBoundsException("fromIndex: $fromIndex, toIndex: $toIndex, size: $size")
    }
    if (fromIndex > toIndex) {
        throw IllegalArgumentException("fromIndex: $fromIndex > toIndex: $toIndex")
    }
}

/**
 * Resets a range of array elements at a specified [fromIndex] (inclusive) to [toIndex] (exclusive) range of indices
 * to some implementation-specific _uninitialized_ value.
 * In particular, references stored in these elements are released and become available for garbage collection.
 * Attempts to read _uninitialized_ values work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
internal fun <E> Array<E>.resetRange(fromIndex: Int, toIndex: Int) {
    arrayFill(@Suppress("UNCHECKED_CAST") (this as Array<Any?>), fromIndex, toIndex, null)
}

@SymbolName("Kotlin_Array_copyImpl")
@PointsTo(0x00000, 0x00000, 0x00004, 0x00000, 0x00000) // destination.intestines -> array.intestines
internal external fun arrayCopy(array: Array<Any?>, fromIndex: Int, destination: Array<Any?>, toIndex: Int, count: Int)

@SymbolName("Kotlin_ByteArray_copyImpl")
internal external fun arrayCopy(array: ByteArray, fromIndex: Int, destination: ByteArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_ShortArray_copyImpl")
internal external fun arrayCopy(array: ShortArray, fromIndex: Int, destination: ShortArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_CharArray_copyImpl")
internal external fun arrayCopy(array: CharArray, fromIndex: Int, destination: CharArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_IntArray_copyImpl")
internal external fun arrayCopy(array: IntArray, fromIndex: Int, destination: IntArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_LongArray_copyImpl")
internal external fun arrayCopy(array: LongArray, fromIndex: Int, destination: LongArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_FloatArray_copyImpl")
internal external fun arrayCopy(array: FloatArray, fromIndex: Int, destination: FloatArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_DoubleArray_copyImpl")
internal external fun arrayCopy(array: DoubleArray, fromIndex: Int, destination: DoubleArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_BooleanArray_copyImpl")
internal external fun arrayCopy(array: BooleanArray, fromIndex: Int, destination: BooleanArray, toIndex: Int, count: Int)


internal fun <E> Collection<E>.collectionToString(): String {
    val sb = StringBuilder(2 + size * 3)
    sb.append("[")
    var i = 0
    val it = iterator()
    while (it.hasNext()) {
        if (i > 0) sb.append(", ")
        val next = it.next()
        if (next == this) sb.append("(this Collection)") else sb.append(next)
        i++
    }
    sb.append("]")
    return sb.toString()
}
