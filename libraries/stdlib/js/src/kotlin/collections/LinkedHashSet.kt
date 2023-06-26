/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
/*
 * Based on GWT LinkedHashSet
 * Copyright 2008 Google Inc.
 */

package kotlin.collections

/**
 * The implementation of the [MutableSet] interface, backed by a [InternalMap] implementation.
 *
 * The insertion order is preserved by the corresponding [InternalMap] implementation.
 */
public actual open class LinkedHashSet<E> : HashSet<E>, MutableSet<E> {
    /**
     * Creates a new empty [LinkedHashSet].
     */
    actual constructor() : super()

    /**
     * Creates a new [LinkedHashSet] filled with the elements of the specified collection.
     *
     * The iteration order of elements in the created set is the same as in the specified collection.
     */
    actual constructor(elements: Collection<E>) : super(elements)

    /**
     * Creates a new empty [LinkedHashSet] with the specified initial capacity and load factor.
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
    actual constructor(initialCapacity: Int, loadFactor: Float) : super(initialCapacity, loadFactor)

    /**
     * Creates a new empty [LinkedHashSet] with the specified initial capacity.
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

    internal constructor(internalMap: InternalMap<E, Boolean>) : super(internalMap)

    private object EmptyHolder {
        val value = LinkedHashSet(InternalHashMap<Nothing, Boolean>(0).also { it.build() })
    }

    @PublishedApi
    internal fun build(): Set<E> {
        internalMap.build()
        return if (size > 0) this else EmptyHolder.value
    }

    override fun checkIsMutable() = internalMap.checkIsMutable()
}

/**
 * Creates a new instance of the specialized implementation of [LinkedHashSet] with the specified [String] elements,
 * which elements the keys as properties of JS object without hashing them.
 */
public fun linkedStringSetOf(vararg elements: String): LinkedHashSet<String> {
    return LinkedHashSet<String>(InternalStringLinkedMap()).apply { addAll(elements) }
}
