package kotlin

import java.util.Comparator
import java.util.LinkedHashMap
import java.util.SortedMap
import java.util.TreeMap
import java.util.Properties

// Map APIs

/**
 * Converts this [[Map]] to a [[LinkedHashMap]] so future insertion orders are maintained
 */
public inline fun <K,V> Map<K,V>.toLinkedMap(): LinkedHashMap<K,V> = toMap<K,V>(LinkedHashMap(size)) as LinkedHashMap<K,V>

/**
 * Converts this [[Map]] to a [[SortedMap]] so iteration order will be in key order
 *
 * @includeFunctionBody ../../test/MapTest.kt toSortedMap
 */
public inline fun <K,V> Map<K,V>.toSortedMap(): SortedMap<K,V> = toMap<K,V>(TreeMap()) as SortedMap<K,V>

/**
 * Converts this [[Map]] to a [[SortedMap]] using the given *comparator* so that iteration order will be in the order
 * defined by the comparator
 *
 * @includeFunctionBody ../../test/MapTest.kt toSortedMapWithComparator
 */
public inline fun <K,V> Map<K,V>.toSortedMap(comparator: Comparator<K>): SortedMap<K,V> = toMap<K,V>(TreeMap(comparator)) as SortedMap<K,V>


/**
 * Converts this [[Map]] to a [[Properties]] object
 *
 * @includeFunctionBody ../../test/MapTest.kt toProperties
 */
public inline fun Map<String, String>.toProperties(): Properties {
    val answer = Properties()
    for (e in this) {
        answer.put(e.key, e.value)
    }
    return answer
}

