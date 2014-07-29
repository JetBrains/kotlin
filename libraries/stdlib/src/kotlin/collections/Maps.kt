package kotlin

import java.util.*

// Map APIs

/** Returns the size of the map */
public val Map<*, *>.size: Int
    get() = size()

/** Returns true if this map is empty */
public val Map<*, *>.empty: Boolean
    get() = isEmpty()

/** Returns the [[Map]] if its not null otherwise it returns the empty [[Map]] */
public fun <K,V> Map<K,V>?.orEmpty() : Map<K,V>
       = if (this != null) this else stdlib_emptyMap()

public fun <K,V> Map<K,V>.contains(key : K) : Boolean = containsKey(key)

/** Returns the key of the entry */
public val <K, V> Map.Entry<K, V>.key: K
    get() = getKey()

/** Returns the value of the entry */
public val <K, V> Map.Entry<K, V>.value: V
    get() = getValue()

/** Returns the key of the entry */
public fun <K, V> Map.Entry<K, V>.component1(): K {
    return getKey()
}

/** Returns the value of the entry */
public fun <K, V> Map.Entry<K, V>.component2(): V {
    return getValue()
}

/** Converts entry to Pair with key being first component and value being second */
public fun <K, V> Map.Entry<K, V>.toPair(): Pair<K, V> {
    return Pair(getKey(), getValue())
}

/**
 * Returns the value for the given key or returns the result of the defaultValue function if there was no entry for the given key
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt getOrElse
 */
public inline fun <K, V> Map<K, V>.getOrElse(key: K, defaultValue: () -> V): V {
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
public inline fun <K, V> MutableMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
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
public fun <K, V> Map<K, V>.iterator(): Iterator<Map.Entry<K, V>> {
    val entrySet = this.entrySet()
    return entrySet.iterator()
}

/**
 * Populates the given *destination* [[Map]] with the value returned by applying the *transform* function on each [[Map.Entry]] in this [[Map]]
 */
public inline fun <K, V, R, C : MutableMap<K, R>> Map<K, V>.mapValuesTo(destination: C, transform: (Map.Entry<K, V>) -> R): C {
    for (e in this) {
        val newValue = transform(e)
        destination.put(e.key, newValue)
    }
    return destination
}

/**
 * Populates the given *destination* [[Map]] with the value returned by applying the *transform* function on each [[Map.Entry]] in this [[Map]]
 */
public inline fun <K, V, R, C : MutableMap<R, V>> Map<K, V>.mapKeysTo(destination: C, transform: (Map.Entry<K, V>) -> R): C {
    for (e in this) {
        val newKey = transform(e)
        destination.put(newKey, e.value)
    }
    return destination
}

/**
 * Puts all the entries into this [[MutableMap]] with the first value in the pair being the key and the second the value
 */
public fun <K, V> MutableMap<K, V>.putAll(vararg values: Pair<K, V>): Unit {
    for ((key, value) in values) {
        put(key, value)
    }
}

/**
 * Puts all the entries into this [[MutableMap]] with the first value in the pair being the key and the second the value
 */
public fun <K, V> MutableMap<K, V>.putAll(values: Iterable<Pair<K,V>>): Unit {
    for ((key, value) in values) {
        put(key, value)
    }
}

/**
 * Returns a new Map containing the results of applying the given *transform* function to each [[Map.Entry]] in this [[Map]]
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt mapValues
 */
public inline fun <K, V, R> Map<K, V>.mapValues(transform: (Map.Entry<K, V>) -> R): Map<K, R> {
    return mapValuesTo(LinkedHashMap<K, R>(this.size), transform)
}

/**
 * Returns a new Map containing the results of applying the given *transform* function to each [[Map.Entry]] in this [[Map]]
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt mapKeys
 */
public inline fun <K, V, R> Map<K, V>.mapKeys(transform: (Map.Entry<K, V>) -> R): Map<R, V> {
    return mapKeysTo(LinkedHashMap<R, V>(this.size), transform)
}

/**
 * Returns a map containing all key-value pairs matching keys with the given *predicate*
 */
public inline fun <K, V> Map<K, V>.filterKeys(predicate: (K) -> Boolean): Map<K, V> {
    val result = LinkedHashMap<K, V>()
    for (entry in this) {
        if (predicate(entry.key)) {
            result.put(entry.key, entry.value)
        }
    }
    return result
}

/**
 * Returns a map containing all key-value pairs matching values with the given *predicate*
 */
public inline fun <K, V> Map<K, V>.filterValues(predicate: (V) -> Boolean): Map<K, V> {
    val result = LinkedHashMap<K, V>()
    for (entry in this) {
        if (predicate(entry.value)) {
            result.put(entry.key, entry.value)
        }
    }
    return result
}


/**
 * Appends all elements matching the given *predicate* into the given *destination*
 */
public inline fun <K, V, C : MutableMap<K, V>> Map<K, V>.filterTo(destination: C, predicate: (Map.Entry<K, V>) -> Boolean): C {
    for (element in this) {
        if (predicate(element)) {
            destination.put(element.key, element.value)
        }
    }
    return destination
}

/**
 * Returns a map containing all key-value pairs matching the given *predicate*
 */
public inline fun <K, V> Map<K, V>.filter(predicate: (Map.Entry<K, V>) -> Boolean): Map<K, V> {
    return filterTo(LinkedHashMap<K, V>(), predicate)
}

/**
 * Appends all elements matching the given *predicate* into the given *destination*
 */
public inline fun <K, V, C : MutableMap<K, V>> Map<K, V>.filterNotTo(destination: C, predicate: (Map.Entry<K, V>) -> Boolean): C {
    for (element in this) {
        if (!predicate(element)) {
            destination.put(element.key, element.value)
        }
    }
    return destination
}

/**
 * Returns a map containing all key-value pairs matching the given *predicate*
 */
public inline fun <K, V> Map<K, V>.filterNot(predicate: (Map.Entry<K, V>) -> Boolean): Map<K, V> {
    return filterNotTo(LinkedHashMap<K, V>(), predicate)
}

/**
 * Appends given [pair] to the mutable map.
 */
public fun <K, V> MutableMap<K, V>.plusAssign(pair: Pair<K, V>) {
    put(pair.first, pair.second)
}

/**
 * Returns a map containing all key-value pairs from the given collection
 */
public fun <K, V> Iterable<Pair<K, V>>.toMap(): Map<K, V> {
    val result = LinkedHashMap<K, V>()
    for (element in this) {
        result.put(element.first, element.second)
    }
    return result
}
