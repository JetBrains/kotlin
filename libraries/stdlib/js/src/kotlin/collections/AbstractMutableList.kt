/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Based on GWT AbstractList
 * Copyright 2007 Google Inc.
*/

@file:JsFileName("AbstractMutableListJs")

package kotlin.collections

/**
 * Provides a skeletal implementation of the [MutableList] interface.
 *
 * @param E the type of elements contained in the list. The list is invariant in its element type.
 */
public actual abstract class AbstractMutableList<E> protected actual constructor() : AbstractMutableCollection<E>(), MutableList<E> {
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
    protected actual var modCount: Int = 0

    abstract override fun add(index: Int, element: E): Unit
    @IgnorableReturnValue
    abstract override fun removeAt(index: Int): E
    @IgnorableReturnValue
    abstract override fun set(index: Int, element: E): E

    /**
     * Adds the specified element to the end of this list.
     *
     * @return `true` because the list is always modified as the result of this operation.
     */
    @IgnorableReturnValue
    actual override fun add(element: E): Boolean {
        checkIsMutable()
        add(size, element)
        return true
    }

    @IgnorableReturnValue
    actual override fun addAll(index: Int, elements: Collection<E>): Boolean {
        AbstractList.checkPositionIndex(index, size)

        checkIsMutable()
        var _index = index
        var changed = false
        for (e in elements) {
            add(_index++, e)
            changed = true
        }
        return changed
    }

    actual override fun clear() {
        checkIsMutable()
        removeRange(0, size)
    }

    @IgnorableReturnValue
    actual override fun removeAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        return removeAll { it in elements }
    }

    @IgnorableReturnValue
    actual override fun retainAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        return removeAll { it !in elements }
    }


    actual override fun iterator(): MutableIterator<E> = IteratorImpl()

    actual override fun contains(element: E): Boolean = indexOf(element) >= 0

    actual override fun indexOf(element: E): Int = indexOfFirst { it == element }

    actual override fun lastIndexOf(element: E): Int = indexOfLast { it == element }

    actual override fun listIterator(): MutableListIterator<E> = listIterator(0)
    actual override fun listIterator(index: Int): MutableListIterator<E> = ListIteratorImpl(index)


    actual override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = SubList(this, fromIndex, toIndex)

    /**
     * Removes the range of elements from this list starting from [fromIndex] and ending with but not including [toIndex].
     */
    protected actual open fun removeRange(fromIndex: Int, toIndex: Int) {
        val iterator = listIterator(fromIndex)
        repeat(toIndex - fromIndex) {
            val _ = iterator.next()
            iterator.remove()
        }
    }

    /**
     * Checks if the two specified lists are *structurally* equal to one another.
     *
     * Two lists are considered structurally equal if they have the same size, and elements at corresponding indices are equal.
     * Elements are compared for equality using the [equals][Any.equals] function.
     * For floating point numbers, this means `NaN` is equal to itself and `-0.0` is not equal to `0.0`.
     *
     * @param other the list to compare with this list.
     * @return `true` if [other] is a [List] that is structurally equal to this list, `false` otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is List<*>) return false

        return AbstractList.orderedEquals(this, other)
    }

    /**
     * Returns the hash code value for this list.
     */
    override fun hashCode(): Int = AbstractList.orderedHashCode(this)


    private open inner class IteratorImpl : MutableIterator<E> {
        /** the index of the item that will be returned on the next call to [next]`()` */
        protected var index = 0
        /** the index of the item that was returned on the previous call to [next]`()`
         * or [ListIterator.previous]`()` (for `ListIterator`),
         * -1 if no such item exists
         */
        protected var last = -1

        override fun hasNext(): Boolean = index < size

        override fun next(): E {
            if (!hasNext()) throw NoSuchElementException()
            last = index++
            return get(last)
        }

        override fun remove() {
            check(last != -1) { "Call next() or previous() before removing element from the iterator." }

            removeAt(last)
            index = last
            last = -1
        }
    }

    /**
     * Implementation of `MutableListIterator` for abstract lists.
     */
    private inner class ListIteratorImpl(index: Int) : IteratorImpl(), MutableListIterator<E> {

        init {
            AbstractList.checkPositionIndex(index, this@AbstractMutableList.size)
            this.index = index
        }

        override fun hasPrevious(): Boolean = index > 0

        override fun nextIndex(): Int = index

        override fun previous(): E {
            if (!hasPrevious()) throw NoSuchElementException()

            last = --index
            return get(last)
        }

        override fun previousIndex(): Int = index - 1

        override fun add(element: E) {
            add(index, element)
            index++
            last = -1
        }

        override fun set(element: E) {
            check(last != -1) { "Call next() or previous() before updating element value with the iterator." }
            set(last, element)
        }
    }

    private class SubList<E>(private val list: AbstractMutableList<E>, private val fromIndex: Int, toIndex: Int) : AbstractMutableList<E>(), RandomAccess {
        private var _size: Int = 0

        init {
            AbstractList.checkRangeIndexes(fromIndex, toIndex, list.size)
            this._size = toIndex - fromIndex
        }

        override fun add(index: Int, element: E) {
            AbstractList.checkPositionIndex(index, _size)

            list.add(fromIndex + index, element)
            _size++
        }

        override fun get(index: Int): E {
            AbstractList.checkElementIndex(index, _size)

            return list[fromIndex + index]
        }

        override fun removeAt(index: Int): E {
            AbstractList.checkElementIndex(index, _size)

            val result = list.removeAt(fromIndex + index)
            _size--
            return result
        }

        override fun set(index: Int, element: E): E {
            AbstractList.checkElementIndex(index, _size)

            return list.set(fromIndex + index, element)
        }

        override fun removeRange(fromIndex: Int, toIndex: Int) {
            list.removeRange(this.fromIndex + fromIndex, this.fromIndex + toIndex)
            _size -= toIndex - fromIndex
        }

        override val size: Int get() = _size

        internal override fun checkIsMutable(): Unit = list.checkIsMutable()
    }

}
