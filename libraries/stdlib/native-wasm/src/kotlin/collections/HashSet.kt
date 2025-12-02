/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

/**
 * A hash table implementation of [MutableSet].
 *
 * This class stores unique elements using a hash table data structure that provides fast lookups
 * and ensures no duplicate elements are stored. It fully implements the [MutableSet] contract,
 * providing all standard Set operations including lookup, insertion, and removal.
 *
 * ## Null elements
 *
 * [HashSet] accepts `null` as an element. Since elements are unique, at most one `null` element
 * can exist in the set.
 *
 * ## Element's hash code and equality contracts
 *
 * [HashSet] relies on the [Any.hashCode] and [Any.equals] functions of elements to organize and locate them.
 * Elements are considered equal if their [Any.equals] function returns `true`, and elements that are equal
 * must have the same [Any.hashCode] value. Violating this contract can lead to incorrect behavior, such as
 * duplicate elements being stored or elements becoming unreachable.
 *
 * The [Any.hashCode] and [Any.equals] functions should be consistent and immutable during the lifetime
 * of the element objects. Modifying an element in a way that changes its hash code or equality
 * after it has been added to a [HashSet] may lead to the element becoming unreachable.
 *
 * ## Performance characteristics
 *
 * The performance characteristics below assume that the [Any.hashCode] function of elements distributes
 * them uniformly across the hash table, minimizing collisions. A poor hash function that causes
 * many collisions can degrade performance.
 *
 * [HashSet] provides efficient implementation for common operations:
 *
 * - **Lookup** ([contains]): O(1) time
 * - **Insertion and removal** ([add], [remove]): O(1) time
 * - **Iteration**: O(n) time
 *
 * ## Iteration order
 *
 * [HashSet] does not guarantee any particular order for iteration over its elements.
 * The iteration order is unpredictable and may change when the set is rehashed (when elements are
 * added or removed and the internal capacity is adjusted). Do not rely on any specific iteration order.
 *
 * If a predictable iteration order is required, consider using [LinkedHashSet], which maintains
 * insertion order.
 *
 * ## Usage guidelines
 *
 * [HashSet] uses an internal data structure with a finite *capacity* - the maximum number of elements
 * it can store before needing to grow. When the set becomes full, it automatically increases its capacity
 * and performs *rehashing* - rebuilding the internal data structure to redistribute elements. Rehashing is
 * a relatively expensive operation that temporarily impacts performance. When creating a [HashSet], you can
 * optionally provide an initial capacity value, which will be used to size the internal data structure,
 * potentially avoiding rehashing operations as the set grows.
 *
 * To optimize performance and memory usage:
 *
 * - If the number of elements is known in advance, use the constructor with initial capacity
 *   to avoid multiple rehashing operations as the set grows.
 * - Ensure element objects have well-distributed [Any.hashCode] implementations to minimize collisions
 *   and maintain good performance.
 * - Prefer [addAll] over multiple individual [add] calls when adding multiple elements.
 *
 * ## Thread safety
 *
 * [HashSet] is not thread-safe. If multiple threads access an instance concurrently and at least
 * one thread modifies it, external synchronization is required.
 *
 * @param E the type of elements contained in the set. The mutable set is invariant in its element type.
 */
public actual class HashSet<E> internal constructor(
    private val backing: HashMap<E, *>
) : MutableSet<E>, kotlin.native.internal.KonanSet<E>, AbstractMutableSet<E>() {
    private companion object {
        private val Empty = HashSet(HashMap.EmptyHolder.value<Nothing, Nothing>())
    }

    /**
     * Creates a new empty [HashSet].
     */
    public actual constructor() : this(HashMap<E, Nothing>())

    /**
     * Creates a new empty [HashSet] with the specified initial capacity.
     *
     * Capacity is the maximum number of elements the set is able to store in the current internal data structure.
     * When the set gets full, its capacity is expanded, which usually leads to rebuild of the internal
     * data structure.
     *
     * @param initialCapacity the initial capacity of the created set.
     *
     * @throws IllegalArgumentException if [initialCapacity] is negative.
     */
    public actual constructor(initialCapacity: Int) : this(HashMap<E, Nothing>(initialCapacity))

    /**
     * Creates a new [HashSet] filled with the elements of the specified collection.
     */
    public actual constructor(elements: Collection<E>) : this(elements.size) {
        addAll(elements)
    }

    /**
     * Creates a new empty [HashSet] with the specified initial capacity and load factor.
     *
     * Capacity is the maximum number of elements the set is able to store in the current internal data structure.
     *
     * @param initialCapacity the initial capacity of the created set.
     * @param loadFactor the load factor of the created set.
     *   Note that this parameter is not used by this implementation.
     *
     * @throws IllegalArgumentException if [initialCapacity] is negative or [loadFactor] is non-positive.
     */
    public actual constructor(initialCapacity: Int, loadFactor: Float) : this(HashMap<E, Nothing>(initialCapacity, loadFactor))

    @PublishedApi
    internal fun build(): Set<E> {
        val _ = backing.build()
        return if (size > 0) this else Empty
    }

    override actual val size: Int get() = backing.size
    override actual fun isEmpty(): Boolean = backing.isEmpty()
    override actual fun contains(element: E): Boolean = backing.containsKey(element)

    /** Implements KonanSet.getElement(). Used for ObjC interop. */
    @Deprecated("This function is not supposed to be used directly.")
    @DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1") // TODO: advance to HIDDEN eventually
    override fun getElement(element: E): E? = backing.getKey(element)
    override actual fun clear(): Unit = backing.clear()

    @IgnorableReturnValue
    override actual fun add(element: E): Boolean = backing.addKey(element) >= 0

    @IgnorableReturnValue
    override actual fun remove(element: E): Boolean = backing.removeKey(element)
    override actual fun iterator(): MutableIterator<E> = backing.keysIterator()

    @IgnorableReturnValue
    override actual fun addAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.addAll(elements)
    }

    @IgnorableReturnValue
    override actual fun removeAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.removeAll(elements)
    }

    @IgnorableReturnValue
    override actual fun retainAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.retainAll(elements)
    }
}

// This hash set keeps insertion order.
public actual typealias LinkedHashSet<V> = HashSet<V>
