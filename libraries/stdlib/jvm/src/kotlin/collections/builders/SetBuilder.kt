/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections.builders

import java.io.NotSerializableException

internal class SetBuilder<E> internal constructor(
    private val backing: MapBuilder<E, *>
) : MutableSet<E>, AbstractMutableSet<E>(), Serializable {
    private companion object {
        private val Empty = SetBuilder(MapBuilder.Empty)
    }

    constructor() : this(MapBuilder<E, Nothing>())

    constructor(initialCapacity: Int) : this(MapBuilder<E, Nothing>(initialCapacity))

    fun build(): Set<E> {
        backing.build()
        return if (size > 0) this else Empty
    }

    private fun writeReplace(): Any =
        if (backing.isReadOnly)
            SerializedCollection(this, SerializedCollection.tagSet)
        else
            throw NotSerializableException("The set cannot be serialized while it is being built.")

    override val size: Int get() = backing.size
    override fun isEmpty(): Boolean = backing.isEmpty()
    override fun contains(element: E): Boolean = backing.containsKey(element)
    override fun clear() = backing.clear()
    override fun add(element: E): Boolean = backing.addKey(element) >= 0
    override fun remove(element: E): Boolean = backing.removeKey(element) >= 0
    override fun iterator(): MutableIterator<E> = backing.keysIterator()

    override fun addAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.addAll(elements)
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.removeAll(elements)
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        backing.checkIsMutable()
        return super.retainAll(elements)
    }
}