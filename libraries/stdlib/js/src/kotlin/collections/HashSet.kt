/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
/*
 * Based on GWT HashSet
 * Copyright 2008 Google Inc.
 */

package kotlin.collections

/**
 * The implementation of the [MutableSet] interface, backed by a [HashMap] instance.
 */
public actual open class HashSet<E> : AbstractMutableSet<E>, MutableSet<E> {

    private val map: HashMap<E, Any>

    /**
     * Constructs a new empty [HashSet].
     */
    actual constructor() {
        map = HashMap<E, Any>()
    }

    /**
     * Constructs a new [HashSet] filled with the elements of the specified collection.
     */
    actual constructor(elements: Collection<E>) {
        map = HashMap<E, Any>(elements.size)
        addAll(elements)
    }

    /**
     * Constructs a new empty [HashSet].
     *
     * @param  initialCapacity the initial capacity (ignored)
     * @param  loadFactor      the load factor (ignored)
     *
     * @throws IllegalArgumentException if the initial capacity or load factor are negative
     */
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    actual constructor(initialCapacity: Int, loadFactor: Float = 0.0f) {
        map = HashMap<E, Any>(initialCapacity, loadFactor)
    }

    actual constructor(initialCapacity: Int) : this(initialCapacity, 0.0f)

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
