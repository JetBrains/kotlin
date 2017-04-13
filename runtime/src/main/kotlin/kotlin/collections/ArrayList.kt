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

package kotlin.collections

class ArrayList<E> private constructor(
        private var array: Array<E>,
        private var offset: Int,
        private var length: Int,
        private val backing: ArrayList<E>?
) : MutableList<E>, RandomAccess {

    constructor() : this(10)

    constructor(initialCapacity: Int) : this(
            arrayOfUninitializedElements(initialCapacity), 0, 0, null)

    constructor(c: Collection<E>) : this(c.size) {
        addAll(c)
    }

    override val size : Int
        get() = length

    override fun isEmpty(): Boolean = length == 0

    override fun get(index: Int): E {
        checkIndex(index)
        return array[offset + index]
    }

    override fun set(index: Int, element: E): E {
        checkIndex(index)
        val old = array[offset + index]
        array[offset + index] = element
        return old
    }

    override fun contains(element: E): Boolean {
        var i = 0
        while (i < length) {
            if (array[offset + i] == element) return true
            i++
        }
        return false
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        val it = elements.iterator()
        while (it.hasNext()) {
            if (!contains(it.next()))return false
        }
        return true
    }

    override fun indexOf(element: E): Int {
        var i = 0
        while (i < length) {
            if (array[offset + i] == element) return i
            i++
        }
        return -1
    }

    override fun lastIndexOf(element: E): Int {
        var i = length - 1
        while (i >= 0) {
            if (array[offset + i] == element) return i
            i--
        }
        return -1
    }

    override fun iterator(): MutableIterator<E> = Itr(this, 0)
    override fun listIterator(): MutableListIterator<E> = Itr(this, 0)

    override fun listIterator(index: Int): MutableListIterator<E> {
        checkInsertIndex(index)
        return Itr(this, index)
    }

    override fun add(element: E): Boolean {
        addAtInternal(offset + length, element)
        return true
    }

    override fun add(index: Int, element: E) {
        checkInsertIndex(index)
        addAtInternal(offset + index, element)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        val n = elements.size
        addAllInternal(offset + length, elements, n)
        return n > 0
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkInsertIndex(index)
        val n = elements.size
        addAllInternal(offset + index, elements, n)
        return n > 0
    }

    override fun clear() {
        removeRangeInternal(offset, length)
    }

    override fun removeAt(index: Int): E {
        checkIndex(index)
        return removeAtInternal(offset + index)
    }

    override fun remove(element: E): Boolean {
        val i = indexOf(element)
        if (i >= 0) removeAt(i)
        return i >= 0
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        var changed = false
        val it = iterator()
        while (it.hasNext()) {
            if (elements.contains(it.next())) {
                it.remove()
                changed = true
            }
        }
        return changed
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        return retainAllInRangeInternal(offset, length, elements) > 0
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        checkInsertIndex(fromIndex)
        checkInsertIndexFrom(toIndex, fromIndex)
        return ArrayList(array, offset + fromIndex, toIndex - fromIndex, this)
    }

    fun trimToSize() {
        if (backing != null) throw IllegalStateException() // just in case somebody casts subList to ArrayList
        if (length < array.size)
            array = array.copyOfUninitializedElements(length)
    }

    fun ensureCapacity(capacity: Int) {
        if (backing != null) throw IllegalStateException() // just in case somebody casts subList to ArrayList
        if (capacity > array.size) {
            var newSize = array.size * 3 / 2
            if (capacity > newSize)
                newSize = capacity
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
        array.copyRange(fromIndex = i, toIndex = offset + length, destinationIndex = i + n)
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
            array.copyRange(fromIndex = i + 1, toIndex = offset + length, destinationIndex = i)
            array.resetAt(offset + length - 1)
            length--
            return old
        }
    }

    private fun removeRangeInternal(rangeOffset: Int, rangeLength: Int) {
        if (backing != null) {
            backing.removeRangeInternal(rangeOffset, rangeLength)
        } else {
            array.copyRange(fromIndex = rangeOffset + rangeLength, toIndex = length, destinationIndex = rangeOffset)
            array.resetRange(fromIndex = length - rangeLength, toIndex = length)
        }
        length -= rangeLength
    }

    private fun retainAllInRangeInternal(rangeOffset: Int, rangeLength: Int, elements: Collection<E>): Int {
        if (backing != null) {
            val removed = backing.retainAllInRangeInternal(rangeOffset, rangeLength, elements)
            length -= removed
            return removed
        } else {
            var i = 0
            var j = 0
            while (i < rangeLength) {
                if (elements.contains(array[rangeOffset + i])) {
                    array[rangeOffset + j++] = array[rangeOffset + i++]
                } else {
                    i++
                }
            }
            val removed = rangeLength - j
            array.copyRange(fromIndex = rangeOffset + rangeLength, toIndex = length, destinationIndex = rangeOffset + j)
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
            if (index <= 0) throw IndexOutOfBoundsException()
            lastIndex = --index
            return list.array[list.offset + lastIndex]
        }

        override fun next(): E {
            if (index >= list.length) throw IndexOutOfBoundsException()
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
            list.removeAt(lastIndex)
            index = lastIndex
            lastIndex = -1
        }
    }
}
