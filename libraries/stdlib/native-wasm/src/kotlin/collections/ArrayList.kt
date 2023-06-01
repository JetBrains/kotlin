/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

actual class ArrayList<E> private constructor(
    private var backingArray: Array<E>,
    private var offset: Int,
    private var length: Int,
    private var isReadOnly: Boolean,
    private val backingList: ArrayList<E>?,
    private val root: ArrayList<E>?
) : MutableList<E>, RandomAccess, AbstractMutableList<E>() {
    private companion object {
        private val Empty = ArrayList<Nothing>(0).also { it.isReadOnly = true }
    }

    init {
        if (backingList != null) this.modCount = backingList.modCount
    }

    /**
     * Creates a new empty [ArrayList].
     */
    actual constructor() : this(10)

    /**
     * Creates a new empty [ArrayList] with the specified initial capacity.
     *
     * Capacity is the maximum number of elements the list is able to store in current backing storage.
     * When the list gets full and a new element can't be added, its capacity is expanded,
     * which usually leads to creation of a bigger backing storage.
     *
     * @param initialCapacity the initial capacity of the created list.
     *   Note that the argument is just a hint for the implementation and can be ignored.
     *
     * @throws IllegalArgumentException if [initialCapacity] is negative.
     */
    actual constructor(initialCapacity: Int) : this(
            arrayOfUninitializedElements(initialCapacity), 0, 0, false, null, null)

    /**
     * Creates a new [ArrayList] filled with the elements of the specified collection.
     *
     * The iteration order of elements in the created list is the same as in the specified collection.
     */
    actual constructor(elements: Collection<E>) : this(elements.size) {
        addAll(elements)
    }

    @PublishedApi
    internal fun build(): List<E> {
        if (backingList != null) throw IllegalStateException() // just in case somebody casts subList to ArrayList
        checkIsMutable()
        isReadOnly = true
        return if (length > 0) this else Empty
    }

    override actual val size: Int
        get() {
            checkForComodification()
            return length
        }

    override actual fun isEmpty(): Boolean {
        checkForComodification()
        return length == 0
    }

    override actual fun get(index: Int): E {
        checkForComodification()
        AbstractList.checkElementIndex(index, length)
        return backingArray[offset + index]
    }

    override actual operator fun set(index: Int, element: E): E {
        checkIsMutable()
        checkForComodification()
        AbstractList.checkElementIndex(index, length)
        val old = backingArray[offset + index]
        backingArray[offset + index] = element
        return old
    }

    override actual fun indexOf(element: E): Int {
        checkForComodification()
        var i = 0
        while (i < length) {
            if (backingArray[offset + i] == element) return i
            i++
        }
        return -1
    }

    override actual fun lastIndexOf(element: E): Int {
        checkForComodification()
        var i = length - 1
        while (i >= 0) {
            if (backingArray[offset + i] == element) return i
            i--
        }
        return -1
    }

    override actual fun iterator(): MutableIterator<E> = listIterator(0)
    override actual fun listIterator(): MutableListIterator<E> = listIterator(0)

    override actual fun listIterator(index: Int): MutableListIterator<E> {
        checkForComodification()
        AbstractList.checkPositionIndex(index, length)
        return Itr(this, index)
    }

    override actual fun add(element: E): Boolean {
        checkIsMutable()
        checkForComodification()
        addAtInternal(offset + length, element)
        return true
    }

    override actual fun add(index: Int, element: E) {
        checkIsMutable()
        checkForComodification()
        AbstractList.checkPositionIndex(index, length)
        addAtInternal(offset + index, element)
    }

    override actual fun addAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        checkForComodification()
        val n = elements.size
        addAllInternal(offset + length, elements, n)
        return n > 0
    }

    override actual fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkIsMutable()
        checkForComodification()
        AbstractList.checkPositionIndex(index, length)
        val n = elements.size
        addAllInternal(offset + index, elements, n)
        return n > 0
    }

    override actual fun clear() {
        checkIsMutable()
        checkForComodification()
        removeRangeInternal(offset, length)
    }

    override actual fun removeAt(index: Int): E {
        checkIsMutable()
        checkForComodification()
        AbstractList.checkElementIndex(index, length)
        return removeAtInternal(offset + index)
    }

    override actual fun remove(element: E): Boolean {
        checkIsMutable()
        checkForComodification()
        val i = indexOf(element)
        if (i >= 0) removeAt(i)
        return i >= 0
    }

    override actual fun removeAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        checkForComodification()
        return retainOrRemoveAllInternal(offset, length, elements, false) > 0
    }

    override actual fun retainAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        checkForComodification()
        return retainOrRemoveAllInternal(offset, length, elements, true) > 0
    }

    override actual fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        AbstractList.checkRangeIndexes(fromIndex, toIndex, length)
        return ArrayList(backingArray, offset + fromIndex, toIndex - fromIndex, isReadOnly, this, root ?: this)
    }

    actual fun trimToSize() {
        if (backingList != null) throw IllegalStateException() // just in case somebody casts subList to ArrayList
        registerModification()
        if (length < backingArray.size)
            backingArray = backingArray.copyOfUninitializedElements(length)
    }

    final actual fun ensureCapacity(minCapacity: Int) {
        if (backingList != null) throw IllegalStateException() // just in case somebody casts subList to ArrayList
        if (minCapacity <= backingArray.size) return
        registerModification()
        ensureCapacityInternal(minCapacity)
    }

    override fun equals(other: Any?): Boolean {
        checkForComodification()
        return other === this ||
                (other is List<*>) && contentEquals(other)
    }

    override fun hashCode(): Int {
        checkForComodification()
        return backingArray.subarrayContentHashCode(offset, length)
    }

    override fun toString(): String {
        checkForComodification()
        return backingArray.subarrayContentToString(offset, length, this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> toArray(array: Array<T>): Array<T> {
        checkForComodification()
        if (array.size < length) {
            return backingArray.copyOfRange(fromIndex = offset, toIndex = offset + length) as Array<T>
        }

        (backingArray as Array<T>).copyInto(array, 0, startIndex = offset, endIndex = offset + length)

        return terminateCollectionToArray(length, array)
    }

    override fun toArray(): Array<Any?> {
        checkForComodification()
        @Suppress("UNCHECKED_CAST")
        return backingArray.copyOfRange(fromIndex = offset, toIndex = offset + length) as Array<Any?>
    }

    // ---------------------------- private ----------------------------

    private fun registerModification() {
        modCount += 1
    }

    private fun checkForComodification() {
        if (root != null && root.modCount != modCount)
            throw ConcurrentModificationException()
    }

    private fun checkIsMutable() {
        if (isReadOnly || root != null && root.isReadOnly) throw UnsupportedOperationException()
    }

    private fun ensureExtraCapacity(n: Int) {
        ensureCapacityInternal(length + n)
    }

    private fun ensureCapacityInternal(minCapacity: Int) {
        if (minCapacity < 0) throw OutOfMemoryError()    // overflow
        if (minCapacity > backingArray.size) {
            val newSize = AbstractList.newCapacity(backingArray.size, minCapacity)
            backingArray = backingArray.copyOfUninitializedElements(newSize)
        }
    }

    private fun contentEquals(other: List<*>): Boolean {
        return backingArray.subarrayContentEquals(offset, length, other)
    }

    private fun insertAtInternal(i: Int, n: Int) {
        ensureExtraCapacity(n)
        backingArray.copyInto(backingArray, startIndex = i, endIndex = offset + length, destinationOffset = i + n)
        length += n
    }

    private fun addAtInternal(i: Int, element: E) {
        registerModification()
        if (backingList != null) {
            backingList.addAtInternal(i, element)
            backingArray = backingList.backingArray
            length++
        } else {
            insertAtInternal(i, 1)
            backingArray[i] = element
        }
    }

    private fun addAllInternal(i: Int, elements: Collection<E>, n: Int) {
        registerModification()
        if (backingList != null) {
            backingList.addAllInternal(i, elements, n)
            backingArray = backingList.backingArray
            length += n
        } else {
            insertAtInternal(i, n)
            var j = 0
            val it = elements.iterator()
            while (j < n) {
                backingArray[i + j] = it.next()
                j++
            }
        }
    }

    private fun removeAtInternal(i: Int): E {
        registerModification()
        if (backingList != null) {
            val old = backingList.removeAtInternal(i)
            length--
            return old
        } else {
            val old = backingArray[i]
            backingArray.copyInto(backingArray, startIndex = i + 1, endIndex = offset + length, destinationOffset = i)
            backingArray.resetAt(offset + length - 1)
            length--
            return old
        }
    }

    private fun removeRangeInternal(rangeOffset: Int, rangeLength: Int) {
        if (rangeLength > 0) registerModification()
        if (backingList != null) {
            backingList.removeRangeInternal(rangeOffset, rangeLength)
        } else {
            backingArray.copyInto(backingArray, startIndex = rangeOffset + rangeLength, endIndex = length, destinationOffset = rangeOffset)
            backingArray.resetRange(fromIndex = length - rangeLength, toIndex = length)
        }
        length -= rangeLength
    }

    /** Retains elements if [retain] == true and removes them it [retain] == false. */
    private fun retainOrRemoveAllInternal(rangeOffset: Int, rangeLength: Int, elements: Collection<E>, retain: Boolean): Int {
        val removed = if (backingList != null) {
            backingList.retainOrRemoveAllInternal(rangeOffset, rangeLength, elements, retain)
        } else {
            var i = 0
            var j = 0
            while (i < rangeLength) {
                if (elements.contains(backingArray[rangeOffset + i]) == retain) {
                    backingArray[rangeOffset + j++] = backingArray[rangeOffset + i++]
                } else {
                    i++
                }
            }
            val removed = rangeLength - j
            backingArray.copyInto(backingArray, startIndex = rangeOffset + rangeLength, endIndex = length, destinationOffset = rangeOffset + j)
            backingArray.resetRange(fromIndex = length - removed, toIndex = length)
            removed
        }
        if (removed > 0) registerModification()
        length -= removed
        return removed
    }

    private class Itr<E> : MutableListIterator<E> {
        private val list: ArrayList<E>
        private var index: Int
        private var lastIndex: Int
        private var expectedModCount: Int

        constructor(list: ArrayList<E>, index: Int) {
            this.list = list
            this.index = index
            this.lastIndex = -1
            this.expectedModCount = list.modCount
        }

        override fun hasPrevious(): Boolean = index > 0
        override fun hasNext(): Boolean = index < list.length

        override fun previousIndex(): Int = index - 1
        override fun nextIndex(): Int = index

        override fun previous(): E {
            checkForComodification()
            if (index <= 0) throw NoSuchElementException()
            lastIndex = --index
            return list.backingArray[list.offset + lastIndex]
        }

        override fun next(): E {
            checkForComodification()
            if (index >= list.length) throw NoSuchElementException()
            lastIndex = index++
            return list.backingArray[list.offset + lastIndex]
        }

        override fun set(element: E) {
            checkForComodification()
            check(lastIndex != -1) { "Call next() or previous() before replacing element from the iterator." }
            list.set(lastIndex, element)
        }

        override fun add(element: E) {
            checkForComodification()
            list.add(index++, element)
            lastIndex = -1
            expectedModCount = list.modCount
        }

        override fun remove() {
            checkForComodification()
            check(lastIndex != -1) { "Call next() or previous() before removing element from the iterator." }
            list.removeAt(lastIndex)
            index = lastIndex
            lastIndex = -1
            expectedModCount = list.modCount
        }

        private fun checkForComodification() {
            if (list.modCount != expectedModCount)
                throw ConcurrentModificationException()
        }
    }
}

private fun <T> Array<T>.subarrayContentHashCode(offset: Int, length: Int): Int {
    var result = 1
    var i = 0
    while (i < length) {
        val nextElement = this[offset + i]
        result = result * 31 + nextElement.hashCode()
        i++
    }
    return result
}

private fun <T> Array<T>.subarrayContentEquals(offset: Int, length: Int, other: List<*>): Boolean {
    if (length != other.size) return false
    var i = 0
    while (i < length) {
        if (this[offset + i] != other[i]) return false
        i++
    }
    return true
}
