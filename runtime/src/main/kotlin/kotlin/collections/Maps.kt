/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.collections

private object EmptyMap : Map<Any?, Nothing> {
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

/**
 * Returns an empty read-only map of specified type. The returned map is serializable (JVM).
 * @sample samples.collections.Maps.Instantiation.emptyReadOnlyMap
 */
@Suppress("UNCHECKED_CAST")
public fun <K, V> emptyMap(): Map<K, V> = EmptyMap as Map<K, V>

/**
 * Returns a new read-only map with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value. If multiple pairs have
 * the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 * The returned map is serializable (JVM).
 *
 * @sample samples.collections.Maps.Instantiation.mapFromPairs
 */
public fun <K, V> mapOf(vararg pairs: Pair<K, V>): Map<K, V> =
        if (pairs.size > 0) hashMapOf(*pairs) else emptyMap()

/**
 * Returns an empty read-only map. The returned map is serializable (JVM).
 * @sample samples.collections.Maps.Instantiation.emptyReadOnlyMap
 */
@kotlin.internal.InlineOnly
public inline fun <K, V> mapOf(): Map<K, V> = emptyMap()

// TODO: Add a singleton map class (see Kotlin JVM mapOf(Pair) implementation).
/**
 * Returns an immutable map, mapping only the specified key to the
 * specified value.  The returned map is serializable.
 * @sample samples.collections.Maps.Instantiation.mapFromPairs
 */
public fun <K, V> mapOf(pair: Pair<K, V>): Map<K, V> = hashMapOf(pair)

/**
 * Returns a new [MutableMap] with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value. If multiple pairs have
 * the same key, the resulting map will contain the value from the last of those pairs.
 * Entries of the map are iterated in the order they were specified.
 * @sample samples.collections.Maps.Instantiation.mutableMapFromPairs
 * @sample samples.collections.Maps.Instantiation.emptyMutableMap
 */
public fun <K, V> mutableMapOf(vararg pairs: Pair<K, V>): MutableMap<K, V>
        = HashMap<K, V>(mapCapacity(pairs.size)).apply { putAll(pairs) }

/**
 * Returns a new [HashMap] with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * @sample samples.collections.Maps.Instantiation.hashMapFromPairs
 */
public fun <K, V> hashMapOf(vararg pairs: Pair<K, V>): HashMap<K, V>
        = HashMap<K, V>(mapCapacity(pairs.size)).apply { putAll(pairs) }

/**
 * Returns a new [HashMap] with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value. If multiple pairs have
 * the same key, the resulting map will contain the value from the last of those pairs.
 * Entries of the map are iterated in the order they were specified.
 *
 * @sample samples.collections.Maps.Instantiation.linkedMapFromPairs
 */
public fun <K, V> linkedMapOf(vararg pairs: Pair<K, V>): LinkedHashMap<K, V>
        = LinkedHashMap<K, V>(mapCapacity(pairs.size)).apply { putAll(pairs) }

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
    if (expectedSize < 0x40000000 /* INT_MAX_POWER_OF_TWO */) {
        return expectedSize + expectedSize / 3
    }
    return 0x7fffffff // any large value
}

// Using global constant with dependency like that introduces weird init order dependencies.
// private const val INT_MAX_POWER_OF_TWO: Int = Int.MAX_VALUE / 2 + 1

/** Returns `true` if this map is not empty. */
@kotlin.internal.InlineOnly
public inline fun <K, V> Map<out K, V>.isNotEmpty(): Boolean = !isEmpty()

/**
 * Returns the [Map] if its not `null`, or the empty [Map] otherwise.
 */
@kotlin.internal.InlineOnly
public inline fun <K, V> Map<K, V>?.orEmpty() : Map<K, V> = this ?: emptyMap()

/**
 * Checks if the map contains the given key. This method allows to use the `x in map` syntax for checking
 * whether an object is contained in the map.
 */
@kotlin.internal.InlineOnly
public inline operator fun <@kotlin.internal.OnlyInputTypes K, V> Map<out K, V>.contains(key: K) : Boolean = containsKey(key)

/**
 * Allows to use the index operator for storing values in a mutable map.
 */
@kotlin.internal.InlineOnly
public inline operator fun <K, V> MutableMap<K, V>.set(key: K, value: V): Unit {
    put(key, value)
}

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
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
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
 * @sample samples.collections.Maps.Usage.getOrElse
 */
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
@kotlin.internal.InlineOnly
public inline operator fun <K, V> MutableMap<K, V>.iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = entries.iterator()

/**
 * Populates the given [destination] map with entries having the keys of this map and the values obtained
 * by applying the [transform] function to each entry in this [Map].
 */
@kotlin.internal.InlineOnly
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
@kotlin.internal.InlineOnly
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
 * Returns a new map with entries having the keys of this map and the values obtained by applying the [transform]
 * function to each entry in this [Map].
 *
 * The returned map preserves the entry iteration order of the original map.
 *
 * @sample samples.collections.Maps.Transforms.mapValues
 */
public inline fun <K, V, R> Map<out K, V>.mapValues(transform: (Map.Entry<K, V>) -> R): Map<K, R> {
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    return mapValuesTo(HashMap<K, R>(
            mapCapacity(size)),
            transform).optimizeReadOnlyMap()
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
 * @sample samples.collections.Maps.Transforms.mapKeys
 */
public inline fun <K, V, R> Map<out K, V>.mapKeys(transform: (Map.Entry<K, V>) -> R): Map<R, V> {
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    return mapKeysTo(HashMap<R, V>(
            mapCapacity(size)),
            transform).optimizeReadOnlyMap()
}

/**
 * Returns a map containing all key-value pairs with keys matching the given [predicate].
 *
 * The returned map preserves the entry iteration order of the original map.
 */
public inline fun <K, V> Map<out K, V>.filterKeys(predicate: (K) -> Boolean): Map<K, V> {
    val result = HashMap<K, V>()
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
 */
public inline fun <K, V> Map<out K, V>.filterValues(predicate: (V) -> Boolean): Map<K, V> {
    val result = HashMap<K, V>()
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
 */
public inline fun <K, V> Map<out K, V>.filter(predicate: (Map.Entry<K, V>) -> Boolean): Map<K, V> {
    return filterTo(HashMap<K, V>(), predicate)
}

/**
 * Appends all entries not matching the given [predicate] into the given [destination].
 *
 * @return the destination map.
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
 */
public inline fun <K, V> Map<out K, V>.filterNot(predicate: (Map.Entry<K, V>) -> Boolean): Map<K, V> {
    return filterNotTo(HashMap<K, V>(), predicate)
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
            else -> toMap(HashMap<K, V>(mapCapacity(size)))
        }
    }
    return toMap(HashMap<K, V>()).optimizeReadOnlyMap()
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs from the given collection of pairs.
 */
public fun <K, V, M : MutableMap<in K, in V>> Iterable<Pair<K, V>>.toMap(destination: M): M
        = destination.apply { putAll(this@toMap) }

/**
 * Returns a new map containing all key-value pairs from the given array of pairs.
 *
 * The returned map preserves the entry iteration order of the original array.
 */
public fun <K, V> Array<out Pair<K, V>>.toMap(): Map<K, V> = when(size) {
    0 -> emptyMap()
    1 -> mapOf(this[0])
    else -> toMap(HashMap<K, V>(mapCapacity(size)))
}

/**
 *  Populates and returns the [destination] mutable map with key-value pairs from the given array of pairs.
 */
public fun <K, V, M : MutableMap<in K, in V>> Array<out Pair<K, V>>.toMap(destination: M): M
        = destination.apply { putAll(this@toMap) }

/**
 * Returns a new map containing all key-value pairs from the given sequence of pairs.
 *
 * The returned map preserves the entry iteration order of the original sequence.
 */
public fun <K, V> Sequence<Pair<K, V>>.toMap(): Map<K, V> = toMap(HashMap<K, V>()).optimizeReadOnlyMap()

/**
 * Populates and returns the [destination] mutable map with key-value pairs from the given sequence of pairs.
 */
public fun <K, V, M : MutableMap<in K, in V>> Sequence<Pair<K, V>>.toMap(destination: M): M
        = destination.apply { putAll(this@toMap) }

/**
 * Returns a new read-only map containing all key-value pairs from the original map.
 *
 * The returned map preserves the entry iteration order of the original map.
 */
@SinceKotlin("1.1")
public fun <K, V> Map<out K, V>.toMap(): Map<K, V> = when (size) {
    0 -> emptyMap()
//    1 -> toSingletonMap()
    else -> toMutableMap()
}

/**
 * Returns a new mutable map containing all key-value pairs from the original map.
 *
 * The returned map preserves the entry iteration order of the original map.
 */
@SinceKotlin("1.1")
public fun <K, V> Map<out K, V>.toMutableMap(): MutableMap<K, V> = HashMap<K, V>(this)

/**
 * Populates and returns the [destination] mutable map with key-value pairs from the given map.
 */
public fun <K, V, M : MutableMap<in K, in V>> Map<out K, V>.toMap(destination: M): M
        = destination.apply { putAll(this@toMap) }

/**
 * Creates a new read-only map by replacing or adding an entry to this map from a given key-value [pair].
 *
 * The returned map preserves the entry iteration order of the original map.
 * The [pair] is iterated in the end if it has a unique key.
 */
public operator fun <K, V> Map<out K, V>.plus(pair: Pair<K, V>): Map<K, V>
        = if (this.isEmpty()) mapOf(pair) else HashMap<K, V>(this).apply { put(pair.first, pair.second) }

/**
 * Creates a new read-only map by replacing or adding entries to this map from a given collection of key-value [pairs].
 *
 * The returned map preserves the entry iteration order of the original map.
 * Those [pairs] with unique keys are iterated in the end in the order of [pairs] collection.
 */
public operator fun <K, V> Map<out K, V>.plus(pairs: Iterable<Pair<K, V>>): Map<K, V>
        = if (this.isEmpty()) pairs.toMap() else HashMap(this).apply { putAll(pairs) }

/**
 * Creates a new read-only map by replacing or adding entries to this map from a given array of key-value [pairs].
 *
 * The returned map preserves the entry iteration order of the original map.
 * Those [pairs] with unique keys are iterated in the end in the order of [pairs] array.
 */
public operator fun <K, V> Map<out K, V>.plus(pairs: Array<out Pair<K, V>>): Map<K, V>
        = if (this.isEmpty()) pairs.toMap() else HashMap(this).apply { putAll(pairs) }

/**
 * Creates a new read-only map by replacing or adding entries to this map from a given sequence of key-value [pairs].
 *
 * The returned map preserves the entry iteration order of the original map.
 * Those [pairs] with unique keys are iterated in the end in the order of [pairs] sequence.
 */
public operator fun <K, V> Map<out K, V>.plus(pairs: Sequence<Pair<K, V>>): Map<K, V>
        = HashMap(this).apply { putAll(pairs) }.optimizeReadOnlyMap()

/**
 * Creates a new read-only map by replacing or adding entries to this map from another [map].
 *
 * The returned map preserves the entry iteration order of the original map.
 * Those entries of another [map] that are missing in this map are iterated in the end in the order of that [map].
 */
public operator fun <K, V> Map<out K, V>.plus(map: Map<out K, V>): Map<K, V>
        = HashMap(this).apply { putAll(map) }

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

@kotlin.internal.InlineExposed
internal fun <K, V> Map<K, V>.optimizeReadOnlyMap() = when (size) {
    0 -> emptyMap()
//    1 -> toSingletonMapOrSelf()
    else -> this
}

// creates a singleton copy of map, if there is specialization available in target platform, otherwise returns itself
// internal inline fun <K, V> Map<K, V>.toSingletonMapOrSelf(): Map<K, V> = toSingletonMap()

// creates a singleton copy of map
//internal fun <K, V> Map<out K, V>.toSingletonMap(): Map<K, V>
//        = with (entries.iterator().next()) { java.util.Collections.singletonMap(key, value) }

// This is from generated _Maps.kt.
/**
 * Returns a [List] containing all key-value pairs.
 */
public fun <K, V> Map<out K, V>.toList(): List<Pair<K, V>> {
    if (size == 0)
        return emptyList()
    val iterator = entries.iterator()
    if (!iterator.hasNext())
        return emptyList()
    val first = iterator.next()
    if (!iterator.hasNext())
        return listOf(first.toPair())
    val result = ArrayList<Pair<K, V>>(size)
    result.add(first.toPair())
    do {
        result.add(iterator.next().toPair())
    } while (iterator.hasNext())
    return result
}

/**
 * Returns a single list of all elements yielded from results of [transform] function being invoked on each entry of original map.
 */
public inline fun <K, V, R> Map<out K, V>.flatMap(transform: (Map.Entry<K, V>) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Appends all elements yielded from results of [transform] function being invoked on each entry of original map, to the given [destination].
 */
public inline fun <K, V, R, C : MutableCollection<in R>> Map<out K, V>.flatMapTo(destination: C, transform: (Map.Entry<K, V>) -> Iterable<R>): C {
    for (element in this) {
        val list = transform(element)
        destination.addAll(list)
    }
    return destination
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each entry in the original map.
 */
public inline fun <K, V, R> Map<out K, V>.map(transform: (Map.Entry<K, V>) -> R): List<R> {
    return mapTo(ArrayList<R>(size), transform)
}

/**
 * Returns a list containing only the non-null results of applying the given [transform] function
 * to each entry in the original map.
 */
public inline fun <K, V, R : Any> Map<out K, V>.mapNotNull(transform: (Map.Entry<K, V>) -> R?): List<R> {
    return mapNotNullTo(ArrayList<R>(), transform)
}

/**
 * Applies the given [transform] function to each entry in the original map
 * and appends only the non-null results to the given [destination].
 */
public inline fun <K, V, R : Any, C : MutableCollection<in R>> Map<out K, V>.mapNotNullTo(destination: C, transform: (Map.Entry<K, V>) -> R?): C {
    forEach { element -> transform(element)?.let { destination.add(it) } }
    return destination
}

/**
 * Applies the given [transform] function to each entry of the original map
 * and appends the results to the given [destination].
 */
public inline fun <K, V, R, C : MutableCollection<in R>> Map<out K, V>.mapTo(destination: C, transform: (Map.Entry<K, V>) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

/**
 * Returns `true` if all entries match the given [predicate].
 */
public inline fun <K, V> Map<out K, V>.all(predicate: (Map.Entry<K, V>) -> Boolean): Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if map has at least one entry.
 */
public fun <K, V> Map<out K, V>.any(): Boolean {
    for (element in this) return true
    return false
}

/**
 * Returns `true` if at least one entry matches the given [predicate].
 */
public inline fun <K, V> Map<out K, V>.any(predicate: (Map.Entry<K, V>) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns the number of entries in this map.
 */
@kotlin.internal.InlineOnly
public inline fun <K, V> Map<out K, V>.count(): Int {
    return size
}

/**
 * Returns the number of entries matching the given [predicate].
 */
public inline fun <K, V> Map<out K, V>.count(predicate: (Map.Entry<K, V>) -> Boolean): Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Performs the given [action] on each entry.
 */
@kotlin.internal.HidesMembers
public inline fun <K, V> Map<out K, V>.forEach(action: (Map.Entry<K, V>) -> Unit): Unit {
    for (element in this) action(element)
}

/**
 * Returns the first entry yielding the largest value of the given function or `null` if there are no entries.
 */
@kotlin.internal.InlineOnly
public inline fun <K, V, R : Comparable<R>> Map<out K, V>.maxBy(selector: (Map.Entry<K, V>) -> R): Map.Entry<K, V>? {
    return entries.maxBy(selector)
}

/**
 * Returns the first entry having the largest value according to the provided [comparator] or `null` if there are no entries.
 */
@kotlin.internal.InlineOnly
public inline fun <K, V> Map<out K, V>.maxWith(comparator: Comparator<in Map.Entry<K, V>>): Map.Entry<K, V>? {
    return entries.maxWith(comparator)
}

/**
 * Returns the first entry yielding the smallest value of the given function or `null` if there are no entries.
 */
public inline fun <K, V, R : Comparable<R>> Map<out K, V>.minBy(selector: (Map.Entry<K, V>) -> R): Map.Entry<K, V>? {
    return entries.minBy(selector)
}

/**
 * Returns the first entry having the smallest value according to the provided [comparator] or `null` if there are no entries.
 */
public fun <K, V> Map<out K, V>.minWith(comparator: Comparator<in Map.Entry<K, V>>): Map.Entry<K, V>? {
    return entries.minWith(comparator)
}

/**
 * Returns `true` if the map has no entries.
 */
public fun <K, V> Map<out K, V>.none(): Boolean {
    for (element in this) return false
    return true
}

/**
 * Returns `true` if no entries match the given [predicate].
 */
public inline fun <K, V> Map<out K, V>.none(predicate: (Map.Entry<K, V>) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return false
    return true
}

/**
 * Performs the given [action] on each entry and returns the map itself afterwards.
 */
@SinceKotlin("1.1")
public inline fun <K, V, M : Map<out K, V>> M.onEach(action: (Map.Entry<K, V>) -> Unit): M {
    return apply { for (element in this) action(element) }
}

/**
 * Creates an [Iterable] instance that wraps the original map returning its entries when being iterated.
 */
@kotlin.internal.InlineOnly
public inline fun <K, V> Map<out K, V>.asIterable(): Iterable<Map.Entry<K, V>> {
    return entries
}

/**
 * Creates a [Sequence] instance that wraps the original map returning its entries when being iterated.
 */
public fun <K, V> Map<out K, V>.asSequence(): Sequence<Map.Entry<K, V>> {
    return entries.asSequence()
}

