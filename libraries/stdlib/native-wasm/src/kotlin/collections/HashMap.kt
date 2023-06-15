/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

import kotlin.native.concurrent.isFrozen
import kotlin.native.FreezingIsDeprecated

@OptIn(FreezingIsDeprecated::class)
actual class HashMap<K, V> private constructor(private val backing: HashMapInternal<K, V>) : MutableMap<K, V> {
    private companion object {
        private val Empty = HashMap(HashMapInternal.EmptyHolder.value)
    }

    actual override val size: Int get() = backing.size

    actual override val keys: MutableSet<K> get() = backing.getKeys { !it.isFrozen }

    actual override val values: MutableCollection<V> get() = backing.getValues { !it.isFrozen }

    actual override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = backing.getEntries { !it.isFrozen }

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
    actual constructor(initialCapacity: Int) : this(HashMapInternal<K, V>(initialCapacity))

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
    actual constructor(initialCapacity: Int, loadFactor: Float) : this(HashMapInternal<K, V>(initialCapacity, loadFactor))

    /**
     * Creates a new [HashMap] filled with the contents of the specified [original] map.
     */
    actual constructor(original: Map<out K, V>) : this(HashMapInternal<K, V>(original))

    /**
     * Creates a new empty [HashMap].
     */
    actual constructor() : this(HashMapInternal<K, V>())

    actual override fun isEmpty(): Boolean = backing.isEmpty()

    actual override fun containsKey(key: K): Boolean = backing.containsKey(key)

    actual override fun containsValue(value: @UnsafeVariance V): Boolean = backing.containsValue(value)

    actual override fun get(key: K): V? = backing.get(key)

    actual override fun put(key: K, value: V): V? = backing.put(key, value)

    actual override fun remove(key: K): V? = backing.remove(key)

    actual override fun putAll(from: Map<out K, V>) {
        backing.putAll(from)
    }

    actual override fun clear() {
        backing.clear()
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                (other is Map<*, *>) &&
                backing.contentEquals(other)
    }

    override fun hashCode(): Int = backing.hashCode()

    override fun toString(): String = backing.toString()

    @PublishedApi
    internal fun build(): Map<K, V> {
        backing.build()
        @Suppress("UNCHECKED_CAST")
        return if (size > 0) this else Empty as Map<K, V>
    }
}

// This hash map keeps insertion order.
actual typealias LinkedHashMap<K, V> = HashMap<K, V>
