/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

import konan.internal.InlineConstructor
import kotlin.collections.*
import kotlin.internal.PureReifiable
import kotlin.util.sortArrayComparable
import kotlin.util.sortArrayWith
import kotlin.util.sortArray

/** Returns the array if it's not `null`, or an empty array otherwise. */
public actual inline fun <reified T> Array<out T>?.orEmpty(): Array<out T> = this ?: emptyArray<T>()

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun <T> Array<T>.copyOf(): Array<T> {
    return this.copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun ByteArray.copyOf(): ByteArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun ShortArray.copyOf(): ShortArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun IntArray.copyOf(): IntArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun LongArray.copyOf(): LongArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun FloatArray.copyOf(): FloatArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun DoubleArray.copyOf(): DoubleArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun BooleanArray.copyOf(): BooleanArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun CharArray.copyOf(): CharArray {
    return copyOfUninitializedElements(size)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public actual inline fun <T> Array<T>.copyOf(newSize: Int): Array<T?> {
    return copyOfNulls(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public actual inline fun ByteArray.copyOf(newSize: Int): ByteArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public actual inline fun ShortArray.copyOf(newSize: Int): ShortArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public actual inline fun IntArray.copyOf(newSize: Int): IntArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public actual inline fun LongArray.copyOf(newSize: Int): LongArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public actual inline fun FloatArray.copyOf(newSize: Int): FloatArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public actual inline fun DoubleArray.copyOf(newSize: Int): DoubleArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public actual inline fun BooleanArray.copyOf(newSize: Int): BooleanArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of the original array, resized to the given [newSize].
 */
@kotlin.internal.InlineOnly
public actual inline fun CharArray.copyOf(newSize: Int): CharArray {
    return copyOfUninitializedElements(newSize)
}

/**
 * Returns new array which is a copy of range of original array.
 */
// TODO: The method may check input or return Array<T?>.
// Now we check its input (fromIndex <= toIndex < size).
// Sync its behaviour wiht Kotlin JVM when the problem is solved there.
@kotlin.internal.InlineOnly
public actual inline fun <T> Array<T>.copyOfRange(fromIndex: Int, toIndex: Int): Array<T> {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun ByteArray.copyOfRange(fromIndex: Int, toIndex: Int): ByteArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun ShortArray.copyOfRange(fromIndex: Int, toIndex: Int): ShortArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun IntArray.copyOfRange(fromIndex: Int, toIndex: Int): IntArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun LongArray.copyOfRange(fromIndex: Int, toIndex: Int): LongArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun FloatArray.copyOfRange(fromIndex: Int, toIndex: Int): FloatArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun DoubleArray.copyOfRange(fromIndex: Int, toIndex: Int): DoubleArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun BooleanArray.copyOfRange(fromIndex: Int, toIndex: Int): BooleanArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Returns new array which is a copy of range of original array.
 */
@kotlin.internal.InlineOnly
public actual inline fun CharArray.copyOfRange(fromIndex: Int, toIndex: Int): CharArray {
    if (fromIndex > toIndex || toIndex > size)
        throw IllegalArgumentException("Wrong indices: fromIndex: $fromIndex, toIndex: $toIndex, array size: $size")
    return copyOfUninitializedElements(fromIndex, toIndex)
}

/**
 * Sorts the array in-place according to the order specified by the given [comparator].
 */
public actual fun <T> Array<out T>.sortWith(comparator: Comparator<in T>): Unit {
    if (size > 1) sortArrayWith(this, 0, size, comparator)
}

/**
 * Sorts the array in-place according to the natural order of its elements.
 *
 * @throws ClassCastException if any element of the array is not [Comparable].
 */
public actual fun <T: Comparable<T>> Array<out T>.sort(): Unit {
    if (size > 1) sortArrayComparable(this)
}

public fun <T> Array<out T>.sortWith(comparator: Comparator<in T>, fromIndex: Int = 0, toIndex: Int = size): Unit {
    sortArrayWith(this, fromIndex, toIndex, comparator)
}

/**
 * Sorts the array in-place.
 */
public actual fun IntArray.sort(): Unit {
    if (size > 1) sortArray(this)
}

/**
 * Sorts the array in-place.
 */
public actual fun LongArray.sort(): Unit {
    if (size > 1) sortArray(this)
}

/**
 * Sorts the array in-place.
 */
public actual fun ByteArray.sort(): Unit {
    if (size > 1) sortArray(this)
}

/**
 * Sorts the array in-place.
 */
public actual fun ShortArray.sort(): Unit {
    if (size > 1) sortArray(this)
}

/**
 * Sorts the array in-place.
 */
public actual fun DoubleArray.sort(): Unit {
    if (size > 1) sortArray(this)
}

/**
 * Sorts the array in-place.
 */
public actual fun FloatArray.sort(): Unit {
    if (size > 1) sortArray(this)
}

/**
 * Sorts the array in-place.
 */
public actual fun CharArray.sort(): Unit {
    if (size > 1) sortArray(this)
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public actual fun ByteArray.toTypedArray(): Array<Byte> {
    val result = arrayOfNulls<Byte>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Byte>
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public actual fun ShortArray.toTypedArray(): Array<Short> {
    val result = arrayOfNulls<Short>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Short>
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public actual fun IntArray.toTypedArray(): Array<Int> {
    val result = arrayOfNulls<Int>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Int>
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public actual fun LongArray.toTypedArray(): Array<Long> {
    val result = arrayOfNulls<Long>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Long>
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public actual fun FloatArray.toTypedArray(): Array<Float> {
    val result = arrayOfNulls<Float>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Float>
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public actual fun DoubleArray.toTypedArray(): Array<Double> {
    val result = arrayOfNulls<Double>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Double>
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public actual fun BooleanArray.toTypedArray(): Array<Boolean> {
    val result = arrayOfNulls<Boolean>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Boolean>
}

/**
 * Returns a *typed* object array containing all of the elements of this primitive array.
 */
public actual fun CharArray.toTypedArray(): Array<Char> {
    val result = arrayOfNulls<Char>(size)
    for (index in indices)
        result[index] = this[index]
    @Suppress("UNCHECKED_CAST")
    return result as Array<Char>
}


/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public actual inline fun <T> Array<out T>.contentToString(): String {
    return this.subarrayContentToString(offset = 0, length = this.size)
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun ByteArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun ShortArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun IntArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun LongArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun FloatArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun DoubleArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun BooleanArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

/**
 * Returns a string representation of the contents of the specified array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun CharArray.contentToString(): String {
    if (size == 0)
        return "[]"
    val iMax = size - 1

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i])
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}


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
 * Returns `true` if the two specified arrays are *deeply* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 *
 * If two corresponding elements are nested arrays, they are also compared deeply.
 * If any of arrays contains itself on any nesting level the behavior is undefined.
 */
@SinceKotlin("1.1")
public actual infix fun <T> Array<out T>.contentDeepEquals(other: Array<out T>): Boolean {
    if (this === other) {
        return true
    }

    if (this.size != other.size) {
        return false
    }

    for (i in indices) {
        val v1 = this[i]
        val v2 = other[i]

        if (v1 == null || v2 == null) {
            if (v1 == v2) {
                continue
            } else {
                return false
            }
        }

        // TODO: Do a typecheck like if(v1.class == v2.class) return false
        // Cases:
        //   Array<T>
        //   Primitive array
        //   Some value (not an array)

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
            else -> if (v1 != v2) return false
        }

    }
    return true
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 * Nested arrays are treated as lists too.
 *
 * If any of arrays contains itself on any nesting level the behavior is undefined.
 */
@SinceKotlin("1.1")
public actual fun <T> Array<out T>.contentDeepHashCode(): Int {
    var result = 1
    for (element in this) {
        var elementHash = 0
        if (element is Array<*>)
            elementHash = element.contentDeepHashCode()
        else
            element?.let { elementHash = element.hashCode() }

        result = 31 * result + elementHash
    }
    return result
}

/**
 * Returns a string representation of the contents of this array as if it is a [List].
 * Nested arrays are treated as lists too.
 *
 * If any of arrays contains itself on any nesting level that reference
 * is rendered as `"[...]"` to prevent recursion.
 */
@SinceKotlin("1.1")
public actual fun <T> Array<out T>.contentDeepToString(): String {
    val length = if (size * 5 + 2 > 0) size * 5 + 2 else Int.MAX_VALUE
    val result = StringBuilder(length)
    contentDeepToStringInternal(result, mutableSetOf())
    return result.toString()
}

private fun <T> Array<out T>.contentDeepToStringInternal(result: StringBuilder, processed: MutableSet<Array<*>>) {
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
            else            -> result.append(element.toString())
        }
    }

    result.append(']')
    processed.remove(this)
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public actual infix fun <T> Array<out T>.contentEquals(other: Array<out T>): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public actual infix fun ByteArray.contentEquals(other: ByteArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public actual infix fun ShortArray.contentEquals(other: ShortArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public actual infix fun IntArray.contentEquals(other: IntArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public actual infix fun LongArray.contentEquals(other: LongArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public actual infix fun FloatArray.contentEquals(other: FloatArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public actual infix fun DoubleArray.contentEquals(other: DoubleArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public actual infix fun BooleanArray.contentEquals(other: BooleanArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true
}

/**
 * Returns `true` if the two specified arrays are *structurally* equal to one another,
 * i.e. contain the same number of the same elements in the same order.
 */
@SinceKotlin("1.1")
public actual infix fun CharArray.contentEquals(other: CharArray): Boolean {
    if (this === other) {
        return true
    }
    if (size != other.size) {
        return false
    }
    for (i in indices) {
        if (this[i] != other[i]) {
            return false
        }
    }
    return true
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun <T> Array<out T>.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + (element?.hashCode() ?: 0)
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun ByteArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + element.hashCode()
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun ShortArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + element.hashCode()
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun IntArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + element.hashCode()
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun LongArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + element.hashCode()
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun FloatArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + element.hashCode()
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun DoubleArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + element.hashCode()
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun BooleanArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + element.hashCode()
    return result
}

/**
 * Returns a hash code based on the contents of this array as if it is [List].
 */
@SinceKotlin("1.1")
public actual fun CharArray.contentHashCode(): Int {
    var result = 1
    for (element in this)
        result = 31 * result  + element.hashCode()
    return result
}

// From JS collections.kt.
internal actual fun <T> arrayOfNulls(reference: Array<T>, size: Int): Array<T> {
    return arrayOfNulls<Any>(size) as Array<T>
}

internal actual fun copyToArrayImpl(collection: Collection<*>): Array<Any?> {
    val array = arrayOfUninitializedElements<Any?>(collection.size)
    val iterator = collection.iterator()
    var index = 0
    while (iterator.hasNext())
        array[index++] = iterator.next()
    return array
}

internal actual fun <T> copyToArrayImpl(collection: Collection<*>, array: Array<T>): Array<T> {
    if (array.size < collection.size)
        return copyToArrayImpl(collection) as Array<T>

    val iterator = collection.iterator()
    var index = 0
    while (iterator.hasNext()) {
        array[index++] = iterator.next() as T
    }
    if (index < array.size) {
        return (array as Array<T>).copyOf(index) as Array<T>
    }
    return array
}

/**
 * Returns a *typed* array containing all of the elements of this collection.
 *
 * Allocates an array of runtime type `T` having its size equal to the size of this collection
 * and populates the array with the elements of this collection.
 */
public actual inline fun <reified T> Collection<T>.toTypedArray(): Array<T> {
    val result = arrayOfNulls<T>(size)
    var index = 0
    for (element in this) result[index++] = element
    @Suppress("UNCHECKED_CAST")
    return result as Array<T>
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public actual operator fun <T> Array<T>.plus(element: T): Array<T> {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public actual operator fun ByteArray.plus(element: Byte): ByteArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public actual operator fun ShortArray.plus(element: Short): ShortArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public actual operator fun IntArray.plus(element: Int): IntArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public actual operator fun LongArray.plus(element: Long): LongArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public actual operator fun FloatArray.plus(element: Float): FloatArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public actual operator fun DoubleArray.plus(element: Double): DoubleArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public actual operator fun BooleanArray.plus(element: Boolean): BooleanArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public actual operator fun CharArray.plus(element: Char): CharArray {
    val index = size
    val result = copyOfUninitializedElements(index + 1)
    result[index] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public actual operator fun <T> Array<T>.plus(elements: Collection<T>): Array<T> {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public actual operator fun ByteArray.plus(elements: Collection<Byte>): ByteArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public actual operator fun ShortArray.plus(elements: Collection<Short>): ShortArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public actual operator fun IntArray.plus(elements: Collection<Int>): IntArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public actual operator fun LongArray.plus(elements: Collection<Long>): LongArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public actual operator fun FloatArray.plus(elements: Collection<Float>): FloatArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public actual operator fun DoubleArray.plus(elements: Collection<Double>): DoubleArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public actual operator fun BooleanArray.plus(elements: Collection<Boolean>): BooleanArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] collection.
 */
public actual operator fun CharArray.plus(elements: Collection<Char>): CharArray {
    var index = size
    val result = copyOfUninitializedElements(index + elements.size)
    for (element in elements) result[index++] = element
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public actual operator fun <T> Array<T>.plus(elements: Array<out T>): Array<T> {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public actual operator fun ByteArray.plus(elements: ByteArray): ByteArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public actual operator fun ShortArray.plus(elements: ShortArray): ShortArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public actual operator fun IntArray.plus(elements: IntArray): IntArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public actual operator fun LongArray.plus(elements: LongArray): LongArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public actual operator fun FloatArray.plus(elements: FloatArray): FloatArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public actual operator fun DoubleArray.plus(elements: DoubleArray): DoubleArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public actual operator fun BooleanArray.plus(elements: BooleanArray): BooleanArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then all elements of the given [elements] array.
 */
public actual operator fun CharArray.plus(elements: CharArray): CharArray {
    val thisSize = size
    val arraySize = elements.size
    val result = copyOfUninitializedElements(thisSize + arraySize)
    elements.copyRangeTo(result, 0, arraySize, thisSize)
    return result
}

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
@kotlin.internal.InlineOnly
public actual inline fun <T> Array<T>.plusElement(element: T): Array<T> {
    return plus(element)
}
