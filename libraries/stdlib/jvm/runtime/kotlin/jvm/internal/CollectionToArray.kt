/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("CollectionToArray")

package kotlin.jvm.internal

import java.lang.reflect.Array as JavaArray
import java.lang.NullPointerException as JavaNPE
import java.util.Arrays

private val EMPTY = emptyArray<Any?>() // shared empty array
private const val MAX_SIZE = Int.MAX_VALUE - 2 // empirically maximal array size that can be allocated without exceeding VM limits

// TODO: eventually should become internal @PublishedApi
@Deprecated("This function will be made internal in a future release")
@DeprecatedSinceKotlin(warningSince = "1.9")
@JvmName("toArray")
fun collectionToArray(collection: Collection<*>): Array<Any?> =
    toArrayImpl(
        collection,
        empty = { EMPTY },
        alloc = { size -> arrayOfNulls<Any?>(size) },
        trim = { result, size -> Arrays.copyOf(result, size) }
    )

// Note: Array<Any?> here can have any reference array JVM type at run time
// TODO: eventually should become internal @PublishedApi
@Deprecated("This function will be made internal in a future release")
@DeprecatedSinceKotlin(warningSince = "1.9")
@JvmName("toArray")
fun collectionToArray(collection: Collection<*>, a: Array<Any?>?): Array<Any?> {
    // Collection.toArray contract requires that NullPointerException is thrown when array is null
    if (a == null) throw JavaNPE()
    return toArrayImpl(
        collection,
        empty = {
            if (a.size > 0) a[0] = null
            a
        },
        alloc = { size ->
            @Suppress("UNCHECKED_CAST")
            if (size <= a.size) a else JavaArray.newInstance(a.javaClass.componentType, size) as Array<Any?>
        },
        trim = { result, size ->
            if (result === a) {
                a[size] = null
                a
            } else
                Arrays.copyOf(result, size)
        }
    )
}

private inline fun toArrayImpl(
    collection: Collection<*>,
    empty: () -> Array<Any?>,
    alloc: (Int) -> Array<Any?>,
    trim: (Array<Any?>, Int) -> Array<Any?>
): Array<Any?> {
    val size = collection.size
    if (size == 0) return empty() // quick path on zero size
    val iter = collection.iterator() // allocate iterator for non-empty collection
    if (!iter.hasNext()) return empty() // size was > 0, but no actual elements
    var result = alloc(size) // use size as a guess to allocate result array
    var i = 0
    // invariant: iter.hasNext is true && i < result.size
    while (true) {
        result[i++] = iter.next()
        if (i >= result.size) {
            if (!iter.hasNext()) return result // perfect match of array size
            // array size was too small -- grow array (invariant: i == result.size > 0 here)
            // now grow at a factor of 1.5 (we expect this to be extremely rare, but still use fast code)
            // note that corner case here is when i == 1 (should get newSize == 2)
            var newSize = (i * 3 + 1) ushr 1
            if (newSize <= i) { // detect overflow in the above line
                if (i >= MAX_SIZE) throw OutOfMemoryError() // exceeded max array that VM can allocate
                newSize = MAX_SIZE // try max array size that VM can allocate
            }
            result = Arrays.copyOf(result, newSize)
        } else {
            if (!iter.hasNext()) return trim(result, i) // ended too early (allocated array too big)
        }
    }
}
