/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * A collection that holds pairs of objects (keys and values) and supports retrieving the value corresponding to each key,
 * checking if a collection holds a particular key or a value. Maps also allow iterating over keys, values or key-value pairs (entries).
 * Complex operations are built upon this functionality and provided in form of [kotlin.collections] extension functions.
 *
 * Map keys are unique; the map holds only one value for each key. In contrast, the same value can be associated with several unique keys.
 *
 * It is implementation-specific how [Map] defines key's uniqueness. If not stated otherwise, [Map] implementations are usually
 * distinguishing elements using [Any.equals]. However, it is not the only way to distinguish elements, and some implementations may use
 * referential equality or compare elements by some of their properties. It is recommended to explicitly specify how a class
 * implementing [Map] distinguish elements.
 *
 * It is also implementation-specific how [Map] handles `null` keys and values: some [Map] implementations may support them, while
 * other may not. It is recommended to explicitly define key/value nullability policy when implementing [Map].
 *
 * As with [Collection], implementing [Any.toString], [Any.equals] and [Any.hashCode] is not enforced,
 * but [Map] implementations should override these functions and provide implementations such that:
 * - [Map.toString] should return a string containing string representation of contained key-value pairs in iteration order.
 * - [Map.equals] should consider two maps equal if and only if they contain the same keys and values associated with these keys
 *   are equal. Unlike some other `equals` implementations, [Map.equals] should consider two maps equal even
 *   if they are instances of different classes; the only requirement here is that both maps have to implement [Map] interface.
 * - [Map.hashCode] should be computed as a sum of [Entry] hash codes, and entry's hash code should be computed as exclusive or (XOR) of
 *   hash codes corresponding to a key and a value:
 *   ```kotlin
 *   var hashCode: Int = 0
 *   for ((k, v) in entries) hashCode += k.hashCode() ^ v.hashCode()
 *   ```
 *
 * Functions in this interface support only read-only access to the map; read-write access is supported through
 * the [MutableMap] interface.
 *
 * @param K the type of map keys. The map is invariant in its key type, as it
 *          can accept a key as a parameter (of [containsKey] for example) and return it in a [keys] set.
 * @param V the type of map values. The map is covariant in its value type.
 */
public actual interface Map<K, out V> {
    // Query Operations
    /**
     * Returns the number of key/value pairs in the map.
     *
     * If a map contains more than [Int.MAX_VALUE] elements, the value of this property is unspecified.
     * For implementations allowing to have more than [Int.MAX_VALUE] elements,
     * it is recommended to explicitly document behavior of this property.
     *
     * @sample samples.collections.Maps.CoreApi.size
     */
    public actual val size: Int

    /**
     * Returns `true` if the map is empty (contains no elements), `false` otherwise.
     *
     * @sample samples.collections.Maps.CoreApi.isEmpty
     */
    public actual fun isEmpty(): Boolean

    /**
     * Returns `true` if the map contains the specified [key].
     *
     * @sample samples.collections.Maps.CoreApi.containsKey
     */
    public actual fun containsKey(key: K): Boolean

    /**
     * Returns `true` if the map maps one or more keys to the specified [value].
     *
     * @sample samples.collections.Maps.CoreApi.containsValue
     */
    public actual fun containsValue(value: @UnsafeVariance V): Boolean

    /**
     * Returns the value corresponding to the given [key], or `null` if such a key is not present in the map.
     *
     * Note that for maps supporting `null` values,
     * the returned `null` value associated with the [key] is indistinguishable from the missing [key],
     * so [containsKey] should be used to check if the map actually contains the [key].
     *
     * @sample samples.collections.Maps.CoreApi.get
     */
    public actual operator fun get(key: K): V?

    // Views
    /**
     * Returns a read-only [Set] of all keys in this map.
     *
     * @sample samples.collections.Maps.CoreApi.keySet
     */
    public actual val keys: Set<K>

    /**
     * Returns a read-only [Collection] of all values in this map. Note that this collection may contain duplicate values.
     *
     * @sample samples.collections.Maps.CoreApi.valueSet
     */
    public actual val values: Collection<V>

    /**
     * Returns a read-only [Set] of all key/value pairs in this map.
     *
     * @sample samples.collections.Maps.CoreApi.entrySet
     */
    public actual val entries: Set<Map.Entry<K, V>>

    /**
     * Represents a key/value pair held by a [Map].
     *
     * Map entries are not supposed to be stored separately or used long after they are obtained.
     * The behavior of an entry is unspecified if the backing map has been modified after the entry was obtained.
     */
    public actual interface Entry<out K, out V> {
        /**
         * Returns the key of this key/value pair.
         */
        public actual val key: K

        /**
         * Returns the value of this key/value pair.
         */
        public actual val value: V
    }
}

/**
 * A collection that holds pairs of objects (keys and values) and supports retrieving
 * the value corresponding to each key, as well as adding new, removing or updating existing pairs.
 *
 * Map keys are unique; the map holds only one value for each key. In contrast, the same value can be associated with several unique keys.
 *
 * If a particular use case does not require map's modification, a read-only counterpart, [Map] could be used instead.
 *
 * [MutableMap] extends [Map] contact with functions allowing to add, remove and update mapping between keys and values.
 *
 * Unlike [Map], [keys], [values] and [entries] collections are all mutable, and changes in them update the map.
 *
 * Until stated otherwise, [MutableMap] implementations are not thread-safe and their modification without
 * explicit synchronization may result in data corruption, loss, and runtime errors.
 *
 * @param K the type of map keys. The map is invariant in its key type.
 * @param V the type of map values. The mutable map is invariant in its value type.
 */
public actual interface MutableMap<K, V> : Map<K, V> {
    // Modification Operations
    /**
     * Associates the specified [value] with the specified [key] in the map.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     *
     * @sample samples.collections.Maps.CoreApi.put
     */
    public actual fun put(key: K, value: V): V?

    /**
     * Removes the specified key and its corresponding value from this map.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     *
     * @sample samples.collections.Maps.CoreApi.remove
     */
    public actual fun remove(key: K): V?

    // Bulk Modification Operations
    /**
     * Updates this map with key/value pairs from the specified map [from].
     *
     * @sample samples.collections.Maps.CoreApi.putAll
     */
    public actual fun putAll(from: Map<out K, V>): Unit

    /**
     * Removes all elements from this map.
     *
     * @sample samples.collections.Maps.CoreApi.clear
     */
    public actual fun clear(): Unit

    // Views
    /**
     * Returns a [MutableSet] of all keys in this map.
     *
     * @sample samples.collections.Maps.CoreApi.keySetMutable
     */
    actual override val keys: MutableSet<K>

    /**
     * Returns a [MutableCollection] of all values in this map. Note that this collection may contain duplicate values.
     *
     * @sample samples.collections.Maps.CoreApi.valueSetMutable
     */
    actual override val values: MutableCollection<V>

    /**
     * Returns a [MutableSet] of all key/value pairs in this map.
     *
     * @sample samples.collections.Maps.CoreApi.entrySetMutable
     */
    actual override val entries: MutableSet<MutableMap.MutableEntry<K, V>>

    /**
     * Represents a key/value pair held by a [MutableMap].
     *
     * Map entries are not supposed to be stored separately or used long after they are obtained.
     * The behavior of an entry is unspecified if the backing map has been modified after the entry was obtained.
     */
    public actual interface MutableEntry<K, V> : Map.Entry<K, V> {
        /**
         * Changes the value associated with the key of this entry.
         *
         * @return the previous value corresponding to the key.
         */
        public actual fun setValue(newValue: V): V
    }
}
