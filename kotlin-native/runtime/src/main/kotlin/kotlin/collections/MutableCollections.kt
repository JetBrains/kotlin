/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

/**
 * Sorts elements in the list in-place according to their natural sort order.
 *
 * The sort is _stable_. It means that equal elements preserve their order relative to each other after sorting.
 */
public actual fun <T : Comparable<T>> MutableList<T>.sort(): Unit = sortWith(naturalOrder())

/**
 * Sorts elements in the list in-place according to the order specified with [comparator].
 *
 * The sort is _stable_. It means that equal elements preserve their order relative to each other after sorting.
 */
public actual fun <T> MutableList<T>.sortWith(comparator: Comparator<in T>): Unit {
    if (size > 1) {
        val it = listIterator()
        val sortedArray = @Suppress("UNCHECKED_CAST") (toTypedArray<Any?>() as Array<T>).apply { sortWith(comparator) }
        for (v in sortedArray) {
            it.next()
            it.set(v)
        }
    }
}
