/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin.collections

/**
 * Returns an array of objects of the given type with the given [size], initialized with _uninitialized_ values.
 * Attempts to read _uninitialized_ values from this array work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
internal fun <E> arrayOfUninitializedElements(size: Int): Array<E> {
    // TODO: special case for size == 0?
    return Array<E>(size)
}

/**
 * Returns new array which is a copy of the original array with new elements filled with **lateinit** _uninitialized_ values.
 * Attempts to read _uninitialized_ values from this array work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
fun <E> Array<E>.copyOfUninitializedElements(newSize: Int): Array<E>     = copyOfUninitializedElements(0, newSize)
fun ByteArray.copyOfUninitializedElements(newSize: Int): ByteArray       = copyOfUninitializedElements(0, newSize)
fun ShortArray.copyOfUninitializedElements(newSize: Int): ShortArray     = copyOfUninitializedElements(0, newSize)
fun IntArray.copyOfUninitializedElements(newSize: Int): IntArray         = copyOfUninitializedElements(0, newSize)
fun LongArray.copyOfUninitializedElements(newSize: Int): LongArray       = copyOfUninitializedElements(0, newSize)
fun CharArray.copyOfUninitializedElements(newSize: Int): CharArray       = copyOfUninitializedElements(0, newSize)
fun FloatArray.copyOfUninitializedElements(newSize: Int): FloatArray     = copyOfUninitializedElements(0, newSize)
fun DoubleArray.copyOfUninitializedElements(newSize: Int): DoubleArray   = copyOfUninitializedElements(0, newSize)
fun BooleanArray.copyOfUninitializedElements(newSize: Int): BooleanArray = copyOfUninitializedElements(0, newSize)

/**
 * Returns a new array which is a copy of the original array with new elements filled with null values.
 */
fun <E> Array<E>.copyOfNulls(newSize: Int): Array<E?>  = copyOfNulls(0, newSize)
fun <E> Array<E>.copyOfNulls(fromIndex: Int, toIndex: Int): Array<E?> {
    val newSize = toIndex - fromIndex
    if (newSize < 0) {
        throw IllegalArgumentException("$fromIndex > $toIndex")
    }
    val result = arrayOfNulls<E>(newSize)
    copyRangeTo(result, fromIndex, if (toIndex > size) size else toIndex, 0)
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
    // TODO: What about a concurrent modification of the collection? Do we need to handle it here?
    for (v in collection) {
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
 * Returns new array which is a copy of the original array's range between [fromIndex] (inclusive)
 * and [toIndex] (exclusive) with new elements filled with **lateinit** _uninitialized_ values.
 * Attempts to read _uninitialized_ values from this array work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
fun <E> Array<E>.copyOfUninitializedElements(fromIndex: Int, toIndex: Int): Array<E> {
    val newSize = toIndex - fromIndex
    if (newSize < 0) {
        throw IllegalArgumentException("$fromIndex > $toIndex")
    }
    val result = arrayOfUninitializedElements<E>(newSize)
    copyRangeTo(result, fromIndex, if (toIndex > size) size else toIndex, 0)
    return result
}

fun ByteArray.copyOfUninitializedElements(fromIndex: Int, toIndex: Int): ByteArray {
    val newSize = toIndex - fromIndex
    if (newSize < 0) {
        throw IllegalArgumentException("$fromIndex > $toIndex")
    }
    val result = ByteArray(newSize)
    copyRangeTo(result, fromIndex, if (toIndex > size) size else toIndex, 0)
    return result
}

fun ShortArray.copyOfUninitializedElements(fromIndex: Int, toIndex: Int): ShortArray {
    val newSize = toIndex - fromIndex
    if (newSize < 0) {
        throw IllegalArgumentException("$fromIndex > $toIndex")
    }
    val result = ShortArray(newSize)
    copyRangeTo(result, fromIndex, if (toIndex > size) size else toIndex, 0)
    return result
}

fun IntArray.copyOfUninitializedElements(fromIndex: Int, toIndex: Int): IntArray {
    val newSize = toIndex - fromIndex
    if (newSize < 0) {
        throw IllegalArgumentException("$fromIndex > $toIndex")
    }
    val result = IntArray(newSize)
    copyRangeTo(result, fromIndex, if (toIndex > size) size else toIndex, 0)
    return result
}

fun LongArray.copyOfUninitializedElements(fromIndex: Int, toIndex: Int): LongArray {
    val newSize = toIndex - fromIndex
    if (newSize < 0) {
        throw IllegalArgumentException("$fromIndex > $toIndex")
    }
    val result = LongArray(newSize)
    copyRangeTo(result, fromIndex, if (toIndex > size) size else toIndex, 0)
    return result
}

fun CharArray.copyOfUninitializedElements(fromIndex: Int, toIndex: Int): CharArray {
    val newSize = toIndex - fromIndex
    if (newSize < 0) {
        throw IllegalArgumentException("$fromIndex > $toIndex")
    }
    val result = CharArray(newSize)
    copyRangeTo(result, fromIndex, if (toIndex > size) size else toIndex, 0)
    return result
}

fun FloatArray.copyOfUninitializedElements(fromIndex: Int, toIndex: Int): FloatArray {
    val newSize = toIndex - fromIndex
    if (newSize < 0) {
        throw IllegalArgumentException("$fromIndex > $toIndex")
    }
    val result = FloatArray(newSize)
    copyRangeTo(result, fromIndex, if (toIndex > size) size else toIndex, 0)
    return result
}

fun DoubleArray.copyOfUninitializedElements(fromIndex: Int, toIndex: Int): DoubleArray {
    val newSize = toIndex - fromIndex
    if (newSize < 0) {
        throw IllegalArgumentException("$fromIndex > $toIndex")
    }
    val result = DoubleArray(newSize)
    copyRangeTo(result, fromIndex, if (toIndex > size) size else toIndex, 0)
    return result
}

fun BooleanArray.copyOfUninitializedElements(fromIndex: Int, toIndex: Int): BooleanArray {
    val newSize = toIndex - fromIndex
    if (newSize < 0) {
        throw IllegalArgumentException("$fromIndex > $toIndex")
    }
    val result = BooleanArray(newSize)
    copyRangeTo(result, fromIndex, if (toIndex > size) size else toIndex, 0)
    return result
}


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
external private fun fillImpl(array: Array<Any>, fromIndex: Int, toIndex: Int, value: Any?)

@SymbolName("Kotlin_IntArray_fillImpl")
external private fun fillImpl(array: IntArray, fromIndex: Int, toIndex: Int, value: Int)

/**
 * Resets a range of array elements at a specified [fromIndex] (inclusive) to [toIndex] (exclusive) range of indices
 * to some implementation-specific _uninitialized_ value.
 * In particular, references stored in these elements are released and become available for garbage collection.
 * Attempts to read _uninitialized_ values work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
internal fun <E> Array<E>.resetRange(fromIndex: Int, toIndex: Int) {
    fillImpl(@Suppress("UNCHECKED_CAST") (this as Array<Any>), fromIndex, toIndex, null)
}

internal fun IntArray.fill(fromIndex: Int, toIndex: Int, value: Int) {
    fillImpl(this, fromIndex, toIndex, value)
}

@SymbolName("Kotlin_Array_copyImpl")
external private fun copyImpl(array: Array<Any>, fromIndex: Int,
                         destination: Array<Any>, toIndex: Int, count: Int)

@SymbolName("Kotlin_ByteArray_copyImpl")
external private fun copyImpl(array: ByteArray, fromIndex: Int,
                              destination: ByteArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_ShortArray_copyImpl")
external private fun copyImpl(array: ShortArray, fromIndex: Int,
                              destination: ShortArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_CharArray_copyImpl")
external private fun copyImpl(array: CharArray, fromIndex: Int,
                              destination: CharArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_IntArray_copyImpl")
external private fun copyImpl(array: IntArray, fromIndex: Int,
                              destination: IntArray, toIndex: Int, count: Int)
@SymbolName("Kotlin_LongArray_copyImpl")
external private fun copyImpl(array: LongArray, fromIndex: Int,
                              destination: LongArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_FloatArray_copyImpl")
external private fun copyImpl(array: FloatArray, fromIndex: Int,
                              destination: FloatArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_DoubleArray_copyImpl")
external private fun copyImpl(array: DoubleArray, fromIndex: Int,
                              destination: DoubleArray, toIndex: Int, count: Int)

@SymbolName("Kotlin_BooleanArray_copyImpl")
external private fun copyImpl(array: BooleanArray, fromIndex: Int,
                              destination: BooleanArray, toIndex: Int, count: Int)

/**
 * Copies a range of array elements at a specified [fromIndex] (inclusive) to [toIndex] (exclusive) range of indices
 * to another [destination] array starting at [destinationIndex].
 */
fun <E> Array<out E>.copyRangeTo(destination: Array<in E>, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(@Suppress("UNCHECKED_CAST") (this as Array<Any>), fromIndex,
             @Suppress("UNCHECKED_CAST") (destination as Array<Any>),
             destinationIndex, toIndex - fromIndex)
}

fun ByteArray.copyRangeTo(destination: ByteArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

fun ShortArray.copyRangeTo(destination: ShortArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

fun CharArray.copyRangeTo(destination: CharArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

fun IntArray.copyRangeTo(destination: IntArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

fun LongArray.copyRangeTo(destination: LongArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

fun FloatArray.copyRangeTo(destination: FloatArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

fun DoubleArray.copyRangeTo(destination: DoubleArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

fun BooleanArray.copyRangeTo(destination: BooleanArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}
/**
 * Copies a range of array elements at a specified [fromIndex] (inclusive) to [toIndex] (exclusive) range of indices
 * to another part of this array starting at [destinationIndex].
 */
fun <E> Array<E>.copyRange(fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyRangeTo(this, fromIndex, toIndex, destinationIndex)
}

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