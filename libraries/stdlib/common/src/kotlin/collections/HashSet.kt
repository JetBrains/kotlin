/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

public expect class HashSet<E> : MutableSet<E> {
    /**
     * Creates a new empty [HashSet].
     */
    public constructor()

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
    public constructor(initialCapacity: Int)

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
    public constructor(initialCapacity: Int, loadFactor: Float)

    /**
     * Creates a new [HashSet] filled with the elements of the specified collection.
     */
    public constructor(elements: Collection<E>)

    // From Set

    override val size: Int
    override fun isEmpty(): Boolean
    override fun contains(element: E): Boolean
    override fun containsAll(elements: Collection<E>): Boolean

    // From MutableSet

    override fun iterator(): MutableIterator<E>
    @IgnorableReturnValue
    override fun add(element: E): Boolean
    @IgnorableReturnValue
    override fun remove(element: E): Boolean
    @IgnorableReturnValue
    override fun addAll(elements: Collection<E>): Boolean
    @IgnorableReturnValue
    override fun removeAll(elements: Collection<E>): Boolean
    @IgnorableReturnValue
    override fun retainAll(elements: Collection<E>): Boolean
    override fun clear()
}
