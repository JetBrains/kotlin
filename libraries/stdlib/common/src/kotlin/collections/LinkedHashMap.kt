/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * A hash table implementation of [MutableMap] that maintains insertion order.
 *
 * This class stores key-value pairs using a hash table data structure that provides fast lookups
 * based on keys, while also maintaining the order in which entries were inserted.
 * It fully implements the [MutableMap] contract, providing all standard map operations
 * including insertion, removal, and lookup of values by key.
 *
 * ## Null keys and values
 *
 * [LinkedHashMap] accepts `null` as a key. Since keys are unique, at most one entry with a `null` key
 * can exist in the map. [LinkedHashMap] also accepts `null` as a value, and multiple entries can have
 * `null` values.
 *
 * ## Key's hash code and equality contracts
 *
 * [LinkedHashMap] relies on the [Any.hashCode] and [Any.equals] functions of keys to organize and locate entries.
 * Keys are considered equal if their [Any.equals] function returns `true`, and keys that are equal must
 * have the same [Any.hashCode] value. Violating this contract can lead to incorrect behavior.
 *
 * The [Any.hashCode] and [Any.equals] functions should be consistent and immutable during the lifetime
 * of the key objects. Modifying a key object in a way that changes its hash code or equality
 * after it has been used as a key in a [LinkedHashMap] may lead to the entry becoming unreachable.
 *
 * ## Performance characteristics
 *
 * The performance characteristics below assume that the [Any.hashCode] function of keys distributes
 * them uniformly across the hash table, minimizing collisions. A poor hash function that causes
 * many collisions can degrade performance.
 *
 * [LinkedHashMap] provides efficient implementation for common operations:
 *
 * - **Lookup** ([get], [containsKey]): O(1) time
 * - **Insertion and removal** ([put], [remove]): O(1) time
 * - **Value search** ([containsValue]): O(n) time, requires scanning all entries
 * - **Iteration** ([entries], [keys], [values]): O(n) time
 *
 * ## Iteration order
 *
 * [LinkedHashMap] maintains a predictable iteration order for its keys, values, and entries.
 * Entries are iterated in the order they were inserted into the map, from oldest to newest.
 * This insertion order is preserved even when the map is rehashed (when entries are added or removed
 * and the internal capacity is adjusted).
 *
 * Note that the insertion order is not affected if a key is _re-inserted_ into the map.
 * A key `k` is re-inserted into the map when `put(k, v)` is called and the map already contains
 * an entry with key `k`.
 *
 * If predictable iteration order is not required, consider using [HashMap], which may have
 * slightly better performance characteristics.
 *
 * ## Usage guidelines
 *
 * [LinkedHashMap] uses an internal data structure with a finite *capacity* - the maximum number of entries
 * it can store before needing to grow. As entries are added, the map tracks its *load factor*, which is
 * the ratio of the number of entries to the current capacity. When this ratio exceeds a certain threshold,
 * the map automatically increases its capacity and performs *rehashing* - rebuilding the internal data
 * structure to redistribute entries. Rehashing is a relatively expensive operation that temporarily impacts
 * performance. When creating a [LinkedHashMap], you can optionally provide values for the initial capacity and
 * load factor threshold. Note that these parameters are just hints for the implementation and can be ignored.
 *
 * To optimize performance and memory usage:
 *
 * - If the number of entries is known in advance, use the constructor with initial capacity
 *   to avoid multiple rehashing operations as the map grows.
 * - Choose an appropriate load factor when creating the map. A lower load factor reduces collision
 *   probability but uses more memory, while a higher load factor saves memory but may increase
 *   lookup time. The default load factor typically provides a good balance.
 * - Ensure key objects have well-distributed [Any.hashCode] implementations to minimize collisions
 *   and maintain good performance.
 * - Prefer [putAll] over multiple individual [put] calls when adding multiple entries.
 *
 * ## Thread safety
 *
 * [LinkedHashMap] is not thread-safe. If multiple threads access an instance concurrently and at least
 * one thread modifies it, external synchronization is required.
 *
 * @param K the type of map keys. The map is invariant in its key type.
 * @param V the type of map values. The mutable map is invariant in its value type.
 */
public expect class LinkedHashMap<K, V> : MutableMap<K, V> {
    /**
     * Creates a new empty [LinkedHashMap].
     */
    public constructor()

    /**
     * Creates a new empty [LinkedHashMap] with the specified initial capacity.
     *
     * Capacity is the maximum number of entries the map is able to store in the current internal data structure.
     * When the map gets full by a certain default load factor, its capacity is expanded,
     * which usually leads to rebuild of the internal data structure.
     *
     * @param initialCapacity the initial capacity of the created map.
     *   Note that the argument is just a hint for the implementation and can be ignored.
     *
     * @throws IllegalArgumentException if [initialCapacity] is negative.
     */
    public constructor(initialCapacity: Int)

    /**
     * Creates a new empty [LinkedHashMap] with the specified initial capacity and load factor.
     *
     * Capacity is the maximum number of entries the map is able to store in the current internal data structure.
     * Load factor is the measure of how full the map is allowed to get in relation to
     * its capacity before the capacity is expanded, which usually leads to rebuild of the internal data structure.
     *
     * @param initialCapacity the initial capacity of the created map.
     *   Note that the argument is just a hint for the implementation and can be ignored.
     * @param loadFactor the load factor of the created map.
     *   Note that the argument is just a hint for the implementation and can be ignored.
     *
     * @throws IllegalArgumentException if [initialCapacity] is negative or [loadFactor] is non-positive.
     */
    public constructor(initialCapacity: Int, loadFactor: Float)

    /**
     * Creates a new [LinkedHashMap] filled with the contents of the specified [original] map.
     *
     * The iteration order of entries in the created map is the same as in the [original] map.
     */
    public constructor(original: Map<out K, V>)

    // From Map

    override val size: Int
    override fun isEmpty(): Boolean
    override fun containsKey(key: K): Boolean
    override fun containsValue(value: V): Boolean
    override fun get(key: K): V?

    // From MutableMap

    @IgnorableReturnValue
    override fun put(key: K, value: V): V?

    @IgnorableReturnValue
    override fun remove(key: K): V?
    override fun putAll(from: Map<out K, V>)
    override fun clear()
    override val keys: MutableSet<K>
    override val values: MutableCollection<V>
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
}
