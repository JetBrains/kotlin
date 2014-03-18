package kotlin

import java.util.AbstractList

private class ImmutableArrayList<T>(
        private val array: Array<T>,
        private val offset: Int,
        private val length: Int
) : AbstractList<T>() {
    {
        // impossible
        if (offset < 0) {
            throw IllegalArgumentException("Negative offset ($offset)")
        }
        // impossible
        if (length < 0) {
            throw IllegalArgumentException("Negative length ($length)")
        }
        // possible when builder is used from different threads
        if (offset + length > array.size) {
            throw IllegalArgumentException("offset ($offset) + length ($length) > array.length (${array.size})")
        }
    }

    protected fun indexInArray(index: Int): Int {
        if (index < 0) {
            throw IndexOutOfBoundsException("Negative index ($index)")
        }
        if (index >= length) {
            throw IndexOutOfBoundsException("index ($index) >= length ($length)")
        }
        return index + offset
    }

    public override fun get(index: Int): T = array[indexInArray(index)]

    public override fun size() : Int = length

    public override fun subList(fromIndex: Int, toIndex: Int) : MutableList<T> {
        if (fromIndex < 0) {
            throw IndexOutOfBoundsException("Negative from index ($fromIndex)")
        }
        if (toIndex < fromIndex) {
            throw IndexOutOfBoundsException("toIndex ($toIndex) < fromIndex ($fromIndex)")
        }
        if (toIndex > length) {
            throw IndexOutOfBoundsException("fromIndex ($fromIndex) + toIndex ($toIndex) > length ($length)")
        }
        if (fromIndex == toIndex) {
            return emptyImmutableArrayList as MutableList<T>
        }
        if (fromIndex == 0 && toIndex == length) {
            return this
        }
        return ImmutableArrayList(array, offset + fromIndex, toIndex - fromIndex)
    }

    // TODO: efficiently implement iterator and other stuff
}

private val emptyArray = arrayOfNulls<Any?>(0)
private val emptyImmutableArrayList = ImmutableArrayList<Any?>(emptyArray, 0, 0)

public class ImmutableArrayListBuilder<T>() {

    private var array = emptyArray
    private var length = 0

    public fun build(): List<T> {
        if (length == 0) {
            return emptyImmutableArrayList as List<T>
        }
        else {
            val r = ImmutableArrayList<T>(array as Array<T>, 0, length)
            array = emptyArray
            length = 0
            return r
        }
    }

    public fun ensureCapacity(capacity: Int) {
        if (array.size < capacity) {
            val newSize = Math.max(capacity, Math.max(array.size * 2, 11))
            array = array.copyOf(newSize)
        }
    }

    public fun add(item: T) {
        ensureCapacity(length + 1)
        array[length] = item
        ++length
    }

}

// default list builder
public fun <T> listBuilder(): ImmutableArrayListBuilder<T> = ImmutableArrayListBuilder<T>()
