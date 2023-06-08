/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

    private inner class EntrySet : AbstractEntrySet<MutableEntry<K, V>, K, V>() {

        override fun add(element: MutableEntry<K, V>): Boolean = throw UnsupportedOperationException("Add is not supported on entries")
        override fun clear() {
            this@HashMap.clear()
        }

        override fun containsEntry(element: Map.Entry<K, V>): Boolean = this@HashMap.containsEntry(element)

        override operator fun iterator(): MutableIterator<MutableEntry<K, V>> = internalMap.iterator()

        override fun removeEntry(element: Map.Entry<K, V>): Boolean {
            if (contains(element)) {
                this@HashMap.remove(element.key)
                return true
            }
            return false
        }

        override val size: Int get() = this@HashMap.size
    }


    /**
     * Internal implementation of the map: either string-based or hashcode-based.
     */
    private val internalMap: InternalMap<K, V>

    private val equality: EqualityComparator

    internal constructor(internalMap: InternalMap<K, V>) : super() {
        this.internalMap = internalMap
        this.equality = internalMap.equality
    }

    /**
     * Creates a new empty [HashMap].
     */
    actual constructor() : this(InternalHashCodeMap(EqualityComparator.HashCode))

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
    actual constructor(initialCapacity: Int, loadFactor: Float) : this() {
        // This implementation of HashMap has no need of load factors or capacities.
        require(initialCapacity >= 0) { "Negative initial capacity: $initialCapacity" }
        require(loadFactor > 0) { "Non-positive load factor: $loadFactor" }
    }

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
    actual constructor(initialCapacity: Int) : this(initialCapacity, 1.0f)


    /**
     * Creates a new [HashMap] filled with the contents of the specified [original] map.
     */
    actual constructor(original: Map<out K, V>) : this() {
        this.putAll(original)
    }

    actual override fun clear() {
        internalMap.clear()
//        structureChanged(this)
    }

    actual override fun containsKey(key: K): Boolean = internalMap.contains(key)

    actual override fun containsValue(value: V): Boolean = internalMap.any { equality.equals(it.value, value) }

    private var _entries: MutableSet<MutableMap.MutableEntry<K, V>>? = null
    actual override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            if (_entries == null) {
                _entries = createEntrySet()
            }
            return _entries!!
        }

    internal open fun createEntrySet(): MutableSet<MutableMap.MutableEntry<K, V>> = EntrySet()

    actual override operator fun get(key: K): V? = internalMap.get(key)

    actual override fun put(key: K, value: V): V? = internalMap.put(key, value)

    actual override fun remove(key: K): V? = internalMap.remove(key)

    actual override val size: Int get() = internalMap.size

}

/**
 * Constructs the specialized implementation of [HashMap] with [String] keys, which stores the keys as properties of
 * JS object without hashing them.
 */
public fun <V> stringMapOf(vararg pairs: Pair<String, V>): HashMap<String, V> {
    return HashMap<String, V>(InternalStringMap(EqualityComparator.HashCode)).apply { putAll(pairs) }
}
