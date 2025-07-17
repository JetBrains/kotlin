/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin.collections

/**
 * Given an [iterator] function constructs an [Iterable] instance that returns values through the [Iterator]
 * provided by that function.
 * @sample samples.collections.Iterables.Building.iterable
 */
@kotlin.internal.InlineOnly
public inline fun <T> Iterable(crossinline iterator: () -> Iterator<T>): Iterable<T> = object : Iterable<T> {
    override fun iterator(): Iterator<T> = iterator()
}

/**
 * A wrapper over another [Iterable] (or any other object that can produce an [Iterator]) that returns
 * an indexing iterator.
 */
internal class IndexingIterable<out T>(private val iteratorFactory: () -> Iterator<T>) : Iterable<IndexedValue<T>> {
    override fun iterator(): Iterator<IndexedValue<T>> = IndexingIterator(iteratorFactory())
}


/**
 * Returns the size of this iterable if it is known, or `null` otherwise.
 */
@PublishedApi
internal fun <T> Iterable<T>.collectionSizeOrNull(): Int? = if (this is Collection<*>) this.size else null

/**
 * Returns the size of this iterable if it is known, or the specified [default] value otherwise.
 */
@PublishedApi
internal fun <T> Iterable<T>.collectionSizeOrDefault(default: Int): Int = if (this is Collection<*>) this.size else default


/**
 * Returns a single list of all elements from all collections in the given collection.
 * @sample samples.collections.Iterables.Operations.flattenIterable
 */
public fun <T> Iterable<Iterable<T>>.flatten(): List<T> {
    val result = ArrayList<T>()
    for (element in this) {
        result.addAll(element)
    }
    return result
}

/**
 * Returns a pair of lists, where
 * *first* list is built from the first values of each pair from this collection,
 * *second* list is built from the second values of each pair from this collection.
 * @sample samples.collections.Iterables.Operations.unzipIterable
 */
public fun <T, R> Iterable<Pair<T, R>>.unzip(): Pair<List<T>, List<R>> {
    val expectedSize = collectionSizeOrDefault(10)
    val listT = ArrayList<T>(expectedSize)
    val listR = ArrayList<R>(expectedSize)
    for (pair in this) {
        listT.add(pair.first)
        listR.add(pair.second)
    }
    return listT to listR
}

/**
 * Returns `true` if the elements in this iterable are sorted according to the specified [comparator].
 *
 * Note that an empty or single-element iterable is always considered sorted.
 */
public fun <T> Iterable<T>.isSortedWith(comparator: Comparator<in T>): Boolean {
    if (this is Collection<T> && size < 2) return true
    val iterator = iterator()
    if (!iterator.hasNext()) return true
    var current = iterator.next()
    while (iterator.hasNext()) {
        val next = iterator.next()
        if (comparator.compare(current, next) > 0) return false
        current = next
    }
    return true
}

/**
 * Returns `true` if the elements in this iterable are sorted according to natural sort order of the value returned by specified [selector] function.
 *
 * Note that an empty or single-element iterable is always considered sorted.
 */
public inline fun <T, R : Comparable<R>> Iterable<T>.isSortedBy(crossinline selector: (T) -> R?): Boolean {
    return isSortedWith(compareBy(selector))
}

/**
 * Returns `true` if the elements in this iterable are sorted according to their natural sort order.
 *
 * Note that an empty or single-element iterable is always considered sorted.
 */
public fun <T : Comparable<T>> Iterable<T>.isSorted(): Boolean {
    return isSortedWith(naturalOrder())
}

/**
 * Returns `true` if the elements in this iterable are sorted descending according to their natural sort order.
 *
 * Note that an empty or single-element iterable is always considered sorted.
 */
public fun <T : Comparable<T>> Iterable<T>.isSortedDescending(): Boolean {
    return isSortedWith(reverseOrder())
}

/**
 * Returns `true` if the elements in this iterable are sorted descending according to natural sort order of the value returned by specified [selector] function.
 *
 * Note that an empty or single-element iterable is always considered sorted.
 */
public inline fun <T, R : Comparable<R>> Iterable<T>.isSortedByDescending(crossinline selector: (T) -> R?): Boolean {
    return isSortedWith(compareByDescending(selector))
}
