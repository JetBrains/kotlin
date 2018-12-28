/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

actual class ArrayList<E> private constructor(
        private var array: Array<E>,
        private var offset: Int,
        private var length: Int,
        private val backing: ArrayList<E>?
) : MutableList<E>, RandomAccess, AbstractMutableCollection<E>() {

    actual constructor() : this(10)

    actual constructor(initialCapacity: Int) : this(
            arrayOfUninitializedElements(initialCapacity), 0, 0, null)

    actual constructor(elements: Collection<E>) : this(elements.size) {
        addAll(elements)
    }

    override actual val size : Int
        get() = length

    override actual fun isEmpty(): Boolean = length == 0

    override actual fun get(index: Int): E {
        checkIndex(index)
        return array[offset + index]
    }

    override actual operator fun set(index: Int, element: E): E {
        checkIndex(index)
        val old = array[offset + index]
        array[offset + index] = element
        return old
    }

    override actual fun contains(element: E): Boolean {
        var i = 0
        while (i < length) {
            if (array[offset + i] == element) return true
            i++
        }
        return false
    }

    override actual fun containsAll(elements: Collection<E>): Boolean {
        val it = elements.iterator()
        while (it.hasNext()) {
            if (!contains(it.next()))return false
        }
        return true
    }

    override actual fun indexOf(element: E): Int {
        var i = 0
        while (i < length) {
            if (array[offset + i] == element) return i
            i++
        }
        return -1
    }

    override actual fun lastIndexOf(element: E): Int {
        var i = length - 1
        while (i >= 0) {
            if (array[offset + i] == element) return i
            i--
        }
        return -1
    }

    override actual fun iterator(): MutableIterator<E> = Itr(this, 0)
    override actual fun listIterator(): MutableListIterator<E> = Itr(this, 0)

    override actual fun listIterator(index: Int): MutableListIterator<E> {
        checkInsertIndex(index)
        return Itr(this, index)
    }

    override actual fun add(element: E): Boolean {
        addAtInternal(offset + length, element)
        return true
    }

    override actual fun add(index: Int, element: E) {
        checkInsertIndex(index)
        addAtInternal(offset + index, element)
    }

    override actual fun addAll(elements: Collection<E>): Boolean {
        val n = elements.size
        addAllInternal(offset + length, elements, n)
        return n > 0
    }

    override actual fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkInsertIndex(index)
        val n = elements.size
        addAllInternal(offset + index, elements, n)
        return n > 0
    }

    override actual fun clear() {
        removeRangeInternal(offset, length)
    }

    override actual fun removeAt(index: Int): E {
        checkIndex(index)
        return removeAtInternal(offset + index)
    }

    override actual fun remove(element: E): Boolean {
        val i = indexOf(element)
        if (i >= 0) removeAt(i)
        return i >= 0
    }

    override actual fun removeAll(elements: Collection<E>): Boolean {
        return retainOrRemoveAllInternal(offset, length, elements, false) > 0
    }

    override actual fun retainAll(elements: Collection<E>): Boolean {
        return retainOrRemoveAllInternal(offset, length, elements, true) > 0
    }

    override actual fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        checkInsertIndex(fromIndex)
        checkInsertIndexFrom(toIndex, fromIndex)
        return ArrayList(array, offset + fromIndex, toIndex - fromIndex, this)
    }

    actual fun trimToSize() {
        if (backing != null) throw IllegalStateException() // just in case somebody casts subList to ArrayList
        if (length < array.size)
            array = array.copyOfUninitializedElements(length)
    }

    final actual fun ensureCapacity(minCapacity: Int) {
        if (backing != null) throw IllegalStateException() // just in case somebody casts subList to ArrayList
        if (minCapacity > array.size) {
            var newSize = array.size * 3 / 2
            if (minCapacity > newSize)
                newSize = minCapacity
            array = array.copyOfUninitializedElements(newSize)
        }
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                (other is List<*>) && contentEquals(other)
    }

    override fun hashCode(): Int {
        var result = 1
        var i = 0
        while (i < length) {
            val nextElement = array[offset + i]
            val nextHash = if (nextElement != null) nextElement.hashCode() else 0
            result = result * 31 + nextHash
            i++
        }
        return result
    }

    override fun toString(): String {
        return this.array.subarrayContentToString(offset, length)
    }

    // ---------------------------- private ----------------------------

    private fun ensureExtraCapacity(n: Int) {
        ensureCapacity(length + n)
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= length) throw IndexOutOfBoundsException()
    }

    private fun checkInsertIndex(index: Int) {
        if (index < 0 || index > length) throw IndexOutOfBoundsException()
    }

    private fun checkInsertIndexFrom(index: Int, fromIndex: Int) {
        if (index < fromIndex || index > length) throw IndexOutOfBoundsException()
    }

    private fun contentEquals(other: List<*>): Boolean {
        if (length != other.size) return false
        var i = 0
        while (i < length) {
            if (array[offset + i] != other[i]) return false
            i++
        }
        return true
    }

    private fun insertAtInternal(i: Int, n: Int) {
        ensureExtraCapacity(n)
        array.copyInto(array, startIndex = i, endIndex = offset + length, destinationOffset = i + n)
        length += n
    }

    private fun addAtInternal(i: Int, element: E) {
        if (backing != null) {
            backing.addAtInternal(i, element)
            array = backing.array
            length++
        } else {
            insertAtInternal(i, 1)
            array[i] = element
        }
    }

    private fun addAllInternal(i: Int, elements: Collection<E>, n: Int) {
        if (backing != null) {
            backing.addAllInternal(i, elements, n)
            array = backing.array
            length += n
        } else {
            insertAtInternal(i, n)
            var j = 0
            val it = elements.iterator()
            while (j < n) {
                array[i + j] = it.next()
                j++
            }
        }
    }

    private fun removeAtInternal(i: Int): E {
        if (backing != null) {
            val old = backing.removeAtInternal(i)
            length--
            return old
        } else {
            val old = array[i]
            array.copyInto(array, startIndex = i + 1, endIndex = offset + length, destinationOffset = i)
            array.resetAt(offset + length - 1)
            length--
            return old
        }
    }

    private fun removeRangeInternal(rangeOffset: Int, rangeLength: Int) {
        if (backing != null) {
            backing.removeRangeInternal(rangeOffset, rangeLength)
        } else {
            array.copyInto(array, startIndex = rangeOffset + rangeLength, endIndex = length, destinationOffset = rangeOffset)
            array.resetRange(fromIndex = length - rangeLength, toIndex = length)
        }
        length -= rangeLength
    }

    /** Retains elements if [retain] == true and removes them it [retain] == false. */
    private fun retainOrRemoveAllInternal(rangeOffset: Int, rangeLength: Int, elements: Collection<E>, retain: Boolean): Int {
        if (backing != null) {
            val removed = backing.retainOrRemoveAllInternal(rangeOffset, rangeLength, elements, retain)
            length -= removed
            return removed
        } else {
            var i = 0
            var j = 0
            while (i < rangeLength) {
                if (elements.contains(array[rangeOffset + i]) == retain) {
                    array[rangeOffset + j++] = array[rangeOffset + i++]
                } else {
                    i++
                }
            }
            val removed = rangeLength - j
            array.copyInto(array, startIndex = rangeOffset + rangeLength, endIndex = length, destinationOffset = rangeOffset + j)
            array.resetRange(fromIndex = length - removed, toIndex = length)
            length -= removed
            return removed
        }
    }

    private class Itr<E> : MutableListIterator<E> {
        private val list: ArrayList<E>
        private var index: Int
        private var lastIndex: Int

        constructor(list: ArrayList<E>, index: Int) {
            this.list = list
            this.index = index
            this.lastIndex = -1
        }

        override fun hasPrevious(): Boolean = index > 0
        override fun hasNext(): Boolean = index < list.length

        override fun previousIndex(): Int = index - 1
        override fun nextIndex(): Int = index

        override fun previous(): E {
            if (index <= 0) throw NoSuchElementException()
            lastIndex = --index
            return list.array[list.offset + lastIndex]
        }

        override fun next(): E {
            if (index >= list.length) throw NoSuchElementException()
            lastIndex = index++
            return list.array[list.offset + lastIndex]
        }

        override fun set(element: E) {
            list.checkIndex(lastIndex)
            list.array[list.offset + lastIndex] = element
        }

        override fun add(element: E) {
            list.add(index++, element)
            lastIndex = -1
        }

        override fun remove() {
            check(lastIndex != -1) { "Call next() or previous() before removing element from the iterator." }
            list.removeAt(lastIndex)
            index = lastIndex
            lastIndex = -1
        }
    }
}
