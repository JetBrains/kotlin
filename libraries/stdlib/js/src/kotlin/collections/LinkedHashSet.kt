/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
/*
 * Based on GWT LinkedHashSet
 * Copyright 2008 Google Inc.
 */

package kotlin.collections

/**
 * The implementation of the [MutableSet] interface, backed by a [LinkedHashMap] instance.
 *
 * This implementation preserves the insertion order of elements during the iteration.
 */
public actual open class LinkedHashSet<E> : HashSet<E>, MutableSet<E> {

    internal constructor(map: LinkedHashMap<E, Any>) : super(map)

    /**
     * Constructs a new empty [LinkedHashSet].
     */
    actual constructor() : super(LinkedHashMap<E, Any>())

    /**
     * Constructs a new [LinkedHashSet] filled with the elements of the specified collection.
     */
    actual constructor(elements: Collection<E>) : super(LinkedHashMap<E, Any>()) {
        addAll(elements)
    }

    /**
     * Constructs a new empty [LinkedHashSet].
     *
     * @param  initialCapacity the initial capacity (ignored)
     * @param  loadFactor      the load factor (ignored)
     *
     * @throws IllegalArgumentException if the initial capacity or load factor are negative
     */
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    actual constructor(initialCapacity: Int, loadFactor: Float = 0.0f) : super(LinkedHashMap<E, Any>(initialCapacity, loadFactor))

    actual constructor(initialCapacity: Int) : this(initialCapacity, 0.0f)

//    public override fun clone(): Any {
//        return LinkedHashSet(this)
//    }

}

/**
 * Creates a new instance of the specialized implementation of [LinkedHashSet] with the specified [String] elements,
 * which elements the keys as properties of JS object without hashing them.
 */
public fun linkedStringSetOf(vararg elements: String): LinkedHashSet<String> {
    return LinkedHashSet(linkedStringMapOf<Any>()).apply { addAll(elements) }
}
