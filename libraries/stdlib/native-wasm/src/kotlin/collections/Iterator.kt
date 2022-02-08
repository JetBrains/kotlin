/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

/**
 * An iterator over a collection or another entity that can be represented as a sequence of elements.
 * Allows to sequentially access the elements.
 */
public interface Iterator<out T> {
    /**
     * Returns the next element in the iteration.
     */
    public operator fun next(): T

    /**
     * Returns `true` if the iteration has more elements.
     */
    public operator fun hasNext(): Boolean
}

/**
 * An iterator over a mutable collection. Provides the ability to remove elements while iterating.
 * @see MutableCollection.iterator
 */
public interface MutableIterator<out T> : Iterator<T> {
    /**
     * Removes from the underlying collection the last element returned by this iterator.
     */
    public fun remove(): Unit
}

/**
 * An iterator over a collection that supports indexed access.
 * @see List.listIterator
 */
public interface ListIterator<out T> : Iterator<T> {
    // Query Operations
    override fun next(): T
    override fun hasNext(): Boolean

    /**
     * Returns `true` if there are elements in the iteration before the current element.
     */
    public fun hasPrevious(): Boolean

    /**
     * Returns the previous element in the iteration and moves the cursor position backwards.
     */
    public fun previous(): T

    /**
     * Returns the index of the element that would be returned by a subsequent call to [next].
     */
    public fun nextIndex(): Int

    /**
     * Returns the index of the element that would be returned by a subsequent call to [previous].
     */
    public fun previousIndex(): Int
}

/**
 * An iterator over a mutable collection that supports indexed access. Provides the ability
 * to add, modify and remove elements while iterating.
 */
public interface MutableListIterator<T> : ListIterator<T>, MutableIterator<T> {
    // Query Operations
    override fun next(): T
    override fun hasNext(): Boolean

    // Modification Operations
    override fun remove(): Unit

    /**
     * Replaces the last element returned by [next] or [previous] with the specified element [element].
     */
    public fun set(element: T): Unit

    /**
     * Adds the specified element [element] into the underlying collection immediately before the element that would be
     * returned by [next], if any, and after the element that would be returned by [previous], if any.
     * (If the collection contains no elements, the new element becomes the sole element in the collection.)
     * The new element is inserted before the implicit cursor: a subsequent call to [next] would be unaffected,
     * and a subsequent call to [previous] would return the new element. (This call increases by one the value \
     * that would be returned by a call to [nextIndex] or [previousIndex].)
     */
    public fun add(element: T): Unit
}
