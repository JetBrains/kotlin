/*
 * Based on GWT AbstractList
 * Copyright 2007 Google Inc.
*/
package kotlin.collections

public abstract class AbstractList<out E> protected constructor() : AbstractCollection<E>(), List<E> {
    abstract override val size: Int
    abstract override fun get(index: Int): E

    override fun iterator(): Iterator<E> = IteratorImpl()

    override fun indexOf(element: @UnsafeVariance E): Int = indexOfFirst { it == element }

    override fun lastIndexOf(element: @UnsafeVariance E): Int = indexOfLast { it == element }

    override fun listIterator(): ListIterator<E> = ListIteratorImpl(0)

    override fun listIterator(index: Int): ListIterator<E> = ListIteratorImpl(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<E> = SubList(this, fromIndex, toIndex)

    internal open class SubList<out E>(
            private val list: AbstractList<E>, private val fromIndex: Int, toIndex: Int) : AbstractList<E>() {
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

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is List<*>) return false

        return orderedEquals(this, other)
    }

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
     * Implementation of `MutableListIterator` for abstract lists.
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

        internal fun checkRangeIndexes(start: Int, end: Int, size: Int) {
            if (start < 0 || end > size) {
                throw IndexOutOfBoundsException("fromIndex: $start, toIndex: $end, size: $size")
            }
            if (start > end) {
                throw IllegalArgumentException("fromIndex: $start > toIndex: $end")
            }
        }

        internal fun orderedHashCode(c: Collection<*>): Int {
            var hashCode = 1
            for (e in c) {
                hashCode = 31 * hashCode + (if (e != null) e.hashCode() else 0)
                hashCode = hashCode or 0 // make sure we don't overflow
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