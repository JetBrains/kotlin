/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

public actual class ArrayList<E>
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
public actual constructor(initialCapacity: Int) : MutableList<E>, RandomAccess, AbstractMutableList<E>() {
    private var backing = arrayOfUninitializedElements<E>(initialCapacity)
    private var length = 0
    private var isReadOnly = false
    private companion object {
        private val Empty = ArrayList<Nothing>(0).also { it.isReadOnly = true }
    }

    /**
     * Creates a new empty [ArrayList].
     */
    public actual constructor() : this(10)

    /**
     * Creates a new [ArrayList] filled with the elements of the specified collection.
     *
     * The iteration order of elements in the created list is the same as in the specified collection.
     */
    public actual constructor(elements: Collection<E>) : this(elements.size) {
        addAll(elements)
    }

    @PublishedApi
    internal fun build(): List<E> {
        checkIsMutable()
        isReadOnly = true
        return if (length > 0) this else Empty
    }

    actual override val size: Int
        get() = length

    actual override fun isEmpty(): Boolean = length == 0

    actual override fun get(index: Int): E {
        AbstractList.checkElementIndex(index, length)
        return backing[index]
    }

    actual override operator fun set(index: Int, element: E): E {
        checkIsMutable()
        AbstractList.checkElementIndex(index, length)
        val old = backing[index]
        backing[index] = element
        return old
    }

    actual override fun indexOf(element: E): Int {
        var i = 0
        while (i < length) {
            if (backing[i] == element) return i
            i++
        }
        return -1
    }

    actual override fun lastIndexOf(element: E): Int {
        var i = length - 1
        while (i >= 0) {
            if (backing[i] == element) return i
            i--
        }
        return -1
    }

    actual override fun iterator(): MutableIterator<E> = listIterator(0)
    actual override fun listIterator(): MutableListIterator<E> = listIterator(0)

    actual override fun listIterator(index: Int): MutableListIterator<E> {
        AbstractList.checkPositionIndex(index, length)
        return Itr(this, index)
    }

    actual override fun add(element: E): Boolean {
        checkIsMutable()
        addAtInternal(length, element)
        return true
    }

    actual override fun add(index: Int, element: E) {
        checkIsMutable()
        AbstractList.checkPositionIndex(index, length)
        addAtInternal(index, element)
    }

    actual override fun addAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        val n = elements.size
        addAllInternal(length, elements, n)
        return n > 0
    }

    actual override fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkIsMutable()
        AbstractList.checkPositionIndex(index, length)
        val n = elements.size
        addAllInternal(index, elements, n)
        return n > 0
    }

    actual override fun clear() {
        checkIsMutable()
        removeRangeInternal(0, length)
    }

    actual override fun removeAt(index: Int): E {
        checkIsMutable()
        AbstractList.checkElementIndex(index, length)
        return removeAtInternal(index)
    }

    actual override fun remove(element: E): Boolean {
        checkIsMutable()
        val i = indexOf(element)
        if (i >= 0) removeAt(i)
        return i >= 0
    }

    actual override fun removeAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        return retainOrRemoveAllInternal(0, length, elements, false) > 0
    }

    actual override fun retainAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        return retainOrRemoveAllInternal(0, length, elements, true) > 0
    }

    actual override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        AbstractList.checkRangeIndexes(fromIndex, toIndex, length)
        return ArraySubList(backing, fromIndex, toIndex - fromIndex, null, this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> toArray(array: Array<T>): Array<T> {
        if (array.size < length) {
            return backing.copyOfRange(fromIndex = 0, toIndex = length) as Array<T>
        }

        (backing as Array<T>).copyInto(array, 0, startIndex = 0, endIndex = length)

        return terminateCollectionToArray(length, array)
    }

    override fun toArray(): Array<Any?> {
        @Suppress("UNCHECKED_CAST")
        return backing.copyOfRange(fromIndex = 0, toIndex = length) as Array<Any?>
    }

    public actual fun trimToSize() {
        registerModification()
        if (length < backing.size)
            backing = backing.copyOfUninitializedElements(length)
    }

    public actual fun ensureCapacity(minCapacity: Int) {
        if (minCapacity <= backing.size) return
        registerModification()
        ensureCapacityInternal(minCapacity)
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                (other is List<*>) && contentEquals(other)
    }

    override fun hashCode(): Int {
        return backing.subarrayContentHashCode(0, length)
    }

    override fun toString(): String {
        return backing.subarrayContentToString(0, length, this)
    }

    // ---------------------------- private ----------------------------

    private fun registerModification() {
        modCount += 1
    }

    private fun checkIsMutable() {
        if (isReadOnly) throw UnsupportedOperationException()
    }

    private fun ensureExtraCapacity(n: Int) {
        ensureCapacityInternal(length + n)
    }

    private fun ensureCapacityInternal(minCapacity: Int) {
        if (minCapacity < 0) throw OutOfMemoryError()    // overflow
        if (minCapacity > backing.size) {
            val newSize = AbstractList.newCapacity(backing.size, minCapacity)
            backing = backing.copyOfUninitializedElements(newSize)
        }
    }

    private fun contentEquals(other: List<*>): Boolean {
        return backing.subarrayContentEquals(0, length, other)
    }

    private fun insertAtInternal(i: Int, n: Int) {
        ensureExtraCapacity(n)
        backing.copyInto(backing, startIndex = i, endIndex = length, destinationOffset = i + n)
        length += n
    }

    private fun addAtInternal(i: Int, element: E) {
        registerModification()
        insertAtInternal(i, 1)
        backing[i] = element
    }

    private fun addAllInternal(i: Int, elements: Collection<E>, n: Int) {
        registerModification()
        insertAtInternal(i, n)
        var j = 0
        val it = elements.iterator()
        while (j < n) {
            backing[i + j] = it.next()
            j++
        }
    }

    private fun removeAtInternal(i: Int): E {
        registerModification()
        val old = backing[i]
        backing.copyInto(backing, startIndex = i + 1, endIndex = length, destinationOffset = i)
        backing.resetAt(length - 1)
        length--
        return old
    }

    private fun removeRangeInternal(rangeOffset: Int, rangeLength: Int) {
        if (rangeLength > 0) registerModification()
        backing.copyInto(backing, startIndex = rangeOffset + rangeLength, endIndex = length, destinationOffset = rangeOffset)
        backing.resetRange(fromIndex = length - rangeLength, toIndex = length)
        length -= rangeLength
    }

    /** Retains elements if [retain] == true and removes them it [retain] == false. */
    private fun retainOrRemoveAllInternal(rangeOffset: Int, rangeLength: Int, elements: Collection<E>, retain: Boolean): Int {
        var i = 0
        var j = 0
        while (i < rangeLength) {
            if (elements.contains(backing[rangeOffset + i]) == retain) {
                backing[rangeOffset + j++] = backing[rangeOffset + i++]
            } else {
                i++
            }
        }
        val removed = rangeLength - j
        backing.copyInto(backing, startIndex = rangeOffset + rangeLength, endIndex = length, destinationOffset = rangeOffset + j)
        backing.resetRange(fromIndex = length - removed, toIndex = length)
        if (removed > 0) registerModification()
        length -= removed
        return removed
    }

    private class Itr<E>(
        private val list: ArrayList<E>,
        private var index: Int
    ) : MutableListIterator<E> {
        private var lastIndex = -1
        private var expectedModCount = list.modCount

        override fun hasPrevious(): Boolean = index > 0
        override fun hasNext(): Boolean = index < list.length

        override fun previousIndex(): Int = index - 1
        override fun nextIndex(): Int = index

        override fun previous(): E {
            checkForComodification()
            if (index <= 0) throw NoSuchElementException()
            lastIndex = --index
            return list.backing[lastIndex]
        }

        override fun next(): E {
            checkForComodification()
            if (index >= list.length) throw NoSuchElementException()
            lastIndex = index++
            return list.backing[lastIndex]
        }

        override fun set(element: E) {
            checkForComodification()
            check(lastIndex != -1) { "Call next() or previous() before replacing element from the iterator." }
            list[lastIndex] = element
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

        // Must inline for native, suppress warning for WASM
        @Suppress("NOTHING_TO_INLINE")
        private inline fun checkForComodification() {
            if (list.modCount != expectedModCount)
                throw ConcurrentModificationException()
        }
    }

    private class ArraySubList<E>(
        private var backing: Array<E>,
        private val offset: Int,
        private var length: Int,
        private val parent: ArraySubList<E>?,
        private val root: ArrayList<E>
    ) : MutableList<E>, RandomAccess, AbstractMutableList<E>() {

        init {
            this.modCount = root.modCount
        }

        override val size: Int
            get() {
                checkForComodification()
                return length
            }

        override fun isEmpty(): Boolean {
            checkForComodification()
            return length == 0
        }

        override fun get(index: Int): E {
            checkForComodification()
            AbstractList.checkElementIndex(index, length)
            return backing[offset + index]
        }

        override operator fun set(index: Int, element: E): E {
            checkIsMutable()
            checkForComodification()
            AbstractList.checkElementIndex(index, length)
            val old = backing[offset + index]
            backing[offset + index] = element
            return old
        }

        override fun indexOf(element: E): Int {
            checkForComodification()
            var i = 0
            while (i < length) {
                if (backing[offset + i] == element) return i
                i++
            }
            return -1
        }

        override fun lastIndexOf(element: E): Int {
            checkForComodification()
            var i = length - 1
            while (i >= 0) {
                if (backing[offset + i] == element) return i
                i--
            }
            return -1
        }

        override fun iterator(): MutableIterator<E> = listIterator(0)
        override fun listIterator(): MutableListIterator<E> = listIterator(0)

        override fun listIterator(index: Int): MutableListIterator<E> {
            checkForComodification()
            AbstractList.checkPositionIndex(index, length)
            return Itr(this, index)
        }

        override fun add(element: E): Boolean {
            checkIsMutable()
            checkForComodification()
            addAtInternal(offset + length, element)
            return true
        }

        override fun add(index: Int, element: E) {
            checkIsMutable()
            checkForComodification()
            AbstractList.checkPositionIndex(index, length)
            addAtInternal(offset + index, element)
        }

        override fun addAll(elements: Collection<E>): Boolean {
            checkIsMutable()
            checkForComodification()
            val n = elements.size
            addAllInternal(offset + length, elements, n)
            return n > 0
        }

        override fun addAll(index: Int, elements: Collection<E>): Boolean {
            checkIsMutable()
            checkForComodification()
            AbstractList.checkPositionIndex(index, length)
            val n = elements.size
            addAllInternal(offset + index, elements, n)
            return n > 0
        }

        override fun clear() {
            checkIsMutable()
            checkForComodification()
            removeRangeInternal(offset, length)
        }

        override fun removeAt(index: Int): E {
            checkIsMutable()
            checkForComodification()
            AbstractList.checkElementIndex(index, length)
            return removeAtInternal(offset + index)
        }

        override fun remove(element: E): Boolean {
            checkIsMutable()
            checkForComodification()
            val i = indexOf(element)
            if (i >= 0) removeAt(i)
            return i >= 0
        }

        override fun removeAll(elements: Collection<E>): Boolean {
            checkIsMutable()
            checkForComodification()
            return retainOrRemoveAllInternal(offset, length, elements, false) > 0
        }

        override fun retainAll(elements: Collection<E>): Boolean {
            checkIsMutable()
            checkForComodification()
            return retainOrRemoveAllInternal(offset, length, elements, true) > 0
        }

        override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
            AbstractList.checkRangeIndexes(fromIndex, toIndex, length)
            return ArraySubList(backing, offset + fromIndex, toIndex - fromIndex, this, root)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> toArray(array: Array<T>): Array<T> {
            checkForComodification()
            if (array.size < length) {
                return backing.copyOfRange(fromIndex = offset, toIndex = offset + length) as Array<T>
            }

            (backing as Array<T>).copyInto(array, 0, startIndex = offset, endIndex = offset + length)

            return terminateCollectionToArray(length, array)
        }

        override fun toArray(): Array<Any?> {
            checkForComodification()
            @Suppress("UNCHECKED_CAST")
            return backing.copyOfRange(fromIndex = offset, toIndex = offset + length) as Array<Any?>
        }

        override fun equals(other: Any?): Boolean {
            checkForComodification()
            return other === this ||
                    (other is List<*>) && contentEquals(other)
        }

        override fun hashCode(): Int {
            checkForComodification()
            return backing.subarrayContentHashCode(offset, length)
        }

        override fun toString(): String {
            checkForComodification()
            return backing.subarrayContentToString(offset, length, this)
        }

        // ---------------------------- private ----------------------------

        private fun registerModification() {
            modCount += 1
        }

        private fun checkForComodification() {
            if (root.modCount != modCount)
                throw ConcurrentModificationException()
        }

        private fun checkIsMutable() {
            if (isReadOnly) throw UnsupportedOperationException()
        }

        private val isReadOnly: Boolean
            get() = root.isReadOnly

        private fun contentEquals(other: List<*>): Boolean {
            return backing.subarrayContentEquals(offset, length, other)
        }

        private fun addAtInternal(i: Int, element: E) {
            registerModification()
            if (parent != null) {
                parent.addAtInternal(i, element)
            } else {
                root.addAtInternal(i, element)
            }
            backing = root.backing
            length++
        }

        private fun addAllInternal(i: Int, elements: Collection<E>, n: Int) {
            registerModification()
            if (parent != null) {
                parent.addAllInternal(i, elements, n)
            } else {
                root.addAllInternal(i, elements, n)
            }
            backing = root.backing
            length += n
        }

        private fun removeAtInternal(i: Int): E {
            registerModification()
            val old = if (parent != null) {
                parent.removeAtInternal(i)
            } else {
                root.removeAtInternal(i)
            }
            length--
            return old
        }

        private fun removeRangeInternal(rangeOffset: Int, rangeLength: Int) {
            if (rangeLength > 0) registerModification()
            if (parent != null) {
                parent.removeRangeInternal(rangeOffset, rangeLength)
            } else {
                root.removeRangeInternal(rangeOffset, rangeLength)
            }
            length -= rangeLength
        }

        /** Retains elements if [retain] == true and removes them it [retain] == false. */
        private fun retainOrRemoveAllInternal(rangeOffset: Int, rangeLength: Int, elements: Collection<E>, retain: Boolean): Int {
            val removed =
                if (parent != null) {
                    parent.retainOrRemoveAllInternal(rangeOffset, rangeLength, elements, retain)
                } else {
                    root.retainOrRemoveAllInternal(rangeOffset, rangeLength, elements, retain)
                }
            if (removed > 0) registerModification()
            length -= removed
            return removed
        }

        private class Itr<E>(
            private val list: ArraySubList<E>,
            private var index: Int
        ) : MutableListIterator<E> {
            private var lastIndex = -1
            private var expectedModCount = list.modCount

            override fun hasPrevious(): Boolean = index > 0
            override fun hasNext(): Boolean = index < list.length

            override fun previousIndex(): Int = index - 1
            override fun nextIndex(): Int = index

            override fun previous(): E {
                checkForComodification()
                if (index <= 0) throw NoSuchElementException()
                lastIndex = --index
                return list.backing[list.offset + lastIndex]
            }

            override fun next(): E {
                checkForComodification()
                if (index >= list.length) throw NoSuchElementException()
                lastIndex = index++
                return list.backing[list.offset + lastIndex]
            }

            override fun set(element: E) {
                checkForComodification()
                check(lastIndex != -1) { "Call next() or previous() before replacing element from the iterator." }
                list[lastIndex] = element
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

            // Must inline for native, suppress warning for WASM
            @Suppress("NOTHING_TO_INLINE")
            private inline fun checkForComodification() {
                if (list.root.modCount != expectedModCount)
                    throw ConcurrentModificationException()
            }
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
