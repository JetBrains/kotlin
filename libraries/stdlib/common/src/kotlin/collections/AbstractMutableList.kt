/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Provides a skeletal implementation of the [MutableList] interface.
 *
 * @param E the type of elements contained in the list. The list is invariant on its element type.
 */
public expect abstract class AbstractMutableList<E> : MutableList<E> {
    protected constructor()

    // From List

    override fun isEmpty(): Boolean
    override fun contains(element: @UnsafeVariance E): Boolean
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
    override fun indexOf(element: @UnsafeVariance E): Int
    override fun lastIndexOf(element: @UnsafeVariance E): Int

    // From MutableCollection

    override fun iterator(): MutableIterator<E>

    // From MutableList

    /**
     * Adds the specified element to the end of this list.
     *
     * @return `true` because the list is always modified as the result of this operation.
     */
    override fun add(element: E): Boolean
    override fun remove(element: E): Boolean
    override fun addAll(elements: Collection<E>): Boolean
    override fun addAll(index: Int, elements: Collection<E>): Boolean
    override fun removeAll(elements: Collection<E>): Boolean
    override fun retainAll(elements: Collection<E>): Boolean
    override fun clear()
    override fun listIterator(): MutableListIterator<E>
    override fun listIterator(index: Int): MutableListIterator<E>
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>
}