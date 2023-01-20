/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Returns a *typed* array containing all of the elements of this collection.
 *
 * Allocates an array of runtime type `T` having its size equal to the size of this collection
 * and populates the array with the elements of this collection.
 * @sample samples.collections.Collections.Collections.collectionToTypedArray
 */
@kotlin.internal.InlineOnly
public actual inline fun <T> Collection<T>.toTypedArray(): Array<T> = copyToArray(this)

@Suppress("UNCHECKED_CAST")
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