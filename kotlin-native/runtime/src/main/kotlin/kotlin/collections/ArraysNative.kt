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
    if (collection.isEmpty()) return empty() // Quick path on empty collection.
    val size = collection.size
    if (size == 0) return empty() // Quick path on zero size.
    val iter = collection.iterator() // Allocate iterator for potentially non-empty collection.
    if (!iter.hasNext()) return empty() // Size was > 0, but no actual elements.
    var result = alloc(size) // Use size as a guess to allocate result array.
    var i = 0
    // Invariant: iter.hasNext is true && i < result.size.
    while (true) {
        result[i++] = iter.next()
        if (i >= result.size) {
            if (!iter.hasNext()) return result // Perfect match of array size.
            // Array is too small, grow array (invariant: i == result.size > 0 here).
            // The result is full, so `i + 1` overflows only if the iterator yields more elements than any array can hold.
            val minCapacity = i + 1
            if (minCapacity < 0) throw OutOfMemoryError()
            // Grows at a factor of 1.5 (we expect this to be extremely rare, but still use fast code).
            result = result.copyOf(AbstractList.newCapacity(i, minCapacity))
        } else {
            if (!iter.hasNext()) return trim(result, i) // ended too early (allocated array too big)
        }
    }
}
