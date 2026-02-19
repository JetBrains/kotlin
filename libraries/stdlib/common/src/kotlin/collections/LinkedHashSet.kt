/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * A hash table implementation of [MutableSet] that maintains insertion order.
 *
 * This class stores unique elements using a hash table data structure that provides fast lookups
 * and ensures no duplicate elements are stored, while also maintaining the order in which elements
 * were inserted. It fully implements the [MutableSet] contract, providing all standard set operations
 * including lookup, insertion, and removal.
 *
 * ## Null elements
 *
 * [LinkedHashSet] accepts `null` as an element. Since elements are unique, at most one `null` element
 * can exist in the set.
 *
 * ## Element's hash code and equality contracts
 *
 * [LinkedHashSet] relies on the [Any.hashCode] and [Any.equals] functions of elements to organize and locate them.
 * Elements are considered equal if their [Any.equals] function returns `true`, and elements that are equal
 * must have the same [Any.hashCode] value. Violating this contract can lead to incorrect behavior, such as
 * duplicate elements being stored or elements becoming unreachable.
 *
 * The [Any.hashCode] and [Any.equals] functions should be consistent and immutable during the lifetime
 * of the element objects. Modifying an element in a way that changes its hash code or equality
 * after it has been added to a [LinkedHashSet] may lead to the element becoming unreachable.
 *
 * ## Performance characteristics
 *
 * The performance characteristics below assume that the [Any.hashCode] function of elements distributes
 * them uniformly across the hash table, minimizing collisions. A poor hash function that causes
 * many collisions can degrade performance.
 *
 * [LinkedHashSet] provides efficient implementation for common operations:
 *
 * - **Lookup** ([contains]): O(1) time
 * - **Insertion and removal** ([add], [remove]): O(1) time
 * - **Iteration**: O(n) time
 *
 * ## Iteration order
 *
 * [LinkedHashSet] maintains a predictable iteration order for its elements.
 * Elements are iterated in the order they were inserted into the set, from oldest to newest.
 * This insertion order is preserved even when the set is rehashed (when elements are added or removed
 * and the internal capacity is adjusted).
 *
 * Note that the insertion order is not affected if an element is _re-inserted_ into the set.
 * An element `e` is re-inserted into the set when `add(e)` is called and the set already contains
 * an element equal to `e`.
 *
 * If predictable iteration order is not required, consider using [HashSet], which may have
 * slightly better performance characteristics.
 *
 * ## Usage guidelines
 *
 * [LinkedHashSet] uses an internal data structure with a finite *capacity* - the maximum number of elements
 * it can store before needing to grow. As elements are added, the set tracks its *load factor*, which is
 * the ratio of the number of elements to the current capacity. When this ratio exceeds a certain threshold,
 * the set automatically increases its capacity and performs *rehashing* - rebuilding the internal data
 * structure to redistribute elements. Rehashing is a relatively expensive operation that temporarily impacts
 * performance. When creating a [LinkedHashSet], you can optionally provide values for the initial capacity and
 * load factor threshold. Note that these parameters are just hints for the implementation and can be ignored.
 *
 * To optimize performance and memory usage:
 *
 * - If the number of elements is known in advance, use the constructor with initial capacity
 *   to avoid multiple rehashing operations as the set grows.
 * - Choose an appropriate load factor when creating the set. A lower load factor reduces collision
 *   probability but uses more memory, while a higher load factor saves memory but may increase
 *   lookup time. The default load factor typically provides a good balance.
 * - Ensure element objects have well-distributed [Any.hashCode] implementations to minimize collisions
 *   and maintain good performance.
 * - Prefer [addAll] over multiple individual [add] calls when adding multiple elements.
 *
 * ## Thread safety
 *
 * [LinkedHashSet] is not thread-safe. If multiple threads access an instance concurrently and at least
 * one thread modifies it, external synchronization is required.
 *
 * @param E the type of elements contained in the set. The mutable set is invariant in its element type.
 */
public expect class LinkedHashSet<E> : MutableSet<E> {
    /**
     * Creates a new empty [LinkedHashSet].
     */
    public constructor()

    /**
     * Creates a new empty [LinkedHashSet] with the specified initial capacity.
     *
     * Capacity is the maximum number of elements the set is able to store in the current internal data structure.
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
     * Creates a new empty [LinkedHashSet] with the specified initial capacity and load factor.
     *
     * Capacity is the maximum number of elements the set is able to store in the current internal data structure.
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
     * Creates a new [LinkedHashSet] filled with the elements of the specified collection.
     *
     * The iteration order of elements in the created set is the same as in the specified collection.
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
