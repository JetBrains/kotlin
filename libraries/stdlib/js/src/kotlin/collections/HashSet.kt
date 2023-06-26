/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
/*
 * Based on GWT HashSet
 * Copyright 2008 Google Inc.
 */

package kotlin.collections

/**
 * The implementation of the [MutableSet] interface, backed by a [InternalMap] implementation.
 */
// Classes that extend HashSet and implement `build()` (freezing) operation
// have to make sure mutating methods check `checkIsMutable`.
public actual open class HashSet<E> : AbstractMutableSet<E>, MutableSet<E> {

    internal val internalMap: InternalMap<E, Boolean>

    /**
     * Internal constructor to specify the underlying map.
     * This is used by LinkedHashSet and stringSetOf().
     *
     * @param map underlying map to use.
     */
    internal constructor(map: InternalMap<E, Boolean>) {
        internalMap = map
    }

    /**
     * Creates a new empty [HashSet].
     */
    actual constructor() : this(InternalHashMap())

    /**
     * Creates a new [HashSet] filled with the elements of the specified collection.
     */
    actual constructor(elements: Collection<E>) : this(InternalHashMap(elements.size)) {
        for (element in elements) {
            internalMap.put(element, true)
        }
    }

    /**
     * Creates a new empty [HashSet] with the specified initial capacity and load factor.
     *
     * Capacity is the maximum number of elements the set is able to store in current internal data structure.
     * Load factor is the measure of how full the set is allowed to get in relation to
     * its capacity before the capacity is expanded, which usually leads to rebuild of the internal data structure.
     *
     * @param initialCapacity the initial capacity of the created set.
     *   Note that the argument is just a hint for the implementation and can be ignored.
     * @param loadFactor the load factor of the created set.
     *   Note that the argument is just a hint for the implementation and can be ignored.
     *
     * @throws IllegalArgumentException if [initialCapacity] is negative or [loadFactor] is non-positive.
     */
    actual constructor(initialCapacity: Int, loadFactor: Float) : this(InternalHashMap(initialCapacity, loadFactor))

    /**
     * Creates a new empty [HashSet] with the specified initial capacity.
     *
     * Capacity is the maximum number of elements the set is able to store in current internal data structure.
     * When the set gets full by a certain default load factor, its capacity is expanded,
     * which usually leads to rebuild of the internal data structure.
     *
     * @param initialCapacity the initial capacity of the created set.
     *   Note that the argument is just a hint for the implementation and can be ignored.
     *
     * @throws IllegalArgumentException if [initialCapacity] is negative.
     */
    actual constructor(initialCapacity: Int) : this(initialCapacity, 1.0f)

    actual override fun add(element: E): Boolean {
        return internalMap.put(element, true) == null
    }

    actual override fun clear() {
        internalMap.clear()
    }

    actual override operator fun contains(element: E): Boolean = internalMap.contains(element)

    actual override fun isEmpty(): Boolean = internalMap.size == 0

    actual override fun iterator(): MutableIterator<E> = internalMap.keysIterator()

    actual override fun remove(element: E): Boolean = internalMap.remove(element) != null

    actual override val size: Int get() = internalMap.size
}

/**
 * Creates a new instance of the specialized implementation of [HashSet] with the specified [String] elements,
 * which elements the keys as properties of JS object without hashing them.
 */
public fun stringSetOf(vararg elements: String): HashSet<String> {
    return HashSet<String>(InternalStringMap()).apply { addAll(elements) }
}
