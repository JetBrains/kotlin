/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
/*
 * Based on GWT HashSet
 * Copyright 2008 Google Inc.
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
// Classes that extend HashSet and implement `build()` (freezing) operation
// have to make sure mutating methods check `checkIsMutable`.
public actual open class HashSet<E> : AbstractMutableSet<E>, MutableSet<E> {

    internal val internalMap: InternalMap<E, Boolean>

    /**
     * Internal constructor to specify the underlying map.
     * This is used by LinkedHashSet and stringSetOf().
     *
     * @param map underlying map to use.
     */
    internal constructor(map: InternalMap<E, Boolean>) {
        internalMap = map
    }

    /**
     * Creates a new empty [HashSet].
     */
    public actual constructor() : this(InternalHashMap())

    /**
     * Creates a new [HashSet] filled with the elements of the specified collection.
     */
    public actual constructor(elements: Collection<E>) : this(InternalHashMap(elements.size)) {
        for (element in elements) {
            internalMap.put(element, true)
        }
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
    public actual constructor(initialCapacity: Int, loadFactor: Float) : this(InternalHashMap(initialCapacity, loadFactor))

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
    public actual constructor(initialCapacity: Int) : this(initialCapacity, 1.0f)

    @IgnorableReturnValue
    actual override fun add(element: E): Boolean {
        return internalMap.put(element, true) == null
    }

    actual override fun clear() {
        internalMap.clear()
    }

    actual override operator fun contains(element: E): Boolean = internalMap.contains(element)

    actual override fun isEmpty(): Boolean = internalMap.size == 0

    actual override fun iterator(): MutableIterator<E> = internalMap.keysIterator()

    @IgnorableReturnValue
    actual override fun remove(element: E): Boolean = internalMap.remove(element) != null

    actual override val size: Int get() = internalMap.size
}

/**
 * Creates a new instance of the specialized implementation of [HashSet] with the specified [String] elements,
 * which elements the keys as properties of JS object without hashing them.
 */
public fun stringSetOf(vararg elements: String): HashSet<String> {
    return HashSet<String>(InternalStringMap()).apply { addAll(elements) }
}
