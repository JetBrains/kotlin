/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Based on GWT AbstractHashMap
 * Copyright 2008 Google Inc.
 */

package kotlin.collections

import kotlin.collections.MutableMap.MutableEntry

/**
 * A hash table implementation of [MutableMap].
 *
 * This class stores key-value pairs using a hash table data structure that provides fast lookups
 * based on keys. It fully implements the [MutableMap] contract, providing all standard map operations
 * including insertion, removal, and lookup of values by key.
 *
 * ## Null keys and values
 *
 * [HashMap] accepts `null` as a key. Since keys are unique, at most one entry with a `null` key
 * can exist in the map. [HashMap] also accepts `null` as a value, and multiple entries can have
 * `null` values.
 *
 * ## Key's hash code and equality contracts
 *
 * [HashMap] relies on the [Any.hashCode] and [Any.equals] functions of keys to organize and locate entries.
 * Keys are considered equal if their [Any.equals] function returns `true`, and keys that are equal must
 * have the same [Any.hashCode] value. Violating this contract can lead to incorrect behavior.
 *
 * The [Any.hashCode] and [Any.equals] functions should be consistent and immutable during the lifetime
 * of the key objects. Modifying a key object in a way that changes its hash code or equality
 * after it has been used as a key in a [HashMap] may lead to the entry becoming unreachable.
 *
 * ## Performance characteristics
 *
 * The performance characteristics below assume that the [Any.hashCode] function of keys distributes
 * them uniformly across the hash table, minimizing collisions. A poor hash function that causes
 * many collisions can degrade performance.
 *
 * [HashMap] provides efficient implementation for common operations:
 *
 * - **Lookup** ([get], [containsKey]): O(1) time
 * - **Insertion and removal** ([put], [remove]): O(1) time
 * - **Value search** ([containsValue]): O(n) time, requires scanning all entries
 * - **Iteration** ([entries], [keys], [values]): O(n) time
 *
 * ## Iteration order
 *
 * [HashMap] does not guarantee any particular order for iteration over its keys, values, or entries.
 * The iteration order is unpredictable and may change when the map is rehashed (when entries are
 * added or removed and the internal capacity is adjusted). Do not rely on any specific iteration order.
 *
 * If a predictable iteration order is required, consider using [LinkedHashMap], which maintains
 * insertion order.
 *
 * ## Usage guidelines
 *
 * [HashMap] uses an internal data structure with a finite *capacity* - the maximum number of entries
 * it can store before needing to grow. When the map becomes full, it automatically increases its capacity
 * and performs *rehashing* - rebuilding the internal data structure to redistribute entries. Rehashing is
 * a relatively expensive operation that temporarily impacts performance. When creating a [HashMap], you can
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
 * [HashMap] is not thread-safe. If multiple threads access an instance concurrently and at least
 * one thread modifies it, external synchronization is required.
 *
 * @param K the type of map keys. The map is invariant in its key type.
 * @param V the type of map values. The mutable map is invariant in its value type.
 */
// Classes that extend HashMap and implement `build()` (freezing) operation
// have to make sure mutating methods check `checkIsMutable`.
public actual open class HashMap<K, V> : AbstractMutableMap<K, V>, MutableMap<K, V> {
    /**
     * Internal implementation of the map: either string-based or hashcode-based.
     */
    internal val internalMap: InternalMap<K, V>

    internal constructor(internalMap: InternalMap<K, V>) : super() {
        this.internalMap = internalMap
    }

    /**
     * Creates a new empty [HashMap].
     */
    public actual constructor() : this(InternalHashMap())

    /**
     * Creates a new empty [HashMap] with the specified initial capacity and load factor.
     *
     * Capacity is the maximum number of entries the map is able to store in the current internal data structure.
     *
     * @param initialCapacity the initial capacity of the created map.
     * @param loadFactor the load factor of the created map.
     *   Note that this parameter is not used by this implementation.
     *
     * @throws IllegalArgumentException if [initialCapacity] is negative or [loadFactor] is non-positive.
     */
    public actual constructor(initialCapacity: Int, loadFactor: Float) : this(InternalHashMap(initialCapacity, loadFactor))

    /**
     * Creates a new empty [HashMap] with the specified initial capacity.
     *
     * Capacity is the maximum number of entries the map is able to store in the current internal data structure.
     * When the map gets full, its capacity is expanded, which usually leads to rebuild of the internal
     * data structure.
     *
     * @param initialCapacity the initial capacity of the created map.
     *
     * @throws IllegalArgumentException if [initialCapacity] is negative.
     */
    public actual constructor(initialCapacity: Int) : this(initialCapacity, 1.0f)

    /**
     * Creates a new [HashMap] filled with the contents of the specified [original] map.
     */
    public actual constructor(original: Map<out K, V>) : this(InternalHashMap(original))

    actual override fun clear() {
        internalMap.clear()
    }

    actual override fun containsKey(key: K): Boolean = internalMap.contains(key)

    actual override fun containsValue(value: V): Boolean = internalMap.containsValue(value)

    override fun createKeysView(): MutableSet<K> = HashMapKeys(internalMap)
    override fun createValuesView(): MutableCollection<V> = HashMapValues(internalMap)

    private var entriesView: HashMapEntrySet<K, V>? = null
    actual override val entries: MutableSet<MutableEntry<K, V>>
        get() = entriesView ?: HashMapEntrySet(internalMap).also { entriesView = it }

    actual override operator fun get(key: K): V? = internalMap.get(key)

    @IgnorableReturnValue
    actual override fun put(key: K, value: V): V? = internalMap.put(key, value)

    @IgnorableReturnValue
    actual override fun remove(key: K): V? = internalMap.remove(key)

    actual override val size: Int get() = internalMap.size

    actual override fun putAll(from: Map<out K, V>): Unit = internalMap.putAll(from)
}

/**
 * Constructs the specialized implementation of [HashMap] with [String] keys,
 * which stores the keys as properties of JS object without hashing them.
 */
public fun <V> stringMapOf(vararg pairs: Pair<String, V>): HashMap<String, V> {
    return HashMap<String, V>(InternalStringMap()).apply { putAll(pairs) }
}
