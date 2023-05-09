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

    actual constructor() : this(HashMap<E, Nothing>())

    actual constructor(initialCapacity: Int) : this(HashMap<E, Nothing>(initialCapacity))

    actual constructor(elements: Collection<E>) : this(elements.size) {
        addAll(elements)
    }

    // This implementation doesn't use a loadFactor
    actual constructor(initialCapacity: Int, loadFactor: Float) : this(initialCapacity)

    @PublishedApi
    internal fun build(): Set<E> {
        backing.build()
        return if (size > 0) this else Empty
    }

    override actual val size: Int get() = backing.size
    override actual fun isEmpty(): Boolean = backing.isEmpty()
    override actual fun contains(element: E): Boolean = backing.containsKey(element)
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