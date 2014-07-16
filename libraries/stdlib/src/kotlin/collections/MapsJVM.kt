package kotlin

import java.util.Comparator
import java.util.LinkedHashMap
import java.util.SortedMap
import java.util.TreeMap
import java.util.Properties

// Map APIs

// Move back to Maps.kt after KT-2093 will be fixed
/** Provides [] access to maps */
public fun <K, V> MutableMap<K, V>.set(key: K, value: V): V? = this.put(key, value)

/**
 * Converts this [[Map]] to a [[LinkedHashMap]] so future insertion orders are maintained
 */
public fun <K, V> Map<K, V>.toLinkedMap(): LinkedHashMap<K, V> = LinkedHashMap(this)

/**
 * Converts this [[Map]] to a [[SortedMap]] so iteration order will be in key order
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt toSortedMap
 */
public fun <K, V> Map<K, V>.toSortedMap(): SortedMap<K, V> = TreeMap(this)

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

