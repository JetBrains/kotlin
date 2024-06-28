/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections.builders

import java.io.Externalizable
import java.io.InvalidObjectException
import java.io.NotSerializableException

internal class ListBuilder<E>(initialCapacity: Int = 10) : MutableList<E>, RandomAccess, AbstractMutableList<E>(), Serializable {
    private var backing = arrayOfUninitializedElements<E>(initialCapacity)
    private var length = 0
    private var isReadOnly = false

    private companion object {
        private val Empty = ListBuilder<Nothing>(0).also { it.isReadOnly = true }
    }

    fun build(): List<E> {
        checkIsMutable()
        isReadOnly = true
        return if (length > 0) this else Empty
    }

    private fun writeReplace(): Any =
        if (isReadOnly)
            SerializedCollection(this, SerializedCollection.tagList)
        else
            throw NotSerializableException("The list cannot be serialized while it is being built.")

    override val size: Int
        get() = length

    override fun isEmpty() = length == 0

    override fun get(index: Int): E {
        AbstractList.checkElementIndex(index, length)
        return backing[index]
    }

    override operator fun set(index: Int, element: E): E {
        checkIsMutable()
        AbstractList.checkElementIndex(index, length)
        val old = backing[index]
        backing[index] = element
        return old
    }

    override fun indexOf(element: E): Int {
        var i = 0
        while (i < length) {
            if (backing[i] == element) return i
            i++
        }
        return -1
    }

    override fun lastIndexOf(element: E): Int {
        var i = length - 1
        while (i >= 0) {
            if (backing[i] == element) return i
            i--
        }
        return -1
    }

    override fun iterator(): MutableIterator<E> = listIterator(0)
    override fun listIterator(): MutableListIterator<E> = listIterator(0)

    override fun listIterator(index: Int): MutableListIterator<E> {
        AbstractList.checkPositionIndex(index, length)
        return Itr(this, index)
    }

    override fun add(element: E): Boolean {
        checkIsMutable()
        addAtInternal(length, element)
        return true
    }

    override fun add(index: Int, element: E) {
        checkIsMutable()
        AbstractList.checkPositionIndex(index, length)
        addAtInternal(index, element)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        val n = elements.size
        addAllInternal(length, elements, n)
        return n > 0
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkIsMutable()
        AbstractList.checkPositionIndex(index, length)
        val n = elements.size
        addAllInternal(index, elements, n)
        return n > 0
    }

    override fun clear() {
        checkIsMutable()
        removeRangeInternal(0, length)
    }

    override fun removeAt(index: Int): E {
        checkIsMutable()
        AbstractList.checkElementIndex(index, length)
        return removeAtInternal(index)
    }

    override fun remove(element: E): Boolean {
        checkIsMutable()
        val i = indexOf(element)
        if (i >= 0) removeAt(i)
        return i >= 0
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        return retainOrRemoveAllInternal(0, length, elements, false) > 0
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        return retainOrRemoveAllInternal(0, length, elements, true) > 0
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        AbstractList.checkRangeIndexes(fromIndex, toIndex, length)
        return BuilderSubList(backing, fromIndex, toIndex - fromIndex, null, this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> toArray(array: Array<T>): Array<T> {
        if (array.size < length) {
            return java.util.Arrays.copyOfRange(backing, 0, length, array.javaClass)
        }

        (backing as Array<T>).copyInto(array, 0, startIndex = 0, endIndex = length)

        return terminateCollectionToArray(length, array)
    }

    override fun toArray(): Array<Any?> {
        @Suppress("UNCHECKED_CAST")
        return backing.copyOfRange(fromIndex = 0, toIndex = length) as Array<Any?>
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
        private val list: ListBuilder<E>,
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

        private fun checkForComodification() {
            if (list.modCount != expectedModCount)
                throw ConcurrentModificationException()
        }
    }

    class BuilderSubList<E>(
        private var backing: Array<E>,
        private val offset: Int,
        private var length: Int,
        private val parent: BuilderSubList<E>?,
        private val root: ListBuilder<E>
    ) : MutableList<E>, RandomAccess, AbstractMutableList<E>(), Serializable {
        init {
            this.modCount = root.modCount
        }

        private fun writeReplace(): Any =
            if (isReadOnly)
                SerializedCollection(this, SerializedCollection.tagList)
            else
                throw NotSerializableException("The list cannot be serialized while it is being built.")

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
            return BuilderSubList(backing, offset + fromIndex, toIndex - fromIndex, this, root)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> toArray(array: Array<T>): Array<T> {
            checkForComodification()
            if (array.size < length) {
                return java.util.Arrays.copyOfRange(backing, offset, offset + length, array.javaClass)
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
            private val list: BuilderSubList<E>,
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

            private fun checkForComodification() {
                if (list.root.modCount != expectedModCount)
                    throw ConcurrentModificationException()
            }
        }
    }
}

internal fun <E> arrayOfUninitializedElements(size: Int): Array<E> {
    require(size >= 0) { "capacity must be non-negative." }
    @Suppress("UNCHECKED_CAST")
    return arrayOfNulls<Any?>(size) as Array<E>
}

private fun <T> Array<out T>.subarrayContentToString(offset: Int, length: Int, thisCollection: Collection<T>): String {
    val sb = StringBuilder(2 + length * 3)
    sb.append("[")
    var i = 0
    while (i < length) {
        if (i > 0) sb.append(", ")
        val nextElement = this[offset + i]
        if (nextElement === thisCollection) {
            sb.append("(this Collection)")
        } else {
            sb.append(nextElement)
        }
        i++
    }
    sb.append("]")
    return sb.toString()
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

internal fun <T> Array<T>.copyOfUninitializedElements(newSize: Int): Array<T> {
    @Suppress("UNCHECKED_CAST")
    return copyOf(newSize) as Array<T>
}

internal fun <E> Array<E>.resetAt(index: Int) {
    @Suppress("UNCHECKED_CAST")
    (this as Array<Any?>)[index] = null
}

internal fun <E> Array<E>.resetRange(fromIndex: Int, toIndex: Int) {
    for (index in fromIndex until toIndex) resetAt(index)
}

internal class SerializedCollection(
    private var collection: Collection<*>,
    private val tag: Int
) : Externalizable {

    constructor() : this(emptyList<Any?>(), 0) // for deserialization

    override fun writeExternal(output: java.io.ObjectOutput) {
        output.writeByte(tag)
        output.writeInt(collection.size)
        for (element in collection) {
            output.writeObject(element)
        }
    }

    override fun readExternal(input: java.io.ObjectInput) {
        val flags = input.readByte().toInt()
        val tag = flags and 1
        val other = flags and 1.inv()
        if (other != 0) {
            throw InvalidObjectException("Unsupported flags value: $flags.")
        }
        val size = input.readInt()
        if (size < 0) throw InvalidObjectException("Illegal size value: $size.")
        collection = when (tag) {
            tagList -> buildList<Any?>(size) {
                repeat(size) { add(input.readObject()) }
            }
            tagSet -> buildSet<Any?>(size) {
                repeat(size) { add(input.readObject()) }
            }
            else ->
                throw InvalidObjectException("Unsupported collection type tag: $tag.")
        }
    }

    private fun readResolve(): Any = collection

    companion object {
        private const val serialVersionUID: Long = 0L
        const val tagList: Int = 0
        const val tagSet: Int = 1
    }
}
