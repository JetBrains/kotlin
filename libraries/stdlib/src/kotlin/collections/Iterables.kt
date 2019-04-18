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

/** Returns true when it's safe to convert this collection to a set without changing contains method behavior. */
private fun <T> Collection<T>.safeToConvertToSet() = size > 2 && this is ArrayList

/** Converts this collection to a set, when it's worth so and it doesn't change contains method behavior. */
internal fun <T> Iterable<T>.convertToSetForSetOperationWith(source: Iterable<T>): Collection<T> =
    when (this) {
        is Set -> this
        is Collection ->
            when {
                source is Collection && source.size < 2 -> this
                else -> if (this.safeToConvertToSet()) toHashSet() else this
            }
        else -> toHashSet()
    }

/** Converts this collection to a set, when it's worth so and it doesn't change contains method behavior. */
internal fun <T> Iterable<T>.convertToSetForSetOperation(): Collection<T> =
    when (this) {
        is Set -> this
        is Collection -> if (this.safeToConvertToSet()) toHashSet() else this
        else -> toHashSet()
    }


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
