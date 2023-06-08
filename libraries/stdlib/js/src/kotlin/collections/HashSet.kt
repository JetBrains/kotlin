/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
/*
 * Based on GWT HashSet
 * Copyright 2008 Google Inc.
 */

package kotlin.collections

/**
 * The implementation of the [MutableSet] interface, backed by a [HashMap] instance.
 */
// Classes that extend HashSet and implement `build()` (freezing) operation
// have to make sure mutating methods check `checkIsMutable`.
public actual open class HashSet<E> : AbstractMutableSet<E>, MutableSet<E> {

    internal val map: HashMap<E, Any>

    /**
     * Creates a new empty [HashSet].
     */
    actual constructor() {
        map = HashMap<E, Any>()
    }

    /**
     * Creates a new [HashSet] filled with the elements of the specified collection.
     */
    actual constructor(elements: Collection<E>) {
        map = HashMap<E, Any>(elements.size)
        addAll(elements)
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
    actual constructor(initialCapacity: Int, loadFactor: Float) {
        map = HashMap<E, Any>(initialCapacity, loadFactor)
    }

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

    /**
     * Protected constructor to specify the underlying map. This is used by
     * LinkedHashSet.

     * @param map underlying map to use.
     */
    internal constructor(map: HashMap<E, Any>) {
        this.map = map
    }

    actual override fun add(element: E): Boolean {
        val old = map.put(element, this)
        return old == null
    }

    actual override fun clear() {
        map.clear()
    }

//    public override fun clone(): Any {
//        return HashSet<E>(this)
//    }

    actual override operator fun contains(element: E): Boolean = map.containsKey(element)

    actual override fun isEmpty(): Boolean = map.isEmpty()

    actual override fun iterator(): MutableIterator<E> = map.keys.iterator()

    actual override fun remove(element: E): Boolean = map.remove(element) != null

    actual override val size: Int get() = map.size

}

/**
 * Creates a new instance of the specialized implementation of [HashSet] with the specified [String] elements,
 * which elements the keys as properties of JS object without hashing them.
 */
public fun stringSetOf(vararg elements: String): HashSet<String> {
    return HashSet(stringMapOf<Any>()).apply { addAll(elements) }
}
