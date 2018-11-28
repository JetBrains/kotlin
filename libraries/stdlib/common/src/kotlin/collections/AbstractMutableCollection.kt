/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Provides a skeletal implementation of the [MutableCollection] interface.
 *
 * @param E the type of elements contained in the collection. The collection is invariant on its element type.
 */
@SinceKotlin("1.3")
public expect abstract class AbstractMutableCollection<E> : MutableCollection<E> {
    protected constructor()

    abstract override val size: Int
    abstract override fun iterator(): MutableIterator<E>
    abstract override fun add(element: E): Boolean

    override fun isEmpty(): Boolean
    override fun contains(element: @UnsafeVariance E): Boolean
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean


    override fun addAll(elements: Collection<E>): Boolean
    override fun remove(element: E): Boolean
    override fun removeAll(elements: Collection<E>): Boolean
    override fun retainAll(elements: Collection<E>): Boolean
    override fun clear(): Unit
}

