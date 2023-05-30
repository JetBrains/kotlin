/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections.builders

import java.io.Externalizable
import java.io.InvalidObjectException
import java.io.NotSerializableException

internal class ListBuilder<E> private constructor(
    private var array: Array<E>,
    private var offset: Int,
    private var length: Int,
    private var isReadOnly: Boolean,
    private val backing: ListBuilder<E>?,
    private val root: ListBuilder<E>?
) : MutableList<E>, RandomAccess, AbstractMutableList<E>(), Serializable {
    private companion object {
        private val Empty = ListBuilder<Nothing>(0).also { it.isReadOnly = true }
    }

    constructor() : this(10)

    constructor(initialCapacity: Int) : this(
        arrayOfUninitializedElements(initialCapacity), 0, 0, false, null, null)

    fun build(): List<E> {
        if (backing != null) throw IllegalStateException() // just in case somebody casts subList to ListBuilder
        checkIsMutable()
        isReadOnly = true
        return if (length > 0) this else Empty
    }

    private fun writeReplace(): Any =
        if (isEffectivelyReadOnly)
            SerializedCollection(this, SerializedCollection.tagList)
        else
            throw NotSerializableException("The list cannot be serialized while it is being built.")

    override val size: Int
        get() = length

    override fun isEmpty(): Boolean = length == 0

    override fun get(index: Int): E {
        AbstractList.checkElementIndex(index, length)
        return array[offset + index]
    }

    override operator fun set(index: Int, element: E): E {
        checkIsMutable()
        AbstractList.checkElementIndex(index, length)
        val old = array[offset + index]
        array[offset + index] = element
        return old
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
        AbstractList.checkPositionIndex(index, length)
        return Itr(this, index)
    }

    override fun add(element: E): Boolean {
        checkIsMutable()
        addAtInternal(offset + length, element)
        return true
    }

    override fun add(index: Int, element: E) {
        checkIsMutable()
        AbstractList.checkPositionIndex(index, length)
        addAtInternal(offset + index, element)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        val n = elements.size
        addAllInternal(offset + length, elements, n)
        return n > 0
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkIsMutable()
        AbstractList.checkPositionIndex(index, length)
        val n = elements.size
        addAllInternal(offset + index, elements, n)
        return n > 0
    }

    override fun clear() {
        checkIsMutable()
        removeRangeInternal(offset, length)
    }

    override fun removeAt(index: Int): E {
        checkIsMutable()
        AbstractList.checkElementIndex(index, length)
        return removeAtInternal(offset + index)
    }

    override fun remove(element: E): Boolean {
        checkIsMutable()
        val i = indexOf(element)
        if (i >= 0) removeAt(i)
        return i >= 0
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        return retainOrRemoveAllInternal(offset, length, elements, false) > 0
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        return retainOrRemoveAllInternal(offset, length, elements, true) > 0
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        AbstractList.checkRangeIndexes(fromIndex, toIndex, length)
        return ListBuilder(array, offset + fromIndex, toIndex - fromIndex, isReadOnly, this, root ?: this)
    }

    override fun <T> toArray(destination: Array<T>): Array<T> {
        if (destination.size < length) {
            return java.util.Arrays.copyOfRange(array, offset, offset + length, destination.javaClass)
        }

        @Suppress("UNCHECKED_CAST")
        (array as Array<T>).copyInto(destination, 0, startIndex = offset, endIndex = offset + length)

        if (destination.size > length) {
            @Suppress("UNCHECKED_CAST")
            destination[length] = null as T // null-terminate
        }

        return destination
    }

    override fun toArray(): Array<Any?> {
        @Suppress("UNCHECKED_CAST")
        return array.copyOfRange(fromIndex = offset, toIndex = offset + length) as Array<Any?>
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
                (other is List<*>) && contentEquals(other)
    }

    override fun hashCode(): Int {
        return array.subarrayContentHashCode(offset, length)
    }

    override fun toString(): String {
        return array.subarrayContentToString(offset, length)
    }

    // ---------------------------- private ----------------------------

    private fun checkIsMutable() {
        if (isEffectivelyReadOnly) throw UnsupportedOperationException()
    }

    private val isEffectivelyReadOnly: Boolean
        get() = isReadOnly || root != null && root.isReadOnly

    private fun ensureExtraCapacity(n: Int) {
        ensureCapacityInternal(length + n)
    }

    private fun ensureCapacityInternal(minCapacity: Int) {
        if (minCapacity < 0) throw OutOfMemoryError()    // overflow
        if (minCapacity > array.size) {
            val newSize = AbstractList.newCapacity(array.size, minCapacity)
            array = array.copyOfUninitializedElements(newSize)
        }
    }

    private fun contentEquals(other: List<*>): Boolean {
        return array.subarrayContentEquals(offset, length, other)
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
        private val list: ListBuilder<E>
        private var index: Int
        private var lastIndex: Int

        constructor(list: ListBuilder<E>, index: Int) {
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
            check(lastIndex != -1) { "Call next() or previous() before replacing element from the iterator." }
            list.set(lastIndex, element)
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

internal fun <E> arrayOfUninitializedElements(size: Int): Array<E> {
    require(size >= 0) { "capacity must be non-negative." }
    @Suppress("UNCHECKED_CAST")
    return arrayOfNulls<Any?>(size) as Array<E>
}

private fun <T> Array<out T>.subarrayContentToString(offset: Int, length: Int): String {
    val sb = StringBuilder(2 + length * 3)
    sb.append("[")
    var i = 0
    while (i < length) {
        if (i > 0) sb.append(", ")
        sb.append(this[offset + i])
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