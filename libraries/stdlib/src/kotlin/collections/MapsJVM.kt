@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MapsKt")
@file:kotlin.jvm.JvmVersion

package kotlin.collections

import java.util.Comparator
import java.util.LinkedHashMap
import java.util.Properties
import java.util.SortedMap
import java.util.TreeMap
import java.util.concurrent.ConcurrentMap



/**
 * Concurrent getOrPut, that is safe for concurrent maps.
 *
 * Returns the value for the given [key]. If the key is not found in the map, calls the [defaultValue] function,
 * puts its result into the map under the given key and returns it.
 *
 * This method guarantees not to put the value into the map if the key is already there,
 * but the [defaultValue] function may be invoked even if the key is already in the map.
 */
public inline fun <K, V> ConcurrentMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    // Do not use computeIfAbsent on JVM8 as it would change locking behavior
    return this.get(key) ?:
            defaultValue().let { default -> this.putIfAbsent(key, default) ?: default }

}


/**
 * Converts this [Map] to a [SortedMap] so iteration order will be in key order.
 *
 * @sample samples.collections.Maps.Transformations.mapToSortedMap
 */
public fun <K : Comparable<K>, V> Map<out K, V>.toSortedMap(): SortedMap<K, V> = TreeMap(this)

/**
 * Converts this [Map] to a [SortedMap] using the given [comparator] so that iteration order will be in the order
 * defined by the comparator.
 *
 * @sample samples.collections.Maps.Transformations.mapToSortedMapWithComparator
 */
public fun <K, V> Map<out K, V>.toSortedMap(comparator: Comparator<in K>): SortedMap<K, V>
        = TreeMap<K, V>(comparator).apply { putAll(this@toSortedMap) }

/**
 * Returns a new [SortedMap] with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value.
 *
 * @sample samples.collections.Maps.Instantiation.sortedMapFromPairs
 */
public fun <K : Comparable<K>, V> sortedMapOf(vararg pairs: Pair<K, V>): SortedMap<K, V>
        = TreeMap<K, V>().apply { putAll(pairs) }


/**
 * Converts this [Map] to a [Properties] object.
 *
 * @sample samples.collections.Maps.Transformations.mapToProperties
 */
@kotlin.internal.InlineOnly
public inline fun Map<String, String>.toProperties(): Properties
        = Properties().apply { putAll(this@toProperties) }

