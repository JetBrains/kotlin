/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Based on GWT AbstractList
 * Copyright 2007 Google Inc.
*/


package kotlin.collections

/**
 * Provides a skeletal implementation of the [MutableList] interface.
 *
 * @param E the type of elements contained in the list. The list is invariant on its element type.
 */
public abstract class AbstractMutableList<E> protected constructor() : AbstractMutableCollection<E>(), MutableList<E> {
    protected var modCount: Int = 0

    abstract override fun add(index: Int, element: E): Unit
    abstract override fun removeAt(index: Int): E
    abstract override fun set(index: Int, element: E): E

    override fun add(element: E): Boolean {
        add(size, element)
        return true
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        var _index = index
        var changed = false
        for (e in elements) {
            add(_index++, e)
            changed = true
        }
        return changed
    }

    override fun clear() {
        removeRange(0, size)
    }

    override fun removeAll(elements: Collection<E>): Boolean = removeAll { it in elements }
    override fun retainAll(elements: Collection<E>): Boolean = removeAll { it !in elements }


    override fun iterator(): MutableIterator<E> = IteratorImpl()

    override fun contains(element: E): Boolean = indexOf(element) >= 0

    override fun indexOf(element: E): Int {
        for (index in 0..lastIndex) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override fun lastIndexOf(element: E): Int {
        for (index in lastIndex downTo 0) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override fun listIterator(): MutableListIterator<E> = listIterator(0)
    override fun listIterator(index: Int): MutableListIterator<E> = ListIteratorImpl(index)


    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = SubList(this, fromIndex, toIndex)

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

    /**
     * Compares this list with another list instance with the ordered structural equality.
     *
     * @return true, if [other] instance is a [List] of the same size, which contains the same elements in the same order.
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

        override val size: Int get() = _size
    }

}
