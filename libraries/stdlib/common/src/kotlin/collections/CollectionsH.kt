/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Marker interface indicating that the [List] implementation supports fast indexed access.
 */
public expect interface RandomAccess

/**
 * Returns the array if it's not `null`, or an empty array otherwise.
 * @sample samples.collections.Arrays.Usage.arrayOrEmpty
 */
public expect inline fun <reified T> Array<out T>?.orEmpty(): Array<out T>

/**
 * Returns a *typed* array containing all the elements of this collection.
 *
 * Allocates an array of runtime type `T` having its size equal to the size of this collection
 * and populates the array with the elements of this collection.
 * @sample samples.collections.Collections.Collections.collectionToTypedArray
 */
public expect inline fun <reified T> Collection<T>.toTypedArray(): Array<T>

/**
 * Fills the list with the provided [value].
 *
 * Each element in the list gets replaced with the [value].
 */
@SinceKotlin("1.2")
public expect fun <T> MutableList<T>.fill(value: T): Unit

/**
 * Randomly shuffles elements in this list.
 *
 * See: https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle#The_modern_algorithm
 */
@SinceKotlin("1.2")
public expect fun <T> MutableList<T>.shuffle(): Unit

/**
 * Returns a new list with the elements of this collection randomly shuffled.
 */
@SinceKotlin("1.2")
public expect fun <T> Iterable<T>.shuffled(): List<T>

/**
 * Sorts elements in the list in-place according to their natural sort order.
 *
 * The sort is _stable_. It means that equal elements preserve their order relative to each other after sorting.
 *
 * @sample samples.collections.Collections.Sorting.sortMutableList
 */
public expect fun <T : Comparable<T>> MutableList<T>.sort(): Unit


/**
 * Sorts elements in the list in-place according to the order specified with [comparator].
 *
 * The sort is _stable_. It means that equal elements preserve their order relative to each other after sorting.
 *
 * @sample samples.collections.Collections.Sorting.sortMutableListWith
 */
public expect fun <T> MutableList<T>.sortWith(comparator: Comparator<in T>): Unit


// from Grouping.kt
public expect fun <T, K> Grouping<T, K>.eachCount(): Map<K, Int>
// public expect inline fun <T, K> Grouping<T, K>.eachSumOf(valueSelector: (T) -> Int): Map<K, Int>

internal expect fun collectionToArray(collection: Collection<*>): Array<Any?>

internal expect fun <T> collectionToArray(collection: Collection<*>, array: Array<T>): Array<T>

internal expect fun <T> arrayOfNulls(reference: Array<T>, size: Int): Array<T>
internal expect fun <K, V> Map<K, V>.toSingletonMapOrSelf(): Map<K, V>
internal expect fun <K, V> Map<out K, V>.toSingletonMap(): Map<K, V>
internal expect fun <T> Array<out T>.copyToArrayOfAny(isVarargs: Boolean): Array<out Any?>
