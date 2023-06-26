/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Based on GWT LinkedHashMap
 * Copyright 2008 Google Inc.
 */
package kotlin.collections

/**
 * Hash table based implementation of the [MutableMap] interface,
 * which additionally preserves the insertion order of entries during the iteration.
 *
 * The insertion order is preserved by the corresponding [InternalMap] implementation.
 */
public actual open class LinkedHashMap<K, V> : HashMap<K, V>, MutableMap<K, V> {
    /**
     * Creates a new empty [LinkedHashMap].
     */
    actual constructor() : super()

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
    actual constructor(initialCapacity: Int) : super(initialCapacity)

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
    actual constructor(initialCapacity: Int, loadFactor: Float) : super(initialCapacity, loadFactor)

    /**
     * Creates a new [LinkedHashMap] filled with the contents of the specified [original] map.
     *
     * The iteration order of entries in the created map is the same as in the [original] map.
     */
    actual constructor(original: Map<out K, V>) : super(original)

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
