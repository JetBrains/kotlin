/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Returns a *typed* array containing all the elements of this collection.
 *
 * Allocates an array of runtime type `T` having its size equal to the size of this collection
 * and populates the array with the elements of this collection.
 * @sample samples.collections.Collections.Collections.collectionToTypedArray
 */
public actual inline fun <reified T> Collection<T>.toTypedArray(): Array<T> {
    @Suppress("UNCHECKED_CAST")
    return collectionToArray(this) as Array<T>
}

@PublishedApi
internal actual fun collectionToArray(collection: Collection<*>): Array<Any?> =
        collectionToArrayImpl(
                collection,
                empty = { emptyArray() },
                alloc = { size -> arrayOfNulls(size) },
                trim = { result, size -> result.copyOf(size) },
        )

internal fun iteratorToArray(first: Any?, iterator: Iterator<Any?>, sizeHint: Int): Array<Any?> =
        iteratorToArrayImpl(
                first, iterator, sizeHint,
                alloc = { size -> arrayOfNulls(size) },
                trim = { result, size -> result.copyOf(size) },
        )

private inline fun collectionToArrayImpl(
        collection: Collection<*>,
        empty: () -> Array<Any?>,
        alloc: (Int) -> Array<Any?>,
        trim: (Array<Any?>, Int) -> Array<Any?>
): Array<Any?> {
    // Prefer `isEmpty()` over `size == 0`: on some (e.g., concurrent) collections `size` is slow or already outdated.
    if (collection.isEmpty()) return empty()
    val sizeHint = collection.size // Only a hint: the collection may have changed since `isEmpty()`.
    val iter = collection.iterator()
    val array = alloc(if (sizeHint > 0) sizeHint else 1) // `isEmpty()` was false, so expect at least one element.

    try {
        array[0] = iter.next()
    } catch (_: NoSuchElementException) {
        return empty()
    }

    return fillRestOfAray(array, iter, trim)
}

private inline fun iteratorToArrayImpl(
        first: Any?,
        iter: Iterator<Any?>,
        sizeHint: Int,
        alloc: (Int) -> Array<Any?>,
        trim: (Array<Any?>, Int) -> Array<Any?>
): Array<Any?> {
    val array = alloc(if (sizeHint > 0) sizeHint else 1) // We've got `first`, so there is at least one element.
    array[0] = first

    return fillRestOfAray(array, iter, trim)
}

private inline fun fillRestOfAray(
        array: Array<Any?>,
        iter: Iterator<Any?>,
        trim: (Array<Any?>, Int) -> Array<Any?>,
): Array<Any?> {
    require(array.isNotEmpty())
    var result = array

    var i = 1

    // Phase 1: trust the size hint and call only `next()`, which halves the number of interface calls per element.
    try {
        while (i < result.size) {
            result[i] = iter.next()
            i++
        }
    } catch (_: NoSuchElementException) {
        return trim(result, i)
    }

    // Phase 2: the result is full (i == result.size). The hint was exact or too small, so check `hasNext()` for every element.
    while (iter.hasNext()) {
        if (i == result.size) {
            // The result is full, so `i + 1` overflows only if the iterator yields more elements than any array can hold.
            val minCapacity = i + 1
            if (minCapacity < 0) throw OutOfMemoryError()
            // Grows at a factor of 1.5 (we expect this to be extremely rare, but still use fast code).
            result = result.copyOf(AbstractList.newCapacity(i, minCapacity))
        }
        result[i++] = iter.next()
    }
    return if (i == result.size) result else trim(result, i) // Trim if the array was grown beyond what was needed.
}
