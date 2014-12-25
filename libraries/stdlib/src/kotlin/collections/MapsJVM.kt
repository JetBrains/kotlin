package kotlin

import java.util.Comparator
import java.util.LinkedHashMap
import java.util.SortedMap
import java.util.TreeMap
import java.util.Properties

/** Provides indexed write access to mutable maps */
// this code is JVM-specific, because JS has native set function
public fun <K, V> MutableMap<K, V>.set(key: K, value: V): V? = put(key, value)

/**
 * Converts this [[Map]] to a [[SortedMap]] so iteration order will be in key order
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt toSortedMap
 */
public fun <K : Any, V> Map<K, V>.toSortedMap(): SortedMap<K, V> = TreeMap(this)

/**
 * Converts this [[Map]] to a [[SortedMap]] using the given *comparator* so that iteration order will be in the order
 * defined by the comparator
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt toSortedMapWithComparator
 */
public fun <K, V> Map<K, V>.toSortedMap(comparator: Comparator<K>): SortedMap<K, V> {
    val result = TreeMap<K, V>(comparator)
    result.putAll(this)
    return result
}

/**
 * Returns a new [[SortedMap]] populated with the given pairs where the first value in each pair
 * is the key and the second value is the value
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt createSortedMap
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
 * Converts this [[Map]] to a [[Properties]] object
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt toProperties
 */
public fun Map<String, String>.toProperties(): Properties {
    val answer = Properties()
    for (e in this) {
        answer.put(e.key, e.value)
    }
    return answer
}

