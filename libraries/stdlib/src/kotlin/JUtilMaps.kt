package kotlin

import java.util.Map as JMap
import java.util.Map.Entry as JEntry
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.List
import java.util.LinkedHashMap
import java.util.Map
import java.util.TreeMap
import java.util.SortedMap
import java.util.Comparator

// Map APIs

/** Returns the size of the map */
val JMap<*,*>.size : Int
get() = size()

/** Returns true if this map is empty */
val JMap<*,*>.empty : Boolean
get() = isEmpty()

/** Provides [] access to maps */
public fun <K, V> JMap<K, V>.set(key : K, value : V) : V? = this.put(key, value)

/** Returns the [[Map]] if its not null otherwise it returns the empty [[Map]] */
public inline fun <K,V> java.util.Map<K,V>?.orEmpty() : java.util.Map<K,V>
= if (this != null) this else Collections.EMPTY_MAP as java.util.Map<K,V>


/** Returns the key of the entry */
val <K,V> JEntry<K,V>.key : K
    get() = getKey().sure()

/** Returns the value of the entry */
val <K,V> JEntry<K,V>.value : V
    get() = getValue().sure()

/**
 * Returns the value for the given key or returns the result of the defaultValue function if there was no entry for the given key
 *
 * @includeFunctionBody ../../test/MapTest.kt getOrElse
 */
public inline fun <K,V> java.util.Map<K,V>.getOrElse(key: K, defaultValue: ()-> V) : V {
    val current = this.get(key)
    if (current != null) {
        return current
    } else {
        return defaultValue()
    }
}

/**
 * Returns the value for the given key or the result of the defaultValue function is put into the map for the given value and returned
 *
 * @includeFunctionBody ../../test/MapTest.kt getOrElse
 */
public inline fun <K,V> java.util.Map<K,V>.getOrPut(key: K, defaultValue: ()-> V) : V {
    val current = this.get(key)
    if (current != null) {
        return current
    } else {
        val answer = defaultValue()
        this.put(key, answer)
        return answer
    }
}


/**
 * Returns an [[Iterator]] over the entries in the [[Map]]
 *
 * @includeFunctionBody ../../test/MapTest.kt iterateWithProperties
 */
public inline fun <K,V> java.util.Map<K,V>.iterator(): java.util.Iterator<java.util.Map.Entry<K,V>> {
    val entrySet = this.entrySet()!!
    return entrySet.iterator()!!
}

/**
 * Returns a new List containing the results of applying the given *transform* function to each [[Map.Entry]] in this [[Map]]
 *
 * @includeFunctionBody ../../test/CollectionTest.kt map
 */
public inline fun <K,V,R> java.util.Map<K,V>.map(transform: (java.util.Map.Entry<K,V>) -> R) : java.util.List<R> {
    return mapTo(java.util.ArrayList<R>(this.size), transform)
}

/**
 * Transforms each [[Map.Entry]] in this [[Map]] with the given *transform* function and
 * adds each return value to the given *results* collection
 */
public inline fun <K,V,R, C: Collection<in R>> java.util.Map<K,V>.mapTo(result: C, transform: (java.util.Map.Entry<K,V>) -> R) : C {
  for (item in this)
    result.add(transform(item))
  return result
}


/**
 * Returns a new Map containing the results of applying the given *transform* function to each [[Map.Entry]] in this [[Map]]
 *
 * @includeFunctionBody ../../test/MapTest.kt mapValues
 */
public inline fun <K,V,R> java.util.Map<K,V>.mapValues(transform : (java.util.Map.Entry<K,V>) -> R): java.util.Map<K,R> {
    return mapValuesTo(java.util.HashMap<K,R>(this.size), transform)
}

/**
 * Populates the given *result* [[Map]] with the value returned by applying the *transform* function on each [[Map.Entry]] in this [[Map]]
 */
public inline fun <K,V,R,C: java.util.Map<K,R>> java.util.Map<K,V>.mapValuesTo(result: C, transform : (java.util.Map.Entry<K,V>) -> R) : C {
  for (e in this) {
      val newValue = transform(e)
      result.put(e.key, newValue)
  }
  return result
}

/**
 * Puts all the entries into the map with the first value in the tuple being the key and the second the value
 */
public inline fun <K,V> java.util.Map<K,V>.putAll(vararg values: #(K,V)): Unit {
    for (v in values) {
        put(v._1, v._2)
    }
}

/**
 * Converts this [[Map]] to a [[LinkedHashMap]] so future insertion orders are maintained
 */
public inline fun <K,V> java.util.Map<K,V>.toLinkedMap(): LinkedHashMap<K,V> = toMap<K,V>(LinkedHashMap(size)) as LinkedHashMap<K,V>

/**
 * Converts this [[Map]] to a [[SortedMap]] so iteration order will be in key order
 *
 * @includeFunctionBody ../../test/MapTest.kt toSortedMap
 */
public inline fun <K,V> java.util.Map<K,V>.toSortedMap(): SortedMap<K,V> = toMap<K,V>(TreeMap()) as SortedMap<K,V>

/**
 * Converts this [[Map]] to a [[SortedMap]] using the given *comparator* so that iteration order will be in the order
 * defined by the comparator
 *
 * @includeFunctionBody ../../test/MapTest.kt toSortedMapWithComparator
 */
public inline fun <K,V> java.util.Map<K,V>.toSortedMap(comparator: Comparator<K>): SortedMap<K,V> = toMap<K,V>(TreeMap(comparator)) as SortedMap<K,V>

/**
 * Copies the entries in this [[Map]] to the given *map*
 */
public inline fun <K,V> java.util.Map<K,V>.toMap(map: Map<K,V>): Map<K,V> {
    map.putAll(this)
    return map
}
