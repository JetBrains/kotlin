/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * A hash table implementation of [MutableMap].
 *
 * This class stores key-value pairs using a hash table data structure that provides fast lookups
 * based on keys. It fully implements the [MutableMap] contract, providing all standard map operations
 * including insertion, removal, and lookup of entries by key.
 *
 * ## Hash and equality contract
 *
 * [HashMap] relies on the [hashCode] and [equals] functions of keys to organize and locate entries.
 * Keys are considered equal if their [equals] function returns `true`, and keys that are equal must
 * have the same [hashCode] value. Violating this contract can lead to incorrect behavior.
 *
 * The [hashCode] and [equals] functions should be consistent and immutable during the lifetime
 * of the key objects. Modifying a key object in a way that changes its hash code or equality
 * after it has been used as a key in a [HashMap] may lead to the entry becoming unreachable.
 *
 * ## Performance characteristics
 *
 * [HashMap] provides efficient implementation for common operations:
 *
 * - **Lookup** ([get], [containsKey]): O(1) average case, O(n) worst case. Performance depends
 *   on the quality of the [hashCode] function. A good hash function distributes keys uniformly,
 *   minimizing collisions and maintaining constant-time performance.
 * - **Insertion and removal** ([put], [remove]): O(1) average case, O(n) worst case
 * - **Value search** ([containsValue]): O(n) linear time, requires scanning all entries
 * - **Iteration** ([entries], [keys], [values]): O(n + capacity) time, where capacity is the current
 *   internal table size. Iteration time depends on both the number of entries and the table capacity.
 *
 * Note: On the JS target, these time-complexity guarantees may not hold due to the underlying
 * JavaScript engine implementation.
 *
 * ## Usage guidelines
 *
 * To optimize performance and memory usage:
 *
 * - If the number of entries is known in advance, use the constructor with initial capacity
 *   to avoid multiple rehashing operations as the map grows.
 * - Choose an appropriate load factor when creating the map. A lower load factor reduces collision
 *   probability but uses more memory, while a higher load factor saves memory but may increase
 *   lookup time. The default load factor typically provides a good balance.
 * - Ensure key objects have well-distributed [hashCode] implementations to minimize collisions
 *   and maintain good performance.
 * - Prefer [putAll] over multiple individual [put] calls when adding multiple entries.
 *
 * ## Thread safety
 *
 * [HashMap] is not thread-safe. If multiple threads access an instance concurrently and at least
 * one thread modifies it, external synchronization is required.
 *
 * @param K the type of map keys.
 * @param V the type of map values.
 */
public expect class HashMap<K, V> : MutableMap<K, V> {
    /**
     * Creates a new empty [HashMap].
     */
    public constructor()

    /**
     * Creates a new empty [HashMap] with the specified initial capacity.
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
     * Creates a new empty [HashMap] with the specified initial capacity and load factor.
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
     * Creates a new [HashMap] filled with the contents of the specified [original] map.
     */
    public constructor(original: Map<out K, V>)

    // From Map

    override val size: Int
    override fun isEmpty(): Boolean
    override fun containsKey(key: K): Boolean
    override fun containsValue(value: V): Boolean
    override operator fun get(key: K): V?

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
