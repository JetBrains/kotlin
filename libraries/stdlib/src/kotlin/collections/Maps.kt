@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MapsKt")

package kotlin

import java.io.Serializable
import java.util.*

private object EmptyMap : Map<Any?, Nothing>, Serializable {
    override fun equals(other: Any?): Boolean = other is Map<*,*> && other.isEmpty()
    override fun hashCode(): Int = 0
    override fun toString(): String = "{}"

    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true

    override fun containsKey(key: Any?): Boolean = false
    override fun containsValue(value: Nothing): Boolean = false
    override fun get(key: Any?): Nothing? = null
    override val entries: Set<Map.Entry<Any?, Nothing>> get() = EmptySet
    override val keys: Set<Any?> get() = EmptySet
    override val values: Collection<Nothing> get() = EmptyList

    private fun readResolve(): Any = EmptyMap
}

/** Returns an empty read-only map of specified type. The returned map is serializable (JVM). */
public fun <K, V> emptyMap(): Map<K, V> = EmptyMap as Map<K, V>

/**
 * Returns a new read-only map with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value. If multiple pairs have
 * the same key, the resulting map will contain the value from the last of those pairs.
 *
 * The returned map is serializable (JVM).
 */
public fun <K, V> mapOf(vararg values: Pair<K, V>): Map<K, V> = if (values.size() > 0) linkedMapOf(*values) else emptyMap()

/** Returns an empty read-only map. The returned map is serializable (JVM). */
public fun <K, V> mapOf(): Map<K, V> = emptyMap()

/**
 * Returns an immutable map, mapping only the specified key to the
 * specified value.  The returned map is serializable.
 */
@JvmVersion
public fun <K, V> mapOf(keyValuePair: Pair<K, V>): Map<K, V> = Collections.singletonMap(keyValuePair.first, keyValuePair.second)

/**
 * Returns a new [HashMap] with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * @sample test.collections.MapTest.createUsingPairs
 */
public fun <K, V> hashMapOf(vararg values: Pair<K, V>): HashMap<K, V> {
    val answer = HashMap<K, V>(mapCapacity(values.size()))
    answer.putAll(*values)
    return answer
}

/**
 * Returns a new [LinkedHashMap] with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 * This map preserves insertion order so iterating through the map's entries will be in the same order.
 *
 * @sample test.collections.MapTest.createLinkedMap
 */
public fun <K, V> linkedMapOf(vararg values: Pair<K, V>): LinkedHashMap<K, V> {
    val answer = LinkedHashMap<K, V>(mapCapacity(values.size()))
    answer.putAll(*values)
    return answer
}

/**
 * Calculate the initial capacity of a map, based on Guava's com.google.common.collect.Maps approach. This is equivalent
 * to the Collection constructor for HashSet, (c.size()/.75f) + 1, but provides further optimisations for very small or
 * very large sizes, allows support non-collection classes, and provides consistency for all map based class construction.
 */

private val INT_MAX_POWER_OF_TWO: Int = Int.MAX_VALUE / 2 + 1

internal fun mapCapacity(expectedSize: Int): Int {
    if (expectedSize < 3) {
        return expectedSize + 1
    }
    if (expectedSize < INT_MAX_POWER_OF_TWO) {
        return expectedSize + expectedSize / 3
    }
    return Int.MAX_VALUE // any large value
}

/** Returns `true` if this map is not empty. */
public fun <K, V> Map<K, V>.isNotEmpty(): Boolean = !isEmpty()

/**
 * Returns the [Map] if its not `null`, or the empty [Map] otherwise.
 */
public fun <K,V> Map<K,V>?.orEmpty() : Map<K,V> = this ?: emptyMap()

/**
 * Checks if the map contains the given key. This method allows to use the `x in map` syntax for checking
 * whether an object is contained in the map.
 */
public operator fun <K,V> Map<K,V>.contains(key : K) : Boolean = containsKey(key)

/**
 * Returns the value corresponding to the given [key], or `null` if such a key is not present in the map.

 * Allows to overcome type-safety restriction of `get` that requires to pass a key of type `Key`.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <K, V> Map<K, V>.getRaw(key: Any?): V? = (this as Map<Any?, V>).get(key)

/**
 * Returns `true` if the map contains the specified [key].
 *
 * Allows to overcome type-safety restriction of `containsKey` that requires to pass a key of type `Key`.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <K> Map<K, *>.containsKeyRaw(key: Any?): Boolean = (this as Map<Any?, *>).containsKey(key)

/**
 * Returns `true` if the map maps one or more keys to the specified [value].
 *
 * Allows to overcome type-safety restriction of `containsValue` that requires to pass a value of type `V`.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <K> Map<K, *>.containsValueRaw(value: Any?): Boolean = (this as Map<K, Any?>).containsValue(value)

/**
 * Returns the key component of the map entry.
 *
 * This method allows to use multi-declarations when working with maps, for example:
 * ```
 * for ((key, value) in map) {
 *     // do something with the key and the value
 * }
 * ```
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun <K, V> Map.Entry<K, V>.component1(): K = key

/**
 * Returns the value component of the map entry.
 * This method allows to use multi-declarations when working with maps, for example:
 * ```
 * for ((key, value) in map) {
 *     // do something with the key and the value
 * }
 * ```
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun <K, V> Map.Entry<K, V>.component2(): V = value

/**
 * Converts entry to [Pair] with key being first component and value being second.
 */
public fun <K, V> Map.Entry<K, V>.toPair(): Pair<K, V> = Pair(key, value)

/**
 * Returns the value for the given key, or the result of the [defaultValue] function if there was no entry for the given key.
 *
 * @sample test.collections.MapTest.getOrElse
 */
public inline fun <K, V> Map<K, V>.getOrElse(key: K, defaultValue: () -> V): V {
    val value = get(key)
    if (value == null && !containsKey(key)) {
        return defaultValue()
    } else {
        return value as V
    }
}

/**
 * Returns the value for the given key. If the key is not found in the map, calls the [defaultValue] function,
 * puts its result into the map under the given key and returns it.
 *
 * @sample test.collections.MapTest.getOrPut
 */
public inline fun <K, V> MutableMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    val value = get(key)
    if (value == null && !containsKey(key)) {
        val answer = defaultValue()
        put(key, answer)
        return answer
    } else {
        return value as V
    }
}

/**
 * Returns an [Iterator] over the entries in the [Map].
 *
 * @sample test.collections.MapTest.iterateWithProperties
 */
public operator fun <K, V> Map<K, V>.iterator(): Iterator<Map.Entry<K, V>> = entries.iterator()

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
 * Puts all the given [values] into this [MutableMap] with the first component in the pair being the key and the second the value.
 */
public fun <K, V> MutableMap<K, V>.putAll(vararg values: Pair<K, V>): Unit {
    for ((key, value) in values) {
        put(key, value)
    }
}

/**
 * Puts all the elements of the given collection into this [MutableMap] with the first component in the pair being the key and the second the value.
 */
public fun <K, V> MutableMap<K, V>.putAll(values: Iterable<Pair<K,V>>): Unit {
    for ((key, value) in values) {
        put(key, value)
    }
}

/**
 * Puts all the elements of the given sequence into this [MutableMap] with the first component in the pair being the key and the second the value.
 */
public fun <K, V> MutableMap<K, V>.putAll(values: Sequence<Pair<K,V>>): Unit {
    for ((key, value) in values) {
        put(key, value)
    }
}

/**
 * Returns a new map with entries having the keys of this map and the values obtained by applying the `transform`
 * function to each entry in this [Map].
 *
 * @sample test.collections.MapTest.mapValues
 */
public inline fun <K, V, R> Map<K, V>.mapValues(transform: (Map.Entry<K, V>) -> R): Map<K, R> {
    return mapValuesTo(LinkedHashMap<K, R>(size), transform)
}

/**
 * Returns a new Map with entries having the keys obtained by applying the `transform` function to each entry in this
 * [Map] and the values of this map.
 *
 * @sample test.collections.MapTest.mapKeys
 */
public inline fun <K, V, R> Map<K, V>.mapKeys(transform: (Map.Entry<K, V>) -> R): Map<R, V> {
    return mapKeysTo(LinkedHashMap<R, V>(size), transform)
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
 * Appends all entries matching the given [predicate] into the mutable map given as [destination] parameter.
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
 * Returns a new map containing all key-value pairs matching the given [predicate].
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
 * Returns a new map containing all key-value pairs not matching the given [predicate].
 */
public inline fun <K, V> Map<K, V>.filterNot(predicate: (Map.Entry<K, V>) -> Boolean): Map<K, V> {
    return filterNotTo(LinkedHashMap<K, V>(), predicate)
}

/**
 * Returns a new map containing all key-value pairs from the given collection of pairs.
 */
public fun <K, V> Iterable<Pair<K, V>>.toMap(): Map<K, V> {
    val result = LinkedHashMap<K, V>(collectionSizeOrNull()?.let { mapCapacity(it) } ?: 16)
    for (element in this) {
        result.put(element.first, element.second)
    }
    return result
}

/**
 * Returns a new map containing all key-value pairs from the given array of pairs.
 */
public fun <K, V> Array<Pair<K, V>>.toMap(): Map<K, V> {
    val result = LinkedHashMap<K, V>(mapCapacity(size()))
    for (element in this) {
        result.put(element.first, element.second)
    }
    return result
}

/**
 * Returns a new map containing all key-value pairs from the given sequence of pairs.
 */

public fun <K, V> Sequence<Pair<K, V>>.toMap(): Map<K, V> {
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

/**
 * Creates a new read-only map by replacing or adding an entry to this map from a given key-value [pair].
 */
public operator fun <K, V> Map<K, V>.plus(pair: Pair<K, V>): Map<K, V> {
    val newMap = this.toLinkedMap()
    newMap.put(pair.first, pair.second)
    return newMap
}

/**
 * Creates a new read-only map by replacing or adding entries to this map from a given collection of key-value [pairs].
 */
public operator fun <K, V> Map<K, V>.plus(pairs: Iterable<Pair<K, V>>): Map<K, V> {
    val newMap = this.toLinkedMap()
    newMap.putAll(pairs)
    return newMap
}

/**
 * Creates a new read-only map by replacing or adding entries to this map from a given array of key-value [pairs].
 */
public operator fun <K, V> Map<K, V>.plus(pairs: Array<Pair<K, V>>): Map<K, V> {
    val newMap = this.toLinkedMap()
    newMap.putAll(*pairs)
    return newMap
}

/**
 * Creates a new read-only map by replacing or adding entries to this map from a given sequence of key-value [pairs].
 */
public operator fun <K, V> Map<K, V>.plus(pairs: Sequence<Pair<K, V>>): Map<K, V> {
    val newMap = this.toLinkedMap()
    newMap.putAll(pairs)
    return newMap
}

/**
 * Creates a new read-only map by replacing or adding entries to this map from another [map].
 */
public operator fun <K, V> Map<K, V>.plus(map: Map<K, V>): Map<K, V> {
    val newMap = this.toLinkedMap()
    newMap.putAll(map)
    return newMap
}

/**
 * Appends or replaces the given [pair] in this mutable map.
 */
public operator fun <K, V> MutableMap<K, V>.plusAssign(pair: Pair<K, V>) {
    put(pair.first, pair.second)
}

/**
 * Appends or replaces all pairs from the given collection of [pairs] in this mutable map.
 */
public operator fun <K, V> MutableMap<K, V>.plusAssign(pairs: Iterable<Pair<K, V>>) {
    putAll(pairs)
}

/**
 * Appends or replaces all pairs from the given array of [pairs] in this mutable map.
 */
public operator fun <K, V> MutableMap<K, V>.plusAssign(pairs: Array<Pair<K, V>>) {
    putAll(*pairs)
}

/**
 * Appends or replaces all pairs from the given sequence of [pairs] in this mutable map.
 */
public operator fun <K, V> MutableMap<K, V>.plusAssign(pairs: Sequence<Pair<K, V>>) {
    putAll(pairs)
}

/**
 * Appends or replaces all entries from the given [map] in this mutable map.
 */
public operator fun <K, V> MutableMap<K, V>.plusAssign(map: Map<K, V>) {
    putAll(map)
}

/**
 * Creates a new read-only map by removing a [key] from this map.
 */
public operator fun <K, V> Map<K, V>.minus(key: K): Map<K, V> {
    val result = LinkedHashMap<K, V>(this)
    result.minusAssign(key)
    return result
}

/**
 * Creates a new read-only map by removing a collection of [keys] from this map.
 */
public operator fun <K, V> Map<K, V>.minus(keys: Iterable<K>): Map<K, V> {
    val result = LinkedHashMap<K, V>(this)
    result.minusAssign(keys)
    return result
}

/**
 * Creates a new read-only map by removing a array of [keys] from this map.
 */
public operator fun <K, V> Map<K, V>.minus(keys: Array<K>): Map<K, V> {
    val result = LinkedHashMap<K, V>(this)
    result.minusAssign(keys)
    return result
}

/**
 * Creates a new read-only map by removing a sequence of [keys] from this map.
 */
public operator fun <K, V> Map<K, V>.minus(keys: Sequence<K>): Map<K, V> {
    val result = LinkedHashMap<K, V>(this)
    result.minusAssign(keys)
    return result
}

/**
 * Removes the given [key] from this mutable map.
 */
public operator fun <K, V> MutableMap<K, V>.minusAssign(key: K) {
    remove(key)
}

/**
 * Removes all the given [keys] from this mutable map.
 */
public operator fun <K, V> MutableMap<K, V>.minusAssign(keys: Iterable<K>) {
    for (key in keys) remove(key)
}

/**
 * Removes all the given [keys] from this mutable map.
 */
public operator fun <K, V> MutableMap<K, V>.minusAssign(keys: Array<K>) {
    for (key in keys) remove(key)
}

/**
 * Removes all the given [keys] from this mutable map.
 */
public operator fun <K, V> MutableMap<K, V>.minusAssign(keys: Sequence<K>) {
    for (key in keys) remove(key)
}
