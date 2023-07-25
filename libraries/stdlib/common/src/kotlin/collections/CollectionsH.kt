/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

expect interface RandomAccess

/**
 * Returns the array if it's not `null`, or an empty array otherwise.
 * @sample samples.collections.Arrays.Usage.arrayOrEmpty
 */
expect inline fun <reified T> Array<out T>?.orEmpty(): Array<out T>


expect inline fun <reified T> Collection<T>.toTypedArray(): Array<T>

@SinceKotlin("1.2")
expect fun <T> MutableList<T>.fill(value: T): Unit

@SinceKotlin("1.2")
expect fun <T> MutableList<T>.shuffle(): Unit

@SinceKotlin("1.2")
expect fun <T> Iterable<T>.shuffled(): List<T>

expect fun <T : Comparable<T>> MutableList<T>.sort(): Unit
expect fun <T> MutableList<T>.sortWith(comparator: Comparator<in T>): Unit


// from Grouping.kt
public expect fun <T, K> Grouping<T, K>.eachCount(): Map<K, Int>
// public expect inline fun <T, K> Grouping<T, K>.eachSumOf(valueSelector: (T) -> Int): Map<K, Int>

internal expect fun collectionToArray(collection: Collection<*>): Array<Any?>

internal expect fun <T> collectionToArray(collection: Collection<*>, array: Array<T>): Array<T>

internal expect fun <T> arrayOfNulls(reference: Array<T>, size: Int): Array<T>
internal expect fun <K, V> Map<K, V>.toSingletonMapOrSelf(): Map<K, V>
internal expect fun <K, V> Map<out K, V>.toSingletonMap(): Map<K, V>
internal expect fun <T> Array<out T>.copyToArrayOfAny(isVarargs: Boolean): Array<out Any?>
