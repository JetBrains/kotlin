/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MapsKt")

package kotlin.collections

import java.util.Comparator
import java.util.Properties
import java.util.SortedMap
import java.util.TreeMap
import java.util.concurrent.ConcurrentMap
import kotlin.collections.builders.MapBuilder

/**
 * Returns a new read-only map, mapping only the specified key to the
 * specified value.
 *
 * The returned map is serializable.
 *
 * @sample samples.collections.Maps.Instantiation.mapFromPairs
 */
public actual fun <K, V> mapOf(pair: Pair<K, V>): Map<K, V> = java.util.Collections.singletonMap(pair.first, pair.second)

@PublishedApi
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
internal actual inline fun <K, V> buildMapInternal(builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> {
    return build(createMapBuilder<K, V>().apply(builderAction))
}

@PublishedApi
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
internal actual inline fun <K, V> buildMapInternal(capacity: Int, builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> {
    return build(createMapBuilder<K, V>(capacity).apply(builderAction))
}

@PublishedApi
@SinceKotlin("1.3")
internal fun <K, V> createMapBuilder(): MutableMap<K, V> {
    return MapBuilder()
}

@PublishedApi
@SinceKotlin("1.3")
internal fun <K, V> createMapBuilder(capacity: Int): MutableMap<K, V> {
    return MapBuilder(capacity)
}

@PublishedApi
@SinceKotlin("1.3")
internal fun <K, V> build(builder: MutableMap<K, V>): Map<K, V> {
    return (builder as MapBuilder<K, V>).build()
}


/**
 * Concurrent getOrPut, that is safe for concurrent maps.
 *
 * Returns the value for the given [key] if the value is present and not `null`.
 * Otherwise, calls the [defaultValue] function,
 * puts its result into the map under the given key and returns the call result.
 *
 * This function guarantees not to put the new value into the map if the key is already
 * mapped to a non-null value. However, the [defaultValue] function may still be invoked.
 *
 * This function relies on [ConcurrentMap.computeIfAbsent]. Hence, the `ConcurrentMap` implementations
 * that support `null` values must override the default `computeIfAbsent` implementation.
 *
 * @sample samples.collections.Maps.Usage.getOrPut
 */
public inline fun <K, V> ConcurrentMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    // Do not use computeIfAbsent on JVM8 as it would change locking behavior
    this.get(key)?.let { return it }

    val newValue = defaultValue()
    return try {
        // computeIfAbsent doesn't put the newValue if it's null
        if (newValue == null) {
            this.putIfAbsent(key, newValue) ?: newValue
        } else {
            this.computeIfAbsent(key) { newValue }
        }
    } catch (e: LinkageError) {
        // In Android projects without the desugared library, computeIfAbsent is available since SDK 24.
        // See https://developer.android.com/studio/write/java8-support-table for more info.
        // Use putIfAbsent as a fallback in this case.
        // Note: If the key was mapped to a null value, putIfAbsent won't replace it, but the new value will still be returned.
        this.putIfAbsent(key, newValue) ?: newValue
    }
}


/**
 * Converts this [Map] to a [SortedMap]. The resulting [SortedMap] determines the equality and order of keys according to their natural sorting order.
 *
 * Note that if the natural sorting order of keys considers any two keys of this map equal
 * (this could happen if the equality of keys according to [Comparable.compareTo] is inconsistent with the equality according to [Any.equals]),
 * only the value associated with the last of them gets into the resulting map.
 *
 * @sample samples.collections.Maps.Transformations.mapToSortedMap
 */
public fun <K : Comparable<K>, V> Map<out K, V>.toSortedMap(): SortedMap<K, V> = TreeMap(this)

/**
 * Converts this [Map] to a [SortedMap]. The resulting [SortedMap] determines the equality and order of keys according to the sorting order provided by the given [comparator].
 *
 * Note that if the `comparator` considers any two keys of this map equal, only the value associated with the last of them gets into the resulting map.
 *
 * @sample samples.collections.Maps.Transformations.mapToSortedMapWithComparator
 */
public fun <K, V> Map<out K, V>.toSortedMap(comparator: Comparator<in K>): SortedMap<K, V> =
    TreeMap<K, V>(comparator).apply { putAll(this@toSortedMap) }

/**
 * Returns a new [SortedMap] with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value.
 *
 * The resulting [SortedMap] determines the equality and order of keys according to their natural sorting order.
 *
 * @sample samples.collections.Maps.Instantiation.sortedMapFromPairs
 */
public fun <K : Comparable<K>, V> sortedMapOf(vararg pairs: Pair<K, V>): SortedMap<K, V> =
    TreeMap<K, V>().apply { putAll(pairs) }

/**
 * Returns a new [SortedMap] with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value.
 *
 * The resulting [SortedMap] determines the equality and order of keys according to the sorting order provided by the given [comparator].
 *
 * @sample samples.collections.Maps.Instantiation.sortedMapWithComparatorFromPairs
 */
@SinceKotlin("1.4")
public fun <K, V> sortedMapOf(comparator: Comparator<in K>, vararg pairs: Pair<K, V>): SortedMap<K, V> =
    TreeMap<K, V>(comparator).apply { putAll(pairs) }


/**
 * Converts this [Map] to a [Properties] object.
 *
 * @sample samples.collections.Maps.Transformations.mapToProperties
 */
@kotlin.internal.InlineOnly
public inline fun Map<String, String>.toProperties(): Properties =
    Properties().apply { putAll(this@toProperties) }


// creates a singleton copy of map, if there is specialization available in target platform, otherwise returns itself
@kotlin.internal.InlineOnly
internal actual inline fun <K, V> Map<K, V>.toSingletonMapOrSelf(): Map<K, V> = toSingletonMap()

// creates a singleton copy of map
internal actual fun <K, V> Map<out K, V>.toSingletonMap(): Map<K, V> =
    with(entries.iterator().next()) { java.util.Collections.singletonMap(key, value) }

/**
 * Calculate the initial capacity of a map, based on Guava's
 * [com.google.common.collect.Maps.capacity](https://github.com/google/guava/blob/v28.2/guava/src/com/google/common/collect/Maps.java#L325)
 * approach.
 */
@PublishedApi
internal actual fun mapCapacity(expectedSize: Int): Int = when {
    // We are not coercing the value to a valid one and not throwing an exception. It is up to the caller to
    // properly handle negative values.
    expectedSize < 0 -> expectedSize
    expectedSize < 3 -> expectedSize + 1
    expectedSize < INT_MAX_POWER_OF_TWO -> ((expectedSize / 0.75F) + 1.0F).toInt()
    // any large value
    else -> Int.MAX_VALUE
}
private const val INT_MAX_POWER_OF_TWO: Int = 1 shl (Int.SIZE_BITS - 2)
