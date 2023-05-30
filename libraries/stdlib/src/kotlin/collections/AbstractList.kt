/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Based on GWT AbstractList
 * Copyright 2007 Google Inc.
*/

package kotlin.collections

/**
 * Provides a skeletal implementation of the read-only [List] interface.
 *
 * This class is intended to help implementing read-only lists so it doesn't support concurrent modification tracking.
 *
 * @param E the type of elements contained in the list. The list is covariant in its element type.
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

        internal fun checkBoundsIndexes(startIndex: Int, endIndex: Int, size: Int) {
            if (startIndex < 0 || endIndex > size) {
                throw IndexOutOfBoundsException("startIndex: $startIndex, endIndex: $endIndex, size: $size")
            }
            if (startIndex > endIndex) {
                throw IllegalArgumentException("startIndex: $startIndex > endIndex: $endIndex")
            }
        }

        private const val maxArraySize = Int.MAX_VALUE - 8

        /** [oldCapacity] and [minCapacity] must be non-negative. */
        internal fun newCapacity(oldCapacity: Int, minCapacity: Int): Int {
            // overflow-conscious
            var newCapacity = oldCapacity + (oldCapacity shr 1)
            if (newCapacity - minCapacity < 0)
                newCapacity = minCapacity
            if (newCapacity - maxArraySize > 0)
                newCapacity = if (minCapacity > maxArraySize) Int.MAX_VALUE else maxArraySize
            return newCapacity
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