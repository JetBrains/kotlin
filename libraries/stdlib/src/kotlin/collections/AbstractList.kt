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

import kotlin.*

/**
 * Provides a skeletal implementation of the read-only [List] interface.
 *
 * This class is intended to help implementing read-only lists so it doesn't support concurrent modification tracking.
 *
 * @param E the type of elements contained in the list. The list is covariant on its element type.
 */
@SinceKotlin("1.1")
public abstract class AbstractList<out E> protected constructor() : AbstractCollection<E>(), List<E> {
    abstract override val size: Int
    abstract override fun get(index: Int): E

    override fun iterator(): Iterator<E> = IteratorImpl()

    override fun indexOf(element: @UnsafeVariance E): Int = indexOfFirst { it == element }

    override fun lastIndexOf(element: @UnsafeVariance E): Int = indexOfLast { it == element }

    override fun listIterator(): ListIterator<E> = ListIteratorImpl(0)

    override fun listIterator(index: Int): ListIterator<E> = ListIteratorImpl(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<E> = SubList(this, fromIndex, toIndex)

    private class SubList<out E>(private val list: AbstractList<E>, private val fromIndex: Int, toIndex: Int) : AbstractList<E>(), RandomAccess {
        private var _size: Int = 0

        init {
            checkRangeIndexes(fromIndex, toIndex, list.size)
            this._size = toIndex - fromIndex
        }

        override fun get(index: Int): E {
            checkElementIndex(index, _size)

            return list[fromIndex + index]
        }

        override val size: Int get() = _size
    }

    /**
     * Compares this list with other list instance with the ordered structural equality.
     *
     * @return true, if [other] instance is a [List] of the same size, which contains the same elements in the same order.
     */
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is List<*>) return false

        return orderedEquals(this, other)
    }

    /**
     * Returns the hash code value for this list.
     */
    override fun hashCode(): Int = orderedHashCode(this)

    private open inner class IteratorImpl : Iterator<E> {
        /** the index of the item that will be returned on the next call to [next]`()` */
        protected var index = 0

        override fun hasNext(): Boolean = index < size

        override fun next(): E {
            if (!hasNext()) throw NoSuchElementException()
            return get(index++)
        }
    }

    /**
     * Implementation of [ListIterator] for abstract lists.
     */
    private open inner class ListIteratorImpl(index: Int) : IteratorImpl(), ListIterator<E> {

        init {
            checkPositionIndex(index, this@AbstractList.size)
            this.index = index
        }

        override fun hasPrevious(): Boolean = index > 0

        override fun nextIndex(): Int = index

        override fun previous(): E {
            if (!hasPrevious()) throw NoSuchElementException()
            return get(--index)
        }

        override fun previousIndex(): Int = index - 1
    }

    internal companion object {
        internal fun checkElementIndex(index: Int, size: Int) {
            if (index < 0 || index >= size) {
                throw IndexOutOfBoundsException("index: $index, size: $size")
            }
        }

        internal fun checkPositionIndex(index: Int, size: Int) {
            if (index < 0 || index > size) {
                throw IndexOutOfBoundsException("index: $index, size: $size")
            }
        }

        internal fun checkRangeIndexes(fromIndex: Int, toIndex: Int, size: Int) {
            if (fromIndex < 0 || toIndex > size) {
                throw IndexOutOfBoundsException("fromIndex: $fromIndex, toIndex: $toIndex, size: $size")
            }
            if (fromIndex > toIndex) {
                throw IllegalArgumentException("fromIndex: $fromIndex > toIndex: $toIndex")
            }
        }

        internal fun orderedHashCode(c: Collection<*>): Int {
            var hashCode = 1
            for (e in c) {
                hashCode = 31 * hashCode + (e?.hashCode() ?: 0)
            }
            return hashCode
        }

        internal fun orderedEquals(c: Collection<*>, other: Collection<*>): Boolean {
            if (c.size != other.size) return false

            val otherIterator = other.iterator()
            for (elem in c) {
                val elemOther = otherIterator.next()
                if (elem != elemOther) {
                    return false
                }
            }
            return true
        }
    }
}