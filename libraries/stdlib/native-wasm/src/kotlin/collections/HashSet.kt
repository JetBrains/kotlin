/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

actual class HashSet<E> internal constructor(
        private val backing: HashMap<E, *>
) : MutableSet<E>, kotlin.native.internal.KonanSet<E>, AbstractMutableSet<E>() {
    private companion object {
        private val Empty = HashSet(HashMap.EmptyHolder.value<Nothing, Nothing>())
    }

    /**
     * Creates a new empty [HashSet].
     */
    actual constructor() : this(HashMap<E, Nothing>())

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
    actual constructor(initialCapacity: Int) : this(HashMap<E, Nothing>(initialCapacity))

    /**
     * Creates a new [HashSet] filled with the elements of the specified collection.
     */
    actual constructor(elements: Collection<E>) : this(elements.size) {
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
    actual constructor(initialCapacity: Int, loadFactor: Float) : this(HashMap<E, Nothing>(initialCapacity, loadFactor))

    @PublishedApi
    internal fun build(): Set<E> {
        backing.build()
        return if (size > 0) this else Empty
    }

    override actual val size: Int get() = backing.size
    override actual fun isEmpty(): Boolean = backing.isEmpty()
    override actual fun contains(element: E): Boolean = backing.containsKey(element)

    /** Implements KonanSet.getElement(). Used for ObjC interop. */
    @Deprecated("This function is not supposed to be used directly.")
    @DeprecatedSinceKotlin(warningSince = "1.9") // TODO: advance to HIDDEN eventually
    override fun getElement(element: E): E? = backing.getKey(element)
    override actual fun clear() = backing.clear()
    override actual fun add(element: E): Boolean = backing.addKey(element) >= 0
    override actual fun remove(element: E): Boolean = backing.removeKey(element) >= 0
    override actual fun iterator(): MutableIterator<E> = backing.keysIterator()

    override actual fun addAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.addAll(elements)
    }

    override actual fun removeAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.removeAll(elements)
    }

    override actual fun retainAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.retainAll(elements)
    }
}

// This hash set keeps insertion order.
actual typealias LinkedHashSet<V> = HashSet<V>