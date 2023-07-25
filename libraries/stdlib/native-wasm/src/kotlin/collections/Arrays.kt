/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

/**
 * Returns the array if it's not `null`, or an empty array otherwise.
 * @sample samples.collections.Arrays.Usage.arrayOrEmpty
 */
public actual inline fun <reified T> Array<out T>?.orEmpty(): Array<out T> = this ?: emptyArray<T>()

internal fun checkCopyOfRangeArguments(fromIndex: Int, toIndex: Int, size: Int) {
    if (toIndex > size)
        throw IndexOutOfBoundsException("toIndex ($toIndex) is greater than size ($size).")
    if (fromIndex > toIndex)
        throw IllegalArgumentException("fromIndex ($fromIndex) is greater than toIndex ($toIndex).")
}

@kotlin.internal.InlineOnly
@PublishedApi
/**
 * Returns a string representation of the contents of the subarray of the specified array as if it is [List].
 */
internal inline fun <T> Array<out T>.subarrayContentToString(offset: Int, length: Int, thisCollection: Collection<T>): String {
    val sb = StringBuilder(2 + length * 3)
    sb.append("[")
    var i = 0
    while (i < length) {
        if (i > 0) sb.append(", ")
        val nextElement = this[offset + i]
        if (nextElement === thisCollection) {
            sb.append("(this Collection)")
        } else {
            sb.append(nextElement)
        }
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
@OptIn(ExperimentalUnsignedTypes::class)
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

internal actual fun collectionToArray(collection: Collection<*>): Array<Any?> = collectionToArrayCommonImpl(collection)

internal actual fun <T> collectionToArray(collection: Collection<*>, array: Array<T>): Array<T> = collectionToArrayCommonImpl(collection, array)

internal actual fun <T> terminateCollectionToArray(collectionSize: Int, array: Array<T>): Array<T> = array

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