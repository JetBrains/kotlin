/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Based on GWT AbstractHashMap
 * Copyright 2008 Google Inc.
 */

package kotlin.collections

import kotlin.collections.MutableMap.MutableEntry

/**
 * Hash table based implementation of the [MutableMap] interface.
 *
 * This implementation makes no guarantees regarding the order of enumeration of [keys], [values] and [entries] collections.
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
    public actual constructor(initialCapacity: Int, loadFactor: Float) : this(InternalHashMap(initialCapacity, loadFactor))

    /**
     * Creates a new empty [HashMap] with the specified initial capacity.
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

    actual override fun put(key: K, value: V): V? = internalMap.put(key, value)

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
