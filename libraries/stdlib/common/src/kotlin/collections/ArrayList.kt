/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * A dynamic array implementation of [MutableList].
 *
 * This class stores elements contiguously in memory using an internal array that automatically
 * grows as needed. It fully implements the [MutableList] contract, providing all standard list
 * operations including indexed access, iteration, and modification. As an implementation of
 * [RandomAccess], it provides fast indexed access to elements.
 *
 * ## Performance characteristics
 *
 * [ArrayList] provides efficient implementation for common operations:
 *
 * - **Indexed access** ([get], [set]): O(1) constant time
 * - **Appending to the end** ([add]): O(1) [amortized](https://en.wikipedia.org/wiki/Amortized_analysis)
 *   constant time. When the internal array is full, it must be resized, which takes O(n) time to copy
 *   all existing elements to a new, larger array. However, these resize operations become less frequent
 *   as the list grows, making the average cost per appending constant over many operations.
 * - **Removing from the end** ([removeLast], [removeAt]`(size - 1)`): O(1) constant time
 * - **Inserting or removing at a position** ([add] with index, [removeAt]): O(n) linear time,
 *   as elements after the position must be shifted
 * - **Search operations** ([contains], [indexOf], [lastIndexOf]): O(n) linear time
 * - **Iteration**: O(n) linear time
 *
 * ## Usage guidelines
 *
 * To optimize performance and memory usage:
 *
 * - If the number of elements is known in advance, use the constructor with initial capacity
 *   to avoid multiple reallocations as the list grows.
 * - Use [ensureCapacity] before adding many elements to pre-allocate sufficient storage.
 * - Prefer [addAll] over multiple individual [add] calls when adding multiple elements.
 * - Call [trimToSize] after all elements have been added to reduce memory consumption if no further
 *   growth is expected.
 *
 * ## Thread safety
 *
 * [ArrayList] is not thread-safe. If multiple threads access an instance concurrently and at least
 * one thread modifies it, external synchronization is required.
 *
 * @param E the type of elements contained in the list.
 */
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

    /**
     * Attempts to reduce the storage used for this list.
     *
     * If the backing storage of this list is larger than necessary to hold its current elements,
     * then it may be resized to become more space efficient.
     * This operation can help reduce memory consumption when the list is not expected to grow further.
     *
     * On the JS target, this method has no effect due to the internal JS implementation.
     *
     * @sample samples.collections.Collections.Lists.ArrayList.trimToSize
     */
    public fun trimToSize()

    /**
     * Ensures that the capacity of this list is at least equal to the specified [minCapacity].
     *
     * If the current capacity is less than the [minCapacity], a new backing storage is allocated with greater capacity.
     * Otherwise, this method takes no action and simply returns.
     *
     * This operation can be used to minimize the number of incremental reallocations when the eventual size
     * of the list is known in advance, improving performance when adding many elements.
     *
     * On the JS target, this method has no effect due to the internal JS implementation.
     *
     * @param minCapacity the desired minimum capacity.
     *
     * @sample samples.collections.Collections.Lists.ArrayList.ensureCapacity
     */
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
