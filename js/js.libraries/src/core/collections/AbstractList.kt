/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

public abstract class AbstractList<E> protected constructor() : AbstractCollection<E>(), MutableList<E> {
    abstract override val size: Int
    abstract override fun get(index: Int): E

    protected var modCount: Int = 0

    override fun add(index: Int, element: E): Unit = throw UnsupportedOperationException("Add not supported on this list")
    override fun removeAt(index: Int): E = throw UnsupportedOperationException("Remove not supported on this list")
    override fun set(index: Int, element: E): E = throw UnsupportedOperationException("Set not supported on this list")

    override fun add(element: E): Boolean {
        add(size, element)
        return true
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        var index = index
        var changed = false
        for (e in elements) {
            add(index++, e)
            changed = true
        }
        return changed
    }

    override fun clear() {
        removeRange(0, size)
    }


    override fun iterator(): MutableIterator<E> = IteratorImpl()

    override fun indexOf(element: E): Int {
        for (i in 0..lastIndex) {
            if (get(i) == element) {
                return i
            }
        }
        return -1
    }

    override fun lastIndexOf(element: E): Int {
        for (i in lastIndex downTo 0) {
            if (get(i) == element) {
                return i
            }
        }
        return -1
    }

    override fun listIterator(): MutableListIterator<E> = listIterator(0)
    override fun listIterator(from: Int): MutableListIterator<E> = ListIteratorImpl(from)


    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = SubList(this, fromIndex, toIndex)

    protected open fun removeRange(fromIndex: Int, endIndex: Int) {
        val iterator = listIterator(fromIndex)
        for (i in fromIndex..endIndex - 1) {
            iterator.next()
            iterator.remove()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is List<*>) return false
        if (size != other.size) return false

        val otherIterator = other.iterator()
        for (elem in this) {
            val elemOther = otherIterator.next()
            if (elem != elemOther) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        return Collections_hashCode(this)
    }


    private open inner class IteratorImpl : MutableIterator<E> {
        /*
     * i is the index of the item that will be returned on the next call to
     * next() last is the index of the item that was returned on the previous
     * call to next() or previous (for ListIterator), -1 if no such item exists.
     */

        internal var i = 0
        internal var last = -1

        override fun hasNext(): Boolean = i < size

        override fun next(): E {
            if (!hasNext()) throw NoSuchElementException()
            last = i++
            return get(last)
        }

        override fun remove() {
            check(last != -1)

            removeAt(last)
            i = last
            last = -1
        }
    }

    /**
     * Implementation of `ListIterator` for abstract lists.
     */
    private inner class ListIteratorImpl(start: Int) : IteratorImpl(), MutableListIterator<E> {
        /*
     * i is the index of the item that will be returned on the next call to
     * next() last is the index of the item that was returned on the previous
     * call to next() or previous (for ListIterator), -1 if no such item exists.
     */

        init {
            checkPositionIndex(start, this@AbstractList.size)

            i = start
        }

        override fun add(o: E) {
            add(i, o)
            i++
            last = -1
        }

        override fun hasPrevious(): Boolean = i > 0

        override fun nextIndex(): Int = i

        override fun previous(): E {
            if (!hasPrevious()) throw NoSuchElementException()

            last = --i
            return get(last)
        }

        override fun previousIndex(): Int = i - 1

        override fun set(o: E) {
            require(last != -1)

            this@AbstractList[last] = o
        }
    }

    private class SubList<E>(private val wrapped: AbstractList<E>, private val fromIndex: Int, toIndex: Int) : AbstractList<E>() {
        private var _size: Int = 0

        init {
            checkCriticalPositionIndexes(fromIndex, toIndex, wrapped.size)
            this._size = toIndex - fromIndex
        }

        override fun add(index: Int, element: E) {
            checkPositionIndex(index, _size)

            wrapped.add(fromIndex + index, element)
            _size++
        }

        override fun get(index: Int): E {
            checkElementIndex(index, _size)

            return wrapped[fromIndex + index]
        }

        override fun removeAt(index: Int): E {
            checkElementIndex(index, _size)

            val result = wrapped.removeAt(fromIndex + index)
            _size--
            return result
        }

        override fun set(index: Int, element: E): E {
            checkElementIndex(index, _size)

            return wrapped.set(fromIndex + index, element)
        }

        override val size: Int get() = _size
    }

    companion object {
        internal fun checkElementIndex(index: Int, size: Int) {
            if (index < 0 || index >= size) {
                throw IndexOutOfBoundsException("Index: $index, Size: $size")
            }
        }

        internal fun checkPositionIndex(index: Int, size: Int) {
            if (index < 0 || index > size) {
                throw IndexOutOfBoundsException("Index: $index, Size: $size")
            }
        }

        internal fun checkCriticalPositionIndexes(start: Int, end: Int, size: Int) {
            if (start < 0 || end > size) {
                throw IndexOutOfBoundsException("fromIndex: $start, toIndex: $end, size: $size")
            }
            if (start > end) {
                throw IllegalArgumentException("fromIndex: $start > toIndex: $end")
            }
        }

        internal fun <T> Collections_hashCode(list: List<T>): Int {
            var hashCode = 1
            for (e in list) {
                hashCode = 31 * hashCode + (e?.hashCode() ?: 0)
                hashCode = hashCode or 0 // make sure we don't overflow
            }
            return hashCode
        }
    }

}
