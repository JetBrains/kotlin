/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

import kotlin.wasm.internal.copyTo

private inline fun rangeCheck(index: Int, count: Int, arraySize: Int) {
    if (index < 0 || count < 0 || index + count > arraySize) throw IndexOutOfBoundsException()
}

internal inline fun arrayCopy(array: ByteArray, fromIndex: Int, destination: ByteArray, toIndex: Int, count: Int) {
    val srcStorage = array.storage
    rangeCheck(fromIndex, count, srcStorage.len())
    val dstStorage = destination.storage
    rangeCheck(toIndex, count, dstStorage.len())
    srcStorage.copyTo(dstStorage, fromIndex, toIndex, count)
}

internal inline fun arrayCopy(array: ShortArray, fromIndex: Int, destination: ShortArray, toIndex: Int, count: Int) {
    val srcStorage = array.storage
    rangeCheck(fromIndex, count, srcStorage.len())
    val dstStorage = destination.storage
    rangeCheck(toIndex, count, dstStorage.len())
    srcStorage.copyTo(dstStorage, fromIndex, toIndex, count)
}

internal inline fun arrayCopy(array: CharArray, fromIndex: Int, destination: CharArray, toIndex: Int, count: Int) {
    val srcStorage = array.storage
    rangeCheck(fromIndex, count, srcStorage.len())
    val dstStorage = destination.storage
    rangeCheck(toIndex, count, dstStorage.len())
    srcStorage.copyTo(dstStorage, fromIndex, toIndex, count)
}

internal inline fun arrayCopy(array: IntArray, fromIndex: Int, destination: IntArray, toIndex: Int, count: Int) {
    val srcStorage = array.storage
    rangeCheck(fromIndex, count, srcStorage.len())
    val dstStorage = destination.storage
    rangeCheck(toIndex, count, dstStorage.len())
    srcStorage.copyTo(dstStorage, fromIndex, toIndex, count)
}

internal inline fun arrayCopy(array: LongArray, fromIndex: Int, destination: LongArray, toIndex: Int, count: Int) {
    val srcStorage = array.storage
    rangeCheck(fromIndex, count, srcStorage.len())
    val dstStorage = destination.storage
    rangeCheck(toIndex, count, dstStorage.len())
    srcStorage.copyTo(dstStorage, fromIndex, toIndex, count)
}

internal inline fun arrayCopy(array: FloatArray, fromIndex: Int, destination: FloatArray, toIndex: Int, count: Int) {
    val srcStorage = array.storage
    rangeCheck(fromIndex, count, srcStorage.len())
    val dstStorage = destination.storage
    rangeCheck(toIndex, count, dstStorage.len())
    srcStorage.copyTo(dstStorage, fromIndex, toIndex, count)
}

internal inline fun arrayCopy(array: DoubleArray, fromIndex: Int, destination: DoubleArray, toIndex: Int, count: Int) {
    val srcStorage = array.storage
    rangeCheck(fromIndex, count, srcStorage.len())
    val dstStorage = destination.storage
    rangeCheck(toIndex, count, dstStorage.len())
    srcStorage.copyTo(dstStorage, fromIndex, toIndex, count)
}

internal inline fun arrayCopy(array: BooleanArray, fromIndex: Int, destination: BooleanArray, toIndex: Int, count: Int) {
    val srcStorage = array.storage
    rangeCheck(fromIndex, count, srcStorage.len())
    val dstStorage = destination.storage
    rangeCheck(toIndex, count, dstStorage.len())
    srcStorage.copyTo(dstStorage, fromIndex, toIndex, count)
}

internal inline fun arrayCopy(array: Array<Any?>, fromIndex: Int, destination: Array<Any?>, toIndex: Int, count: Int) {
    val srcStorage = array.storage
    rangeCheck(fromIndex, count, srcStorage.len())
    val dstStorage = destination.storage
    rangeCheck(toIndex, count, dstStorage.len())
    srcStorage.copyTo(dstStorage, fromIndex, toIndex, count)
}

/**
 * Returns a *typed* array containing all of the elements of this collection.
 *
 * Allocates an array of runtime type `T` having its size equal to the size of this collection
 * and populates the array with the elements of this collection.
 * @sample samples.collections.Collections.Collections.collectionToTypedArray
 */
@kotlin.internal.InlineOnly
public actual inline fun <T> Collection<T>.toTypedArray(): Array<T> = copyToArray(this)

@PublishedApi
internal fun <T> copyToArray(collection: Collection<T>): Array<T> =
    if (collection is AbstractCollection<T>)
        //TODO: Find more proper way to call abstract collection's toArray
        @Suppress("INVISIBLE_MEMBER") collection.toArray() as Array<T>
    else
        copyToArrayImpl(collection) as Array<T>

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
        (array as Array<T?>).fill(null, index)
    }
    return array
}