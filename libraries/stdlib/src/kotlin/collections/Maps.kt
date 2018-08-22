/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MapsKt")

package kotlin.collections

import kotlin.contracts.*

private object EmptyMap : Map<Any?, Nothing>, Serializable {
    private const val serialVersionUID: Long = 8246714829545688274

    override fun equals(other: Any?): Boolean = other is Map<*, *> && other.isEmpty()
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

/**
 * Returns an empty read-only map of specified type.
 *
 * The returned map is serializable (JVM).
 * @sample samples.collections.Maps.Instantiation.emptyReadOnlyMap
 */
public fun <K, V> emptyMap(): Map<K, V> = @Suppress("UNCHECKED_CAST") (EmptyMap as Map<K, V>)

/**
 * Returns a new read-only map with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 *
 * The returned map is serializable (JVM).
 *
 * @sample samples.collections.Maps.Instantiation.mapFromPairs
 */
public fun <K, V> mapOf(vararg pairs: Pair<K, V>): Map<K, V> =
    if (pairs.size > 0) pairs.toMap(LinkedHashMap(mapCapacity(pairs.size))) else emptyMap()

/**
 * Returns an empty read-only map.
 *
 * The returned map is serializable (JVM).
 * @sample samples.collections.Maps.Instantiation.emptyReadOnlyMap
 */
@kotlin.internal.InlineOnly
public inline fun <K, V> mapOf(): Map<K, V> = emptyMap()

/**
 * Returns an empty new [MutableMap].
 *
 * The returned map preserves the entry iteration order.
 * @sample samples.collections.Maps.Instantiation.emptyMutableMap
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <K, V> mutableMapOf(): MutableMap<K, V> = LinkedHashMap()

/**
 * Returns a new [MutableMap] with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 *
 * @sample samples.collections.Maps.Instantiation.mutableMapFromPairs
 * @sample samples.collections.Maps.Instantiation.emptyMutableMap
 */
public fun <K, V> mutableMapOf(vararg pairs: Pair<K, V>): MutableMap<K, V> =
    LinkedHashMap<K, V>(mapCapacity(pairs.size)).apply { putAll(pairs) }

/**
 * Returns an empty new [HashMap].
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <K, V> hashMapOf(): HashMap<K, V> = HashMap<K, V>()

/**
 * Returns a new [HashMap] with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * @sample samples.collections.Maps.Instantiation.hashMapFromPairs
 */
public fun <K, V> hashMapOf(vararg pairs: Pair<K, V>): HashMap<K, V> = HashMap<K, V>(mapCapacity(pairs.size)).apply { putAll(pairs) }

/**
 * Returns an empty new [LinkedHashMap].
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <K, V> linkedMapOf(): LinkedHashMap<K, V> = LinkedHashMap<K, V>()

/**
 * Returns a new [LinkedHashMap] with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 *
 * @sample samples.collections.Maps.Instantiation.linkedMapFromPairs
 */
public fun <K, V> linkedMapOf(vararg pairs: Pair<K, V>): LinkedHashMap<K, V> = pairs.toMap(LinkedHashMap(mapCapacity(pairs.size)))

/**
 * Calculate the initial capacity of a map, based on Guava's com.google.common.collect.Maps approach. This is equivalent
 * to the Collection constructor for HashSet, (c.size()/.75f) + 1, but provides further optimisations for very small or
 * very large sizes, allows support non-collection classes, and provides consistency for all map based class construction.
 */
@PublishedApi
internal fun mapCapacity(expectedSize: Int): Int {
    if (expectedSize < 3) {
        return expectedSize + 1
    }
    if (expectedSize < INT_MAX_POWER_OF_TWO) {
        return expectedSize + expectedSize / 3
    }
    return Int.MAX_VALUE // any large value
}

private const val INT_MAX_POWER_OF_TWO: Int = Int.MAX_VALUE / 2 + 1

/** Returns `true` if this map is not empty. */
@kotlin.internal.InlineOnly
public inline fun <K, V> Map<out K, V>.isNotEmpty(): Boolean = !isEmpty()

/**
 * Returns `true` if this nullable map is either null or empty.
 * @sample samples.collections.Maps.Usage.mapIsNullOrEmpty
 */
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
public inline fun <K, V> Map<out K, V>?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }

    return this == null || isEmpty()
}

/**
 * Returns the [Map] if its not `null`, or the empty [Map] otherwise.
 */
@kotlin.internal.InlineOnly
public inline fun <K, V> Map<K, V>?.orEmpty(): Map<K, V> = this ?: emptyMap()

/**
 * Returns this map if it's not empty
 * or the result of calling [defaultValue] function if the map is empty.
 *
 * @sample samples.collections.Maps.Usage.mapIfEmpty
 */
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
public inline fun <M, R> M.ifEmpty(defaultValue: () -> R): R where M : Map<*, *>, M : R =
    if (isEmpty()) defaultValue() else this

/**
 * Checks if the map contains the given key.
 *
 * This method allows to use the `x in map` syntax for checking whether an object is contained in the map.
 */
@kotlin.internal.InlineOnly
public inline operator fun <@kotlin.internal.OnlyInputTypes K, V> Map<out K, V>.contains(key: K): Boolean = containsKey(key)

/**
 * Returns the value corresponding to the given [key], or `null` if such a key is not present in the map.
 */
@kotlin.internal.InlineOnly
public inline operator fun <@kotlin.internal.OnlyInputTypes K, V> Map<out K, V>.get(key: K): V? =
    @Suppress("UNCHECKED_CAST") (this as Map<K, V>).get(key)

/**
 * Allows to use the index operator for storing values in a mutable map.
 */
@kotlin.internal.InlineOnly
public inline operator fun <K, V> MutableMap<K, V>.set(key: K, value: V): Unit {
    put(key, value)
}

/**
 * Returns `true` if the map contains the specified [key].
 *
 * Allows to overcome type-safety restriction of `containsKey` that requires to pass a key of type `K`.
 */
@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes K> Map<out K, *>.containsKey(key: K): Boolean =
    @Suppress("UNCHECKED_CAST") (this as Map<K, *>).containsKey(key)

/**
 * Returns `true` if the map maps one or more keys to the specified [value].
 *
 * Allows to overcome type-safety restriction of `containsValue` that requires to pass a value of type `V`.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // false warning, extension takes precedence in some cases
@kotlin.internal.InlineOnly
public inline fun <K, @kotlin.internal.OnlyInputTypes V> Map<K, V>.containsValue(value: V): Boolean = this.containsValue(value)


/**
 * Removes the specified key and its corresponding value from this map.
 *
 * @return the previous value associated with the key, or `null` if the key was not present in the map.

 * Allows to overcome type-safety restriction of `remove` that requires to pass a key of type `K`.
 */
@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes K, V> MutableMap<out K, V>.remove(key: K): V? =
    @Suppress("UNCHECKED_CAST") (this as MutableMap<K, V>).remove(key)

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
 *
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
 * @sample samples.collections.Maps.Usage.getOrElse
 */
@kotlin.internal.InlineOnly
public inline fun <K, V> Map<K, V>.getOrElse(key: K, defaultValue: () -> V): V = get(key) ?: defaultValue()


internal inline fun <K, V> Map<K, V>.getOrElseNullable(key: K, defaultValue: () -> V): V {
    val value = get(key)
    if (value == null && !containsKey(key)) {
        return defaultValue()
    } else {
        @Suppress("UNCHECKED_CAST")
        return value as V
    }
}

/**
 * Returns the value for the given [key] or throws an exception if there is no such key in the map.
 *
 * If the map was created by [withDefault], resorts to its `defaultValue` provider function
 * instead of throwing an exception.
 *
 * @throws NoSuchElementException when the map doesn't contain a value for the specified key and
 * no implicit default value was provided for that map.
 */
@SinceKotlin("1.1")
public fun <K, V> Map<K, V>.getValue(key: K): V = getOrImplicitDefault(key)

/**
 * Returns the value for the given key. If the key is not found in the map, calls the [defaultValue] function,
 * puts its result into the map under the given key and returns it.
 *
 * @sample samples.collections.Maps.Usage.getOrPut
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
 * @sample samples.collections.Maps.Usage.forOverEntries
 */
@kotlin.internal.InlineOnly
public inline operator fun <K, V> Map<out K, V>.iterator(): Iterator<Map.Entry<K, V>> = entries.iterator()

/**
 * Returns a [MutableIterator] over the mutable entries in the [MutableMap].
 *
 */
@kotlin.jvm.JvmName("mutableIterator")
@kotlin.internal.InlineOnly
public inline operator fun <K, V> MutableMap<K, V>.iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = entries.iterator()

/**
 * Populates the given [destination] map with entries having the keys of this map and the values obtained
 * by applying the [transform] function to each entry in this [Map].
 */
public inline fun <K, V, R, M : MutableMap<in K, in R>> Map<out K, V>.mapValuesTo(destination: M, transform: (Map.Entry<K, V>) -> R): M {
    return entries.associateByTo(destination, { it.key }, transform)
}

/**
 * Populates the given [destination] map with entries having the keys obtained
 * by applying the [transform] function to each entry in this [Map] and the values of this map.
 *
 * In case if any two entries are mapped to the equal keys, the value of the latter one will overwrite
 * the value associated with the former one.
 */
public inline fun <K, V, R, M : MutableMap<in R, in V>> Map<out K, V>.mapKeysTo(destination: M, transform: (Map.Entry<K, V>) -> R): M {
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
public fun <K, V> MutableMap<in K, in V>.putAll(pairs: Iterable<Pair<K, V>>): Unit {
    for ((key, value) in pairs) {
        put(key, value)
    }
}

/**
 * Puts all the elements of the given sequence into this [MutableMap] with the first component in the pair being the key and the second the value.
 */
public fun <K, V> MutableMap<in K, in V>.putAll(pairs: Sequence<Pair<K, V>>): Unit {
    for ((key, value) in pairs) {
        put(key, value)
    }
}

/**
 * Returns a new map with entries having the keys of this map and the values obtained by applying the [transform]
 * function to each entry in this [Map].
 *
 * The returned map preserves the entry iteration order of the original map.
 *
 * @sample samples.collections.Maps.Transformations.mapValues
 */
public inline fun <K, V, R> Map<out K, V>.mapValues(transform: (Map.Entry<K, V>) -> R): Map<K, R> {
    return mapValuesTo(LinkedHashMap<K, R>(mapCapacity(size)), transform) // .optimizeReadOnlyMap()
}

/**
 * Returns a new Map with entries having the keys obtained by applying the [transform] function to each entry in this
 * [Map] and the values of this map.
 *
 * In case if any two entries are mapped to the equal keys, the value of the latter one will overwrite
 * the value associated with the former one.
 *
 * The returned map preserves the entry iteration order of the original map.
 *
 * @sample samples.collections.Maps.Transformations.mapKeys
 */
public inline fun <K, V, R> Map<out K, V>.mapKeys(transform: (Map.Entry<K, V>) -> R): Map<R, V> {
    return mapKeysTo(LinkedHashMap<R, V>(mapCapacity(size)), transform) // .optimizeReadOnlyMap()
}

/**
 * Returns a map containing all key-value pairs with keys matching the given [predicate].
 *
 * The returned map preserves the entry iteration order of the original map.
 * @sample samples.collections.Maps.Filtering.filterKeys
 */
public inline fun <K, V> Map<out K, V>.filterKeys(predicate: (K) -> Boolean): Map<K, V> {
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
 *
 * The returned map preserves the entry iteration order of the original map.
 *  @sample samples.collections.Maps.Filtering.filterValues
 */
public inline fun <K, V> Map<out K, V>.filterValues(predicate: (V) -> Boolean): Map<K, V> {
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
 * @sample samples.collections.Maps.Filtering.filterTo
 */
public inline fun <K, V, M : MutableMap<in K, in V>> Map<out K, V>.filterTo(destination: M, predicate: (Map.Entry<K, V>) -> Boolean): M {
    for (element in this) {
        if (predicate(element)) {
            destination.put(element.key, element.value)
        }
    }
    return destination
}

/**
 * Returns a new map containing all key-value pairs matching the given [predicate].
 *
 * The returned map preserves the entry iteration order of the original map.
 * @sample samples.collections.Maps.Filtering.filter
 */
public inline fun <K, V> Map<out K, V>.filter(predicate: (Map.Entry<K, V>) -> Boolean): Map<K, V> {
    return filterTo(LinkedHashMap<K, V>(), predicate)
}

/**
 * Appends all entries not matching the given [predicate] into the given [destination].
 *
 * @return the destination map.
 * @sample samples.collections.Maps.Filtering.filterNotTo
 */
public inline fun <K, V, M : MutableMap<in K, in V>> Map<out K, V>.filterNotTo(destination: M, predicate: (Map.Entry<K, V>) -> Boolean): M {
    for (element in this) {
        if (!predicate(element)) {
            destination.put(element.key, element.value)
        }
    }
    return destination
}

/**
 * Returns a new map containing all key-value pairs not matching the given [predicate].
 *
 * The returned map preserves the entry iteration order of the original map.
 * @sample samples.collections.Maps.Filtering.filterNot
 */
public inline fun <K, V> Map<out K, V>.filterNot(predicate: (Map.Entry<K, V>) -> Boolean): Map<K, V> {
    return filterNotTo(LinkedHashMap<K, V>(), predicate)
}

/**
 * Returns a new map containing all key-value pairs from the given collection of pairs.
 *
 * The returned map preserves the entry iteration order of the original collection.
 */
public fun <K, V> Iterable<Pair<K, V>>.toMap(): Map<K, V> {
    if (this is Collection) {
        return when (size) {
            0 -> emptyMap()
            1 -> mapOf(if (this is List) this[0] else iterator().next())
            else -> toMap(LinkedHashMap<K, V>(mapCapacity(size)))
        }
    }
    return toMap(LinkedHashMap<K, V>()).optimizeReadOnlyMap()
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs from the given collection of pairs.
 */
public fun <K, V, M : MutableMap<in K, in V>> Iterable<Pair<K, V>>.toMap(destination: M): M =
    destination.apply { putAll(this@toMap) }

/**
 * Returns a new map containing all key-value pairs from the given array of pairs.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public fun <K, V> Array<out Pair<K, V>>.toMap(): Map<K, V> = when (size) {
    0 -> emptyMap()
    1 -> mapOf(this[0])
    else -> toMap(LinkedHashMap<K, V>(mapCapacity(size)))
}

/**
 *  Populates and returns the [destination] mutable map with key-value pairs from the given array of pairs.
 */
public fun <K, V, M : MutableMap<in K, in V>> Array<out Pair<K, V>>.toMap(destination: M): M =
    destination.apply { putAll(this@toMap) }

/**
 * Returns a new map containing all key-value pairs from the given sequence of pairs.
 *
 * The returned map preserves the entry iteration order of the original sequence.
 */
public fun <K, V> Sequence<Pair<K, V>>.toMap(): Map<K, V> = toMap(LinkedHashMap<K, V>()).optimizeReadOnlyMap()

/**
 * Populates and returns the [destination] mutable map with key-value pairs from the given sequence of pairs.
 */
public fun <K, V, M : MutableMap<in K, in V>> Sequence<Pair<K, V>>.toMap(destination: M): M =
    destination.apply { putAll(this@toMap) }

/**
 * Returns a new read-only map containing all key-value pairs from the original map.
 *
 * The returned map preserves the entry iteration order of the original map.
 */
@SinceKotlin("1.1")
public fun <K, V> Map<out K, V>.toMap(): Map<K, V> = when (size) {
    0 -> emptyMap()
    1 -> toSingletonMap()
    else -> toMutableMap()
}

/**
 * Returns a new mutable map containing all key-value pairs from the original map.
 *
 * The returned map preserves the entry iteration order of the original map.
 */
@SinceKotlin("1.1")
public fun <K, V> Map<out K, V>.toMutableMap(): MutableMap<K, V> = LinkedHashMap(this)

/**
 * Populates and returns the [destination] mutable map with key-value pairs from the given map.
 */
@SinceKotlin("1.1")
public fun <K, V, M : MutableMap<in K, in V>> Map<out K, V>.toMap(destination: M): M =
    destination.apply { putAll(this@toMap) }

/**
 * Creates a new read-only map by replacing or adding an entry to this map from a given key-value [pair].
 *
 * The returned map preserves the entry iteration order of the original map.
 * The [pair] is iterated in the end if it has a unique key.
 */
public operator fun <K, V> Map<out K, V>.plus(pair: Pair<K, V>): Map<K, V> =
    if (this.isEmpty()) mapOf(pair) else LinkedHashMap(this).apply { put(pair.first, pair.second) }

/**
 * Creates a new read-only map by replacing or adding entries to this map from a given collection of key-value [pairs].
 *
 * The returned map preserves the entry iteration order of the original map.
 * Those [pairs] with unique keys are iterated in the end in the order of [pairs] collection.
 */
public operator fun <K, V> Map<out K, V>.plus(pairs: Iterable<Pair<K, V>>): Map<K, V> =
    if (this.isEmpty()) pairs.toMap() else LinkedHashMap(this).apply { putAll(pairs) }

/**
 * Creates a new read-only map by replacing or adding entries to this map from a given array of key-value [pairs].
 *
 * The returned map preserves the entry iteration order of the original map.
 * Those [pairs] with unique keys are iterated in the end in the order of [pairs] array.
 */
public operator fun <K, V> Map<out K, V>.plus(pairs: Array<out Pair<K, V>>): Map<K, V> =
    if (this.isEmpty()) pairs.toMap() else LinkedHashMap(this).apply { putAll(pairs) }

/**
 * Creates a new read-only map by replacing or adding entries to this map from a given sequence of key-value [pairs].
 *
 * The returned map preserves the entry iteration order of the original map.
 * Those [pairs] with unique keys are iterated in the end in the order of [pairs] sequence.
 */
public operator fun <K, V> Map<out K, V>.plus(pairs: Sequence<Pair<K, V>>): Map<K, V> =
    LinkedHashMap(this).apply { putAll(pairs) }.optimizeReadOnlyMap()

/**
 * Creates a new read-only map by replacing or adding entries to this map from another [map].
 *
 * The returned map preserves the entry iteration order of the original map.
 * Those entries of another [map] that are missing in this map are iterated in the end in the order of that [map].
 */
public operator fun <K, V> Map<out K, V>.plus(map: Map<out K, V>): Map<K, V> =
    LinkedHashMap(this).apply { putAll(map) }


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

/**
 * Returns a map containing all entries of the original map except the entry with the given [key].
 *
 * The returned map preserves the entry iteration order of the original map.
 */
@SinceKotlin("1.1")
public operator fun <K, V> Map<out K, V>.minus(key: K): Map<K, V> =
    this.toMutableMap().apply { minusAssign(key) }.optimizeReadOnlyMap()

/**
 * Returns a map containing all entries of the original map except those entries
 * the keys of which are contained in the given [keys] collection.
 *
 * The returned map preserves the entry iteration order of the original map.
 */
@SinceKotlin("1.1")
public operator fun <K, V> Map<out K, V>.minus(keys: Iterable<K>): Map<K, V> =
    this.toMutableMap().apply { minusAssign(keys) }.optimizeReadOnlyMap()

/**
 * Returns a map containing all entries of the original map except those entries
 * the keys of which are contained in the given [keys] array.
 *
 * The returned map preserves the entry iteration order of the original map.
 */
@SinceKotlin("1.1")
public operator fun <K, V> Map<out K, V>.minus(keys: Array<out K>): Map<K, V> =
    this.toMutableMap().apply { minusAssign(keys) }.optimizeReadOnlyMap()

/**
 * Returns a map containing all entries of the original map except those entries
 * the keys of which are contained in the given [keys] sequence.
 *
 * The returned map preserves the entry iteration order of the original map.
 */
@SinceKotlin("1.1")
public operator fun <K, V> Map<out K, V>.minus(keys: Sequence<K>): Map<K, V> =
    this.toMutableMap().apply { minusAssign(keys) }.optimizeReadOnlyMap()

/**
 * Removes the entry with the given [key] from this mutable map.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline operator fun <K, V> MutableMap<K, V>.minusAssign(key: K) {
    remove(key)
}

/**
 * Removes all entries the keys of which are contained in the given [keys] collection from this mutable map.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline operator fun <K, V> MutableMap<K, V>.minusAssign(keys: Iterable<K>) {
    this.keys.removeAll(keys)
}

/**
 * Removes all entries the keys of which are contained in the given [keys] array from this mutable map.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline operator fun <K, V> MutableMap<K, V>.minusAssign(keys: Array<out K>) {
    this.keys.removeAll(keys)
}

/**
 * Removes all entries from the keys of which are contained in the given [keys] sequence from this mutable map.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline operator fun <K, V> MutableMap<K, V>.minusAssign(keys: Sequence<K>) {
    this.keys.removeAll(keys)
}


// do not expose for now @PublishedApi
internal fun <K, V> Map<K, V>.optimizeReadOnlyMap() = when (size) {
    0 -> emptyMap()
    1 -> toSingletonMapOrSelf()
    else -> this
}
