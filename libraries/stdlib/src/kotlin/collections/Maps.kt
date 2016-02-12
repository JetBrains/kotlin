@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MapsKt")

package kotlin.collections

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
public fun <K, V> emptyMap(): Map<K, V> = @Suppress("CAST_NEVER_SUCCEEDS") (EmptyMap as Map<K, V>)

/**
 * Returns a new read-only map with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value. If multiple pairs have
 * the same key, the resulting map will contain the value from the last of those pairs.
 *
 * The returned map is serializable (JVM).
 */
public fun <K, V> mapOf(vararg pairs: Pair<K, V>): Map<K, V> = if (pairs.size > 0) linkedMapOf(*pairs) else emptyMap()

/** Returns an empty read-only map. The returned map is serializable (JVM). */
@kotlin.internal.InlineOnly
public inline fun <K, V> mapOf(): Map<K, V> = emptyMap()

/**
 * Returns an immutable map, mapping only the specified key to the
 * specified value.  The returned map is serializable.
 */
@JvmVersion
public fun <K, V> mapOf(pair: Pair<K, V>): Map<K, V> = Collections.singletonMap(pair.first, pair.second)

/**
 * Returns a new [MutableMap] with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 * This map preserves insertion order so iterating through the map's entries will be in the same order.
 */
public fun <K, V> mutableMapOf(vararg pairs: Pair<K, V>): MutableMap<K, V>
        = LinkedHashMap<K, V>(mapCapacity(pairs.size)).apply { putAll(pairs) }

/**
 * Returns a new [HashMap] with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * @sample test.collections.MapTest.createUsingPairs
 */
public fun <K, V> hashMapOf(vararg pairs: Pair<K, V>): HashMap<K, V>
        = HashMap<K, V>(mapCapacity(pairs.size)).apply { putAll(pairs) }


/**
 * Returns a new [LinkedHashMap] with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 * This map preserves insertion order so iterating through the map's entries will be in the same order.
 *
 * @sample test.collections.MapTest.createLinkedMap
 */
public fun <K, V> linkedMapOf(vararg pairs: Pair<K, V>): LinkedHashMap<K, V>
        = LinkedHashMap<K, V>(mapCapacity(pairs.size)).apply { putAll(pairs) }

/**
 * Calculate the initial capacity of a map, based on Guava's com.google.common.collect.Maps approach. This is equivalent
 * to the Collection constructor for HashSet, (c.size()/.75f) + 1, but provides further optimisations for very small or
 * very large sizes, allows support non-collection classes, and provides consistency for all map based class construction.
 */

private val INT_MAX_POWER_OF_TWO: Int = Int.MAX_VALUE / 2 + 1

@kotlin.internal.InlineExposed
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
@kotlin.internal.InlineOnly
public inline fun <K, V> Map<K, V>.isNotEmpty(): Boolean = !isEmpty()

/**
 * Returns the [Map] if its not `null`, or the empty [Map] otherwise.
 */
@kotlin.internal.InlineOnly
public inline fun <K,V> Map<K,V>?.orEmpty() : Map<K,V> = this ?: emptyMap()

/**
 * Checks if the map contains the given key. This method allows to use the `x in map` syntax for checking
 * whether an object is contained in the map.
 */
@kotlin.internal.InlineOnly
public inline operator fun <@kotlin.internal.OnlyInputTypes K, V> Map<out K, V>.contains(key: K) : Boolean = containsKey(key)

/**
 * Returns the value corresponding to the given [key], or `null` if such a key is not present in the map.
 */
@kotlin.internal.InlineOnly
public inline operator fun <@kotlin.internal.OnlyInputTypes K, V> Map<out K, V>.get(key: K): V?
        = @Suppress("UNCHECKED_CAST") (this as Map<K, V>).get(key)

/**
 * Returns `true` if the map contains the specified [key].
 *
 * Allows to overcome type-safety restriction of `containsKey` that requires to pass a key of type `K`.
 */
@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes K> Map<out K, *>.containsKey(key: K): Boolean
        = @Suppress("UNCHECKED_CAST") (this as Map<K, *>).containsKey(key)

/**
 * Returns `true` if the map maps one or more keys to the specified [value].
 *
 * Allows to overcome type-safety restriction of `containsValue` that requires to pass a value of type `V`.
 */
@kotlin.internal.InlineOnly
public inline fun <K, @kotlin.internal.OnlyInputTypes V> Map<K, V>.containsValue(value: V): Boolean = this.containsValue(value)


/**
 * Removes the specified key and its corresponding value from this map.
 *
 * @return the previous value associated with the key, or `null` if the key was not present in the map.

 * Allows to overcome type-safety restriction of `remove` that requires to pass a key of type `K`.
 */
@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes K, V> MutableMap<out K, V>.remove(key: K): V?
        = @Suppress("UNCHECKED_CAST") (this as MutableMap<K, V>).remove(key)

/**
 * Returns the key component of the map entry.
 *
 * This method allows to use destructuring declarations when working with maps, for example:
 * ```
 * for ((key, value) in map) {
 *     // do something with the key and the value
 * }
 * ```
 */
@kotlin.internal.InlineOnly
public inline operator fun <K, V> Map.Entry<K, V>.component1(): K = key

/**
 * Returns the value component of the map entry.
 * This method allows to use destructuring declarations when working with maps, for example:
 * ```
 * for ((key, value) in map) {
 *     // do something with the key and the value
 * }
 * ```
 */
@kotlin.internal.InlineOnly
public inline operator fun <K, V> Map.Entry<K, V>.component2(): V = value

/**
 * Converts entry to [Pair] with key being first component and value being second.
 */
@kotlin.internal.InlineOnly
public inline fun <K, V> Map.Entry<K, V>.toPair(): Pair<K, V> = Pair(key, value)

/**
 * Returns the value for the given key, or the result of the [defaultValue] function if there was no entry for the given key.
 *
 * @sample test.collections.MapTest.getOrElse
 */
@kotlin.internal.InlineOnly
public inline fun <K, V> Map<K, V>.getOrElse(key: K, defaultValue: () -> V): V = get(key) ?: defaultValue()


internal inline fun <K, V> Map<K, V>.getOrElseNullable(key: K, defaultValue: () -> V): V {
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
    return if (value == null) {
        val answer = defaultValue()
        put(key, answer)
        answer
    } else {
        value
    }
}

/**
 * Returns an [Iterator] over the entries in the [Map].
 *
 * @sample test.collections.MapTest.iterateWithProperties
 */
@kotlin.internal.InlineOnly
public inline operator fun <K, V> Map<K, V>.iterator(): Iterator<Map.Entry<K, V>> = entries.iterator()

/**
 * Returns a [MutableIterator] over the mutable entries in the [MutableMap].
 *
 */
@JvmVersion
@JvmName("mutableIterator")
@kotlin.internal.InlineOnly
public inline operator fun <K, V> MutableMap<K, V>.iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = entries.iterator()

/**
 * Populates the given `destination` [Map] with entries having the keys of this map and the values obtained
 * by applying the `transform` function to each entry in this [Map].
 */
public inline fun <K, V, R, C : MutableMap<in K, in R>> Map<K, V>.mapValuesTo(destination: C, transform: (Map.Entry<K, V>) -> R): C {
    return entries.associateByTo(destination, { it.key }, transform)
}

/**
 * Populates the given `destination` [Map] with entries having the keys obtained
 * by applying the `transform` function to each entry in this [Map] and the values of this map.
 */
public inline fun <K, V, R, C : MutableMap<in R, in V>> Map<K, V>.mapKeysTo(destination: C, transform: (Map.Entry<K, V>) -> R): C {
    return entries.associateByTo(destination, transform, { it.value })
}

/**
 * Puts all the given [pairs] into this [MutableMap] with the first component in the pair being the key and the second the value.
 */
public fun <K, V> MutableMap<in K, in V>.putAll(pairs: Array<out Pair<K, V>>): Unit {
    for ((key, value) in pairs) {
        put(key, value)
    }
}

/**
 * Puts all the elements of the given collection into this [MutableMap] with the first component in the pair being the key and the second the value.
 */
public fun <K, V> MutableMap<in K, in V>.putAll(pairs: Iterable<Pair<K,V>>): Unit {
    for ((key, value) in pairs) {
        put(key, value)
    }
}

/**
 * Puts all the elements of the given sequence into this [MutableMap] with the first component in the pair being the key and the second the value.
 */
public fun <K, V> MutableMap<in K, in V>.putAll(pairs: Sequence<Pair<K,V>>): Unit {
    for ((key, value) in pairs) {
        put(key, value)
    }
}

/**
 * Returns a new map with entries having the keys of this map and the values obtained by applying the `transform`
 * function to each entry in this [Map].
 *
 * @sample test.collections.MapTest.mapValues
 */
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun <K, V, R> Map<K, V>.mapValues(transform: (Map.Entry<K, V>) -> R): Map<K, R> {
    return mapValuesTo(LinkedHashMap<K, R>(mapCapacity(size)), transform)
}

/**
 * Returns a new Map with entries having the keys obtained by applying the `transform` function to each entry in this
 * [Map] and the values of this map.
 *
 * @sample test.collections.MapTest.mapKeys
 */
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun <K, V, R> Map<K, V>.mapKeys(transform: (Map.Entry<K, V>) -> R): Map<R, V> {
    return mapKeysTo(LinkedHashMap<R, V>(mapCapacity(size)), transform)
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
public inline fun <K, V, C : MutableMap<in K, in V>> Map<K, V>.filterTo(destination: C, predicate: (Map.Entry<K, V>) -> Boolean): C {
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
public inline fun <K, V, C : MutableMap<in K, in V>> Map<K, V>.filterNotTo(destination: C, predicate: (Map.Entry<K, V>) -> Boolean): C {
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
public fun <K, V> Iterable<Pair<K, V>>.toMap(): Map<K, V> = toMap(LinkedHashMap<K, V>(collectionSizeOrNull()?.let { mapCapacity(it) } ?: 16))

/**
 * Populates and returns the [destination] mutable map with key-value pairs from the given collection of pairs.
 */
public fun <K, V, M : MutableMap<in K, in V>> Iterable<Pair<K, V>>.toMap(destination: M): M
        = destination.apply { putAll(this@toMap) }

/**
 * Returns a new map containing all key-value pairs from the given array of pairs.
 */
public fun <K, V> Array<out Pair<K, V>>.toMap(): Map<K, V> = toMap(LinkedHashMap<K, V>(mapCapacity(size)))

/**
 *  Populates and returns the [destination] mutable map with key-value pairs from the given array of pairs.
 */
public fun <K, V, M : MutableMap<in K, in V>> Array<out Pair<K, V>>.toMap(destination: M): M
        = destination.apply { putAll(this@toMap) }

/**
 * Returns a new map containing all key-value pairs from the given sequence of pairs.
 */

public fun <K, V> Sequence<Pair<K, V>>.toMap(): Map<K, V> = toMap(LinkedHashMap<K, V>())

/**
 * Populates and returns the [destination] mutable map with key-value pairs from the given sequence of pairs.
 */

public fun <K, V, M : MutableMap<in K, in V>> Sequence<Pair<K, V>>.toMap(destination: M): M
        = destination.apply { putAll(this@toMap) }

/**
 * Creates a new read-only map by replacing or adding an entry to this map from a given key-value [pair].
 */
public operator fun <K, V> Map<K, V>.plus(pair: Pair<K, V>): Map<K, V>
        = LinkedHashMap(this).apply { put(pair.first, pair.second) }

/**
 * Creates a new read-only map by replacing or adding entries to this map from a given collection of key-value [pairs].
 */
public operator fun <K, V> Map<K, V>.plus(pairs: Iterable<Pair<K, V>>): Map<K, V>
        = LinkedHashMap(this).apply { putAll(pairs) }

/**
 * Creates a new read-only map by replacing or adding entries to this map from a given array of key-value [pairs].
 */
public operator fun <K, V> Map<K, V>.plus(pairs: Array<out Pair<K, V>>): Map<K, V>
        = LinkedHashMap(this).apply { putAll(pairs) }

/**
 * Creates a new read-only map by replacing or adding entries to this map from a given sequence of key-value [pairs].
 */
public operator fun <K, V> Map<K, V>.plus(pairs: Sequence<Pair<K, V>>): Map<K, V>
        = LinkedHashMap(this).apply { putAll(pairs) }

/**
 * Creates a new read-only map by replacing or adding entries to this map from another [map].
 */
public operator fun <K, V> Map<K, V>.plus(map: Map<K, V>): Map<K, V>
        = LinkedHashMap(this).apply { putAll(map) }


/**
 * Appends or replaces the given [pair] in this mutable map.
 */
@kotlin.internal.InlineOnly
public inline operator fun <K, V> MutableMap<in K, in V>.plusAssign(pair: Pair<K, V>) {
    put(pair.first, pair.second)
}

/**
 * Appends or replaces all pairs from the given collection of [pairs] in this mutable map.
 */
@kotlin.internal.InlineOnly
public inline operator fun <K, V> MutableMap<in K, in V>.plusAssign(pairs: Iterable<Pair<K, V>>) {
    putAll(pairs)
}

/**
 * Appends or replaces all pairs from the given array of [pairs] in this mutable map.
 */
@kotlin.internal.InlineOnly
public inline operator fun <K, V> MutableMap<in K, in V>.plusAssign(pairs: Array<out Pair<K, V>>) {
    putAll(pairs)
}

/**
 * Appends or replaces all pairs from the given sequence of [pairs] in this mutable map.
 */
@kotlin.internal.InlineOnly
public inline operator fun <K, V> MutableMap<in K, in V>.plusAssign(pairs: Sequence<Pair<K, V>>) {
    putAll(pairs)
}

/**
 * Appends or replaces all entries from the given [map] in this mutable map.
 */
@kotlin.internal.InlineOnly
public inline operator fun <K, V> MutableMap<in K, in V>.plusAssign(map: Map<K, V>) {
    putAll(map)
}
