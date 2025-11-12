/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * A hash table implementation of [MutableSet].
 *
 * This class stores unique elements using a hash table data structure that provides fast lookups
 * and ensures no duplicate elements are stored. It fully implements the [MutableSet] contract,
 * providing all standard Set operations including lookup, insertion, and removal.
 *
 * ## Hash and equality contract
 *
 * [HashSet] relies on the [hashCode] and [equals] functions of elements to organize and locate them.
 * Elements are considered equal if their [equals] function returns `true`, and elements that are equal
 * must have the same [hashCode] value. Violating this contract can lead to incorrect behavior, such as
 * duplicate elements being stored or elements becoming unreachable.
 *
 * The [hashCode] and [equals] functions should be consistent and immutable during the lifetime
 * of the element objects. Modifying an element in a way that changes its hash code or equality
 * after it has been added to a [HashSet] may lead to the element becoming unreachable.
 *
 * ## Performance characteristics
 *
 * [HashSet] provides efficient implementation for common operations:
 *
 * - **Lookup** ([contains]): O(1) average case, O(n) worst case. Performance depends
 *   on the quality of the [hashCode] function. A good hash function distributes elements uniformly,
 *   minimizing collisions and maintaining constant-time performance.
 * - **Insertion and removal** ([add], [remove]): O(1) average case, O(n) worst case
 * - **Iteration**: O(n + capacity) time, where capacity is the current internal table size.
 *   Iteration time depends on both the number of elements and the table capacity.
 *
 * Note: On the JS target, these time-complexity guarantees may not hold due to the underlying
 * JavaScript engine implementation.
 *
 * ## Usage guidelines
 *
 * To optimize performance and memory usage:
 *
 * - If the number of elements is known in advance, use the constructor with initial capacity
 *   to avoid multiple rehashing operations as the set grows.
 * - Choose an appropriate load factor when creating the set. A lower load factor reduces collision
 *   probability but uses more memory, while a higher load factor saves memory but may increase
 *   lookup time. The default load factor typically provides a good balance.
 * - Ensure element objects have well-distributed [hashCode] implementations to minimize collisions
 *   and maintain good performance.
 * - Prefer [addAll] over multiple individual [add] calls when adding multiple elements.
 *
 * ## Thread safety
 *
 * [HashSet] is not thread-safe. If multiple threads access an instance concurrently and at least
 * one thread modifies it, external synchronization is required.
 *
 * @param E the type of elements contained in the set.
 */
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
