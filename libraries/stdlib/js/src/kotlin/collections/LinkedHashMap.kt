/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Based on GWT LinkedHashMap
 * Copyright 2008 Google Inc.
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
 * it can store before needing to grow. When the map becomes full, the map automatically increases its capacity
 * and performs *rehashing* - rebuilding the internal data structure to redistribute entries. Rehashing is a
 * relatively expensive operation that temporarily impacts performance. When creating a [LinkedHashMap], you can
 * optionally provide an initial capacity value, which will be used to size the internal data structure,
 * potentially avoiding rehashing operations as the map grows.
 *
 * To optimize performance and memory usage:
 *
 * - If the number of entries is known in advance, use the constructor with initial capacity
 *   to avoid multiple rehashing operations as the map grows.
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
public actual open class LinkedHashMap<K, V> : HashMap<K, V>, MutableMap<K, V> {
    /**
     * Creates a new empty [LinkedHashMap].
     */
    public actual constructor() : super()

    /**
     * Creates a new empty [LinkedHashMap] with the specified initial capacity.
     *
     * Capacity is the maximum number of entries the map is able to store in current internal data structure.
     * When the map gets full by a certain default load factor, its capacity is expanded,
     * which usually leads to rebuild of the internal data structure.
     *
     * @param initialCapacity the initial capacity of the created map.
     *   Note that the argument is just a hint for the implementation and can be ignored.
     *
     * @throws IllegalArgumentException if [initialCapacity] is negative.
     */
    public actual constructor(initialCapacity: Int) : super(initialCapacity)

    /**
     * Creates a new empty [LinkedHashMap] with the specified initial capacity and load factor.
     *
     * Capacity is the maximum number of entries the map is able to store in current internal data structure.
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
    public actual constructor(initialCapacity: Int, loadFactor: Float) : super(initialCapacity, loadFactor)

    /**
     * Creates a new [LinkedHashMap] filled with the contents of the specified [original] map.
     *
     * The iteration order of entries in the created map is the same as in the [original] map.
     */
    public actual constructor(original: Map<out K, V>) : super(original)

    internal constructor(internalMap: InternalMap<K, V>) : super(internalMap)

    private object EmptyHolder {
        val value = LinkedHashMap(InternalHashMap<Nothing, Nothing>(0).also { it.build() })
    }

    @PublishedApi
    internal fun build(): Map<K, V> {
        internalMap.build()
        return if (size > 0) this else EmptyHolder.value.unsafeCast<Map<K, V>>()
    }

    override fun checkIsMutable() = internalMap.checkIsMutable()
}

/**
 * Constructs the specialized implementation of [LinkedHashMap] with [String] keys,
 * which stores the keys as properties of JS object without hashing them.
 */
public fun <V> linkedStringMapOf(vararg pairs: Pair<String, V>): LinkedHashMap<String, V> {
    return LinkedHashMap<String, V>(InternalStringLinkedMap()).apply { putAll(pairs) }
}
