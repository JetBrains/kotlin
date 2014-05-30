package kotlin

import java.util.Collections
import java.util.HashMap

// Map APIs

/** Returns the size of the map */
public val Map<*,*>.size : Int
get() = size()

/** Returns true if this map is empty */
public val Map<*,*>.empty : Boolean
get() = isEmpty()

/** Provides [] access to maps */
public fun <K, V> MutableMap<K, V>.set(key : K, value : V) : V? = this.put(key, value)

/** Returns the [[Map]] if its not null otherwise it returns the empty [[Map]] */
public fun <K,V> Map<K,V>?.orEmpty() : Map<K,V>
= if (this != null) this else stdlib_emptyMap()

public fun <K,V> Map<K,V>.contains(key : K) : Boolean = containsKey(key)

/** Returns the key of the entry */
public val <K,V> Map.Entry<K,V>.key : K
    get() = getKey()

/** Returns the value of the entry */
public val <K,V> Map.Entry<K,V>.value : V
    get() = getValue()

/** Returns the key of the entry */
fun <K,V> Map.Entry<K,V>.component1() : K {
    return getKey()
}

/** Returns the value of the entry */
fun <K,V> Map.Entry<K,V>.component2() : V {
    return getValue()
}

/**
 * Returns the value for the given key or returns the result of the defaultValue function if there was no entry for the given key
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt getOrElse
 */
public inline fun <K,V> Map<K,V>.getOrElse(key: K, defaultValue: ()-> V) : V {
    if (this.containsKey(key)) {
        return this.get(key) as V
    } else {
        return defaultValue()
    }
}

/**
 * Returns the value for the given key or the result of the defaultValue function is put into the map for the given value and returned
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt getOrPut
 */
public inline fun <K,V> MutableMap<K,V>.getOrPut(key: K, defaultValue: ()-> V) : V {
    if (this.containsKey(key)) {
        return this.get(key) as V
    } else {
        val answer = defaultValue()
        this.put(key, answer)
        return answer
    }
}


/**
 * Returns an [[Iterator]] over the entries in the [[Map]]
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt iterateWithProperties
 */
public fun <K,V> Map<K,V>.iterator(): Iterator<Map.Entry<K,V>> {
    val entrySet = this.entrySet()
    return entrySet.iterator()
}

/**
 * Populates the given *result* [[Map]] with the value returned by applying the *transform* function on each [[Map.Entry]] in this [[Map]]
 */
public inline fun <K,V,R,C: MutableMap<K,R>> Map<K,V>.mapValuesTo(result: C, transform : (Map.Entry<K,V>) -> R) : C {
  for (e in this) {
      val newValue = transform(e)
      result.put(e.key, newValue)
  }
  return result
}

/**
 * Puts all the entries into this [[MutableMap]] with the first value in the pair being the key and the second the value
 */
public fun <K,V> MutableMap<K,V>.putAll(vararg values: Pair<K, V>): Unit {
    for ((key, value) in values) {
        put(key, value)
    }
}

/**
 * Copies the entries in this [[Map]] to the given mutable *map*
 */
public fun <K,V, C : MutableMap<K, in V>> Map<K,V>.toMap(map: C): C {
    map.putAll(this)
    return map
}

/**
 * Copies the entries from given iterable of pairs to the given mutable *map*
 */
public fun <K,V, C : MutableMap<K, in V>> Iterable<Pair<K,V>>.toMap(map: C): C {
    for((key,value) in this) {
        if (map.containsKey(key)) {
            throw DuplicateKeyException()
        }
        map.put(key,value)
    }
    return map
}

/**
 * Creates map from given iterable of pairs
 */
public fun <K,V> Iterable<Pair<K,V>>.toMap() : Map<K,V> {
    return toMap(HashMap<K,V>())
}

/**
 * Returns a new Map containing the results of applying the given *transform* function to each [[Map.Entry]] in this [[Map]]
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt mapValues
 */
public inline fun <K,V,R> Map<K,V>.mapValues(transform : (Map.Entry<K,V>) -> R): Map<K,R> {
    return mapValuesTo(java.util.HashMap<K,R>(this.size), transform)
}
