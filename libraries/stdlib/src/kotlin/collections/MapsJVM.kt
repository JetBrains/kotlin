package kotlin

import java.util.Comparator
import java.util.LinkedHashMap
import java.util.SortedMap
import java.util.TreeMap
import java.util.Properties

/**
 * Allows to use the index operator for storing values in a mutable map.
 */
// this code is JVM-specific, because JS has native set function
public fun <K, V> MutableMap<K, V>.set(key: K, value: V): V? = put(key, value)

/**
 * Converts this [Map] to a [SortedMap] so iteration order will be in key order
 *
 * @sample test.collections.MapJVMTest.toSortedMap
 */
public fun <K : Comparable<K>, V> Map<K, V>.toSortedMap(): SortedMap<K, V> = TreeMap(this)

/**
 * Converts this [Map] to a [SortedMap] using the given [comparator] so that iteration order will be in the order
 * defined by the comparator
 *
 * @sample test.collections.MapJVMTest.toSortedMapWithComparator
 */
public fun <K, V> Map<K, V>.toSortedMap(comparator: Comparator<K>): SortedMap<K, V> {
    val result = TreeMap<K, V>(comparator)
    result.putAll(this)
    return result
}

/**
 * Returns a new [SortedMap] with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value.
 *
 * @sample test.collections.MapJVMTest.createSortedMap
 */
public fun <K, V> sortedMapOf(vararg values: Pair<K, V>): SortedMap<K, V> {
    val answer = TreeMap<K, V>()
    /**
    TODO replace by this simpler call when we can pass vararg values into other methods
    answer.putAll(values)
     */
    for (v in values) {
        answer.put(v.first, v.second)
    }
    return answer
}


/**
 * Converts this [Map] to a [Properties] object
 *
 * @sample test.collections.MapJVMTest.toProperties
 */
public fun Map<String, String>.toProperties(): Properties {
    val answer = Properties()
    for (e in this) {
        answer.put(e.key, e.value)
    }
    return answer
}

