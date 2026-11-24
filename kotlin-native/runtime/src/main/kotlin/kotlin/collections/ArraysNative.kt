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
    val result = arrayOfNulls<T>(size)
    var index = 0
    for (element in this) result[index++] = element
    @Suppress("UNCHECKED_CAST")
    return result as Array<T>
}

internal actual fun collectionToArray(collection: Collection<*>): Array<Any?> =
        toArrayImpl(
                collection,
                empty = { emptyArray() },
                alloc = { size -> arrayOfNulls(size) },
                trim = { result, size -> result.copyOf(size) },
        )

private inline fun toArrayImpl(
        collection: Collection<*>,
        empty: () -> Array<Any?>,
        alloc: (Int) -> Array<Any?>,
        trim: (Array<Any?>, Int) -> Array<Any?>
): Array<Any?> {
    // Prefer `isEmpty()` over `size == 0`: on some (e.g., concurrent) collections `size` is slow or already outdated.
    if (collection.isEmpty()) return empty()
    val sizeHint = collection.size // Only a hint: the collection may have changed since `isEmpty()`.
    val iter = collection.iterator()
    var result = alloc(if (sizeHint > 0) sizeHint else 1) // `isEmpty()` was false, so expect at least one element.
    var i = 0

    // Phase 1: trust the size hint and call only `next()`, which halves the number of interface calls per element.
    try {
        while (i < result.size) {
            result[i] = iter.next()
            i++
        }
    } catch (_: NoSuchElementException) {
        return if (i == 0) empty() else trim(result, i)
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
