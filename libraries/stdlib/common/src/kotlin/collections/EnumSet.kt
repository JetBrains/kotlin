/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * A specialized [Set] for enum types those represented internally as
 * bitmap (inaccurately speaking, bit-set). Bitmap is more compact and efficient than
 * normal hash or binary search tree when a lot of read and write operations. It slower
 * than `SingletonSet`, but `SingletonSet` is immutable and only have single element.
 *
 * All bulk operations should run very fast if their argument is also an [EnumSet],
 * like [addAll], [containsAll] and [retainAll].
 *
 * It is NOT thread-safety.
 */
expect abstract class EnumSet<E : Enum<E>> : MutableSet<E> {

    // From Set

    abstract override val size: Int
    abstract override fun isEmpty(): Boolean
    abstract override fun contains(element: @UnsafeVariance E): Boolean
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean

    // From MutableSet

    abstract override fun iterator(): MutableIterator<E>
    abstract override fun add(element: E): Boolean
    abstract override fun remove(element: E): Boolean
    override fun addAll(elements: Collection<E>): Boolean
    override fun removeAll(elements: Collection<E>): Boolean
    override fun retainAll(elements: Collection<E>): Boolean
    abstract override fun clear()
}
