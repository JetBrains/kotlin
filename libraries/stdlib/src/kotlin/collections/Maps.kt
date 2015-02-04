package kotlin

import java.util.*

private object EmptyMap : Map<Any, Any> {
    private val map = HashMap<Any, Any>()

    override fun containsKey(key: Any?): Boolean = map.containsKey(key)
    override fun containsValue(value: Any?): Boolean = map.containsValue(value)
    override fun entrySet(): Set<Map.Entry<Any, Any>> = map.entrySet()
    override fun get(key: Any?): Any? = map.get(key)
    override fun keySet(): Set<Any> = map.keySet()
    override fun values(): Collection<Any> = map.values()
    override fun isEmpty(): Boolean = map.isEmpty()
    override fun size(): Int = map.size()
    override fun equals(other: Any?): Boolean = map.equals(other)
    override fun hashCode(): Int = map.hashCode()
    override fun toString(): String = map.toString()
}

/** Returns an empty read-only map of specified type */
public fun emptyMap<K, V>(): Map<K, V> = EmptyMap as Map<K, V>

/**
 * Returns a new read-only map with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value.
 */
public fun mapOf<K, V>(vararg values: Pair<K, V>): Map<K, V> = if (values.size() == 0) emptyMap() else linkedMapOf(*values)

/** Returns an empty read-only map */
public fun mapOf<K, V>(): Map<K, V> = emptyMap()

/**
 * Returns a new [HashMap] with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value.
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt createUsingPairs
 */
public fun <K, V> hashMapOf(vararg values: Pair<K, V>): HashMap<K, V> {
    val answer = HashMap<K, V>(values.size())
    answer.putAll(*values)
    return answer
}

/**
 * Returns a new [LinkedHashMap] with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value.
 * This map preserves insertion order so iterating through the map's entries will be in the same order.
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt createLinkedMap
 */
public fun <K, V> linkedMapOf(vararg values: Pair<K, V>): LinkedHashMap<K, V> {
    val answer = LinkedHashMap<K, V>(values.size())
    answer.putAll(*values)
    return answer
}

/**
 * Returns the [Map] if its not null, or the empty [Map] otherwise.
 */
public fun <K,V> Map<K,V>?.orEmpty() : Map<K,V>
       = if (this != null) this else emptyMap()

/**
 * Checks if the map contains the given key. This method allows to use the `x in map` syntax for checking
 * whether an object is contained in the map.
 */
public fun <K,V> Map<K,V>.contains(key : K) : Boolean = containsKey(key)

/**
 * Allows to access the key of a map entry as a property. Equivalent to `getKey()`.
 */
public val <K, V> Map.Entry<K, V>.key: K
    get() = getKey()

/**
 * Allows to access the value of a map entry as a property. Equivalent to `getValue()`.
 */
public val <K, V> Map.Entry<K, V>.value: V
    get() = getValue()

/**
 * Returns the key of the map entry. This method allows to use multi-declarations when working with maps,
 * for example:
 * ```
 * for ((key, value) in map) {
 *     // do something with the key and the value
 * }
 * ```
 */
public fun <K, V> Map.Entry<K, V>.component1(): K {
    return getKey()
}

/**
 * Returns the value of the map entry. This method allows to use multi-declarations when working with maps,
 * for example:
 * ```
 * for ((key, value) in map) {
 *     // do something with the key and the value
 * }
 * ```
 */
public fun <K, V> Map.Entry<K, V>.component2(): V {
    return getValue()
}

/**
 * Converts entry to [Pair] with key being first component and value being second
 */
public fun <K, V> Map.Entry<K, V>.toPair(): Pair<K, V> {
    return Pair(getKey(), getValue())
}

/**
 * Returns the value for the given key, or the result of the [defaultValue] function if there was no entry for the given key.
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt getOrElse
 */
public inline fun <K, V> Map<K, V>.getOrElse(key: K, defaultValue: () -> V): V {
    if (containsKey(key)) {
        return get(key) as V
    } else {
        return defaultValue()
    }
}

/**
 * Returns the value for the given key. If the key is not found in the map, calls the [defaultValue] function,
 * puts its result into the map under the given key and returns it.
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt getOrPut
 */
public inline fun <K, V> MutableMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    if (containsKey(key)) {
        return get(key) as V
    } else {
        val answer = defaultValue()
        put(key, answer)
        return answer
    }
}

/**
 * Returns an [Iterator] over the entries in the [Map].
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt iterateWithProperties
 */
public fun <K, V> Map<K, V>.iterator(): Iterator<Map.Entry<K, V>> {
    val entrySet = entrySet()
    return entrySet.iterator()
}

/**
 * Populates the given `destination` [Map] with entries having the keys of this map and the values obtained
 * by applying the `transform` function to each entry in this [Map].
 */
public inline fun <K, V, R, C : MutableMap<K, R>> Map<K, V>.mapValuesTo(destination: C, transform: (Map.Entry<K, V>) -> R): C {
    for (e in this) {
        val newValue = transform(e)
        destination.put(e.key, newValue)
    }
    return destination
}

/**
 * Populates the given `destination` [Map] with entries having the keys obtained
 * by applying the `transform` function to each entry in this [Map] and the values of this map.
 */
public inline fun <K, V, R, C : MutableMap<R, V>> Map<K, V>.mapKeysTo(destination: C, transform: (Map.Entry<K, V>) -> R): C {
    for (e in this) {
        val newKey = transform(e)
        destination.put(newKey, e.value)
    }
    return destination
}

/**
 * Puts all the entries into this [MutableMap] with the first value in the pair being the key and the second the value
 */
public fun <K, V> MutableMap<K, V>.putAll(vararg values: Pair<K, V>): Unit {
    for ((key, value) in values) {
        put(key, value)
    }
}

/**
 * Puts all the entries into this [MutableMap] with the first value in the pair being the key and the second the value
 */
public fun <K, V> MutableMap<K, V>.putAll(values: Iterable<Pair<K,V>>): Unit {
    for ((key, value) in values) {
        put(key, value)
    }
}

/**
 * Returns a new map with entries having the keys of this map and the values obtained by applying the `transform`
 * function to each entry in this [Map].
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt mapValues
 */
public inline fun <K, V, R> Map<K, V>.mapValues(transform: (Map.Entry<K, V>) -> R): Map<K, R> {
    return mapValuesTo(LinkedHashMap<K, R>(size()), transform)
}

/**
 * Returns a new Map with entries having the keys obtained by applying the `transform` function to each entry in this
 * [Map] and the values of this map.
 *
 * @includeFunctionBody ../../test/collections/MapTest.kt mapKeys
 */
public inline fun <K, V, R> Map<K, V>.mapKeys(transform: (Map.Entry<K, V>) -> R): Map<R, V> {
    return mapKeysTo(LinkedHashMap<R, V>(size()), transform)
}

/**
 * Returns a map containing all key-value pairs with keys matching the given [predicate].
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
 * Returns a map containing all key-value pairs with values matching the given [predicate].
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
 * Appends all entries matching the given [predicate] into the given [destination] mutable map.
 *
 * @return the destination map.
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
 * Returns a map containing all key-value pairs matching the given [predicate].
 */
public inline fun <K, V> Map<K, V>.filter(predicate: (Map.Entry<K, V>) -> Boolean): Map<K, V> {
    return filterTo(LinkedHashMap<K, V>(), predicate)
}

/**
 * Appends all entries not matching the given [predicate] into the given [destination].
 *
 * @return the destination map.
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
 * Returns a map containing all key-value pairs not matching the given [predicate].
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
 * Returns a new map containing all key-value pairs from the given collection of pairs.
 */
public fun <K, V> Iterable<Pair<K, V>>.toMap(): Map<K, V> {
    val result = LinkedHashMap<K, V>()
    for (element in this) {
        result.put(element.first, element.second)
    }
    return result
}

/**
 * Converts this [Map] to a [LinkedHashMap], maintaining the insertion order of elements added to that map afterwards.
 */
public fun <K, V> Map<K, V>.toLinkedMap(): MutableMap<K, V> = LinkedHashMap(this)
