/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

import kotlin.internal.InlineOnly
import kotlin.random.Random

// Copied from Kotlin/Native

actual interface RandomAccess

/** Returns the array if it's not `null`, or an empty array otherwise. */
actual inline fun <reified T> Array<out T>?.orEmpty(): Array<out T> = this ?: emptyArray<T>()


public actual inline fun <reified T> Collection<T>.toTypedArray(): Array<T> {
    val result = arrayOfNulls<T>(size)
    var index = 0
    for (element in this) result[index++] = element
    @Suppress("UNCHECKED_CAST")
    return result as Array<T>
}

// from Grouping.kt
// public actual inline fun <T, K> Grouping<T, K>.eachSumOf(valueSelector: (T) -> Int): Map<K, Int>

@Suppress("NOTHING_TO_INLINE")
internal inline actual fun <K, V> Map<K, V>.toSingletonMapOrSelf(): Map<K, V> = toSingletonMap()

// creates a singleton copy of map
internal actual fun <K, V> Map<out K, V>.toSingletonMap(): Map<K, V>
        = with(entries.iterator().next()) { mutableMapOf(key to value) }

/** Copies typed varargs array to an array of objects */
internal actual fun <T> Array<out T>.copyToArrayOfAny(isVarargs: Boolean): Array<out Any?> =
    // if the array came from varargs and already is array of Any, copying isn't required.
    if (isVarargs) this
    else this.copyOfUninitializedElements(this.size)


/**
 * Returns an immutable set containing only the specified object [element].
 */
public fun <T> setOf(element: T): Set<T> = hashSetOf(element)

@PublishedApi
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
internal actual inline fun <E> buildSetInternal(builderAction: MutableSet<E>.() -> Unit): Set<E> {
    return HashSet<E>().apply(builderAction).build()
}

@PublishedApi
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
internal actual inline fun <E> buildSetInternal(capacity: Int, builderAction: MutableSet<E>.() -> Unit): Set<E> {
    return HashSet<E>(capacity).apply(builderAction).build()
}


/**
 * Returns an immutable map, mapping only the specified key to the
 * specified value.
 */
public fun <K, V> mapOf(pair: Pair<K, V>): Map<K, V> = hashMapOf(pair)

@PublishedApi
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
internal actual inline fun <K, V> buildMapInternal(builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> {
    return HashMap<K, V>().apply(builderAction).build()
}

@PublishedApi
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
internal actual inline fun <K, V> buildMapInternal(capacity: Int, builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> {
    return HashMap<K, V>(capacity).apply(builderAction).build()
}

@PublishedApi
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
internal actual inline fun <E> buildListInternal(builderAction: MutableList<E>.() -> Unit): List<E> {
    return ArrayList<E>().apply(builderAction).build()
}

@PublishedApi
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@kotlin.internal.InlineOnly
internal actual inline fun <E> buildListInternal(capacity: Int, builderAction: MutableList<E>.() -> Unit): List<E> {
    return ArrayList<E>(capacity).apply(builderAction).build()
}


/**
 * Replaces each element in the list with a result of a transformation specified.
 */
public fun <T> MutableList<T>.replaceAll(transformation: (T) -> T) {
    val it = listIterator()
    while (it.hasNext()) {
        val element = it.next()
        it.set(transformation(element))
    }
}

/**
 * Groups elements from the [Grouping] source by key and counts elements in each group.
 *
 * @return a [Map] associating the key of each group with the count of elements in the group.
 *
 * @sample samples.collections.Grouping.groupingByEachCount
 */
@SinceKotlin("1.1")
public actual fun <T, K> Grouping<T, K>.eachCount(): Map<K, Int> = eachCountTo(mutableMapOf<K, Int>())

// Copied from JS.

/**
 * Fills the list with the provided [value].
 *
 * Each element in the list gets replaced with the [value].
 */
@SinceKotlin("1.2")
public actual fun <T> MutableList<T>.fill(value: T): Unit {
    for (index in 0..lastIndex) {
        this[index] = value
    }
}

/**
 * Randomly shuffles elements in this list.
 *
 * See: https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle#The_modern_algorithm
 */
@SinceKotlin("1.2")
public actual fun <T> MutableList<T>.shuffle(): Unit {
    for (i in lastIndex downTo 1) {
        val j = Random.nextInt(i + 1)
        val copy = this[i]
        this[i] = this[j]
        this[j] = copy
    }
}

/**
 * Returns a new list with the elements of this list randomly shuffled.
 */
@SinceKotlin("1.2")
public actual fun <T> Iterable<T>.shuffled(): List<T> = toMutableList().apply { shuffle() }

@PublishedApi
@SinceKotlin("1.3")
@InlineOnly
internal actual inline fun checkIndexOverflow(index: Int): Int {
    if (index < 0) {
        // TODO: api version check?
        throwIndexOverflow()
    }
    return index
}

@PublishedApi
@SinceKotlin("1.3")
@InlineOnly
internal actual inline fun checkCountOverflow(count: Int): Int {
    if (count < 0) {
        // TODO: api version check?
        throwCountOverflow()
    }
    return count
}
