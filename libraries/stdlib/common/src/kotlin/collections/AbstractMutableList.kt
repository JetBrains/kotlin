/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Provides a skeletal implementation of the [MutableList] interface.
 *
 * @param E the type of elements contained in the list. The list is invariant in its element type.
 */
public expect abstract class AbstractMutableList<E> : MutableList<E> {
    /**
     * The number of times this list is structurally modified.
     *
     * A modification is considered to be structural if it changes the list size,
     * or otherwise changes it in a way that iterations in progress may return incorrect results.
     *
     * This value can be used by iterators returned by [iterator] and [listIterator]
     * to provide fail-fast behavior when a concurrent modification is detected during iteration.
     * [ConcurrentModificationException] will be thrown in this case.
     */
    @SinceKotlin("2.0")
    protected var modCount: Int

    protected constructor()

    /**
     * Removes the range of elements from this list starting from [fromIndex] and ending with but not including [toIndex].
     */
    @SinceKotlin("2.0")
    protected open fun removeRange(fromIndex: Int, toIndex: Int): Unit

    // From List

    override fun isEmpty(): Boolean
    override fun contains(element: E): Boolean
    override fun containsAll(elements: Collection<E>): Boolean
    override fun indexOf(element: E): Int
    override fun lastIndexOf(element: E): Int

    // From MutableCollection

    override fun iterator(): MutableIterator<E>

    // From MutableList

    /**
     * Adds the specified element to the end of this list.
     *
     * @return `true` because the list is always modified as the result of this operation.
     */
    @IgnorableReturnValue
    override fun add(element: E): Boolean
    @IgnorableReturnValue
    override fun remove(element: E): Boolean
    @IgnorableReturnValue
    override fun addAll(elements: Collection<E>): Boolean
    @IgnorableReturnValue
    override fun addAll(index: Int, elements: Collection<E>): Boolean
    @IgnorableReturnValue
    override fun removeAll(elements: Collection<E>): Boolean
    @IgnorableReturnValue
    override fun retainAll(elements: Collection<E>): Boolean
    override fun clear()
    override fun listIterator(): MutableListIterator<E>
    override fun listIterator(index: Int): MutableListIterator<E>
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>
}
