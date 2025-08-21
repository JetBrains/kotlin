/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

public expect class ArrayList<E> : MutableList<E>, RandomAccess {

    /**
     * Creates a new empty [ArrayList].
     */
    public constructor()

    /**
     * Creates a new empty [ArrayList] with the specified initial capacity.
     *
     * Capacity is the maximum number of elements the list is able to store in current backing storage.
     * When the list gets full and a new element can't be added, its capacity is expanded,
     * which usually leads to creation of a bigger backing storage.
     *
     * @param initialCapacity the initial capacity of the created list.
     *   Note that the argument is just a hint for the implementation and can be ignored.
     *
     * @throws IllegalArgumentException if [initialCapacity] is negative.
     */
    public constructor(initialCapacity: Int)

    /**
     * Creates a new [ArrayList] filled with the elements of the specified collection.
     *
     * The iteration order of elements in the created list is the same as in the specified collection.
     */
    public constructor(elements: Collection<E>)

    public fun trimToSize()
    public fun ensureCapacity(minCapacity: Int)

    // From List

    override val size: Int
    override fun isEmpty(): Boolean
    override fun contains(element: E): Boolean
    override fun containsAll(elements: Collection<E>): Boolean
    override operator fun get(index: Int): E
    override fun indexOf(element: E): Int
    override fun lastIndexOf(element: E): Int

    // From MutableCollection

    override fun iterator(): MutableIterator<E>

    // From MutableList

    @IgnorableReturnValue
    override fun add(element: E): Boolean

    @IgnorableReturnValue
    override fun remove(element: E): Boolean

    @IgnorableReturnValue
    override fun addAll(elements: Collection<E>): Boolean

    @IgnorableReturnValue
    override fun addAll(index: Int, elements: Collection<E>): Boolean

    @IgnorableReturnValue
    override fun removeAll(elements: Collection<E>): Boolean

    @IgnorableReturnValue
    override fun retainAll(elements: Collection<E>): Boolean
    override fun clear()

    @IgnorableReturnValue
    override operator fun set(index: Int, element: E): E
    override fun add(index: Int, element: E)

    @IgnorableReturnValue
    override fun removeAt(index: Int): E
    override fun listIterator(): MutableListIterator<E>
    override fun listIterator(index: Int): MutableListIterator<E>
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>
}
