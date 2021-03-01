/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.collections

/**
 * AbstractMutableList implementation copied from JS backend
 * (see <Kotlin JVM root>js/js.libraries/src/core/collections/AbstractMutableList.kt).
 *
 *  Based on GWT AbstractList
 *  Copyright 2007 Google Inc.
 */

/**
 * Provides a skeletal implementation of the [MutableList] interface.
 *
 * @param E the type of elements contained in the list. The list is invariant in its element type.
 */
public actual abstract class AbstractMutableList<E> protected actual constructor() : AbstractMutableCollection<E>(), MutableList<E> {
    protected var modCount: Int = 0

    abstract override fun add(index: Int, element: E): Unit
    abstract override fun removeAt(index: Int): E
    abstract override fun set(index: Int, element: E): E

    /**
     * Adds the specified element to the end of this list.
     *
     * @return `true` because the list is always modified as the result of this operation.
     */
    override actual fun add(element: E): Boolean {
        add(size, element)
        return true
    }

    override actual fun addAll(index: Int, elements: Collection<E>): Boolean {
        var i = index
        var changed = false
        for (e in elements) {
            add(i++, e)
            changed = true
        }
        return changed
    }

    override actual fun clear() {
        removeRange(0, size)
    }

    override actual fun removeAll(elements: Collection<E>): Boolean = removeAll { it in elements }
    override actual fun retainAll(elements: Collection<E>): Boolean = removeAll { it !in elements }


    override actual fun iterator(): MutableIterator<E> = IteratorImpl()

    override actual fun contains(element: E): Boolean = indexOf(element) >= 0

    override actual fun indexOf(element: E): Int {
        for (index in 0..lastIndex) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override actual fun lastIndexOf(element: E): Int {
        for (index in lastIndex downTo 0) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override actual fun listIterator(): MutableListIterator<E> = listIterator(0)
    override actual fun listIterator(index: Int): MutableListIterator<E> = ListIteratorImpl(index)


    override actual fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = SubList(this, fromIndex, toIndex)

    /**
     * Removes the range of elements from this list starting from [fromIndex] and ending with but not including [toIndex].
     */
    protected open fun removeRange(fromIndex: Int, toIndex: Int) {
        val iterator = listIterator(fromIndex)
        repeat(toIndex - fromIndex) {
            iterator.next()
            iterator.remove()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is List<*>) return false

        return AbstractList.orderedEquals(this, other)
    }

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
            check(last != -1) { "Call next() or previous() before removing element from the iterator."}

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
            check(last != -1) { "Call next() or previous() before updating element value with the iterator."}
            this@AbstractMutableList[last] = element
        }
    }

    private class SubList<E>(private val list: AbstractMutableList<E>, private val fromIndex: Int, toIndex: Int) : AbstractMutableList<E>() {
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

        override val size: Int get() = _size
    }

}