/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

import kotlin.*

internal fun checkWindowSizeStep(size: Int, step: Int) {
    require(size > 0 && step > 0) {
        if (size != step)
            "Both size $size and step $step must be greater than zero."
        else
            "size $size must be greater than zero."
    }
}

internal fun <T> Sequence<T>.windowedSequence(size: Int, step: Int, partialWindows: Boolean, reuseBuffer: Boolean): Sequence<List<T>> {
    checkWindowSizeStep(size, step)
    return Sequence { windowedIterator(iterator(), size, step, partialWindows, reuseBuffer) }
}

internal fun <T> windowedIterator(iterator: Iterator<T>, size: Int, step: Int, partialWindows: Boolean, reuseBuffer: Boolean): Iterator<List<T>> {
    if (!iterator.hasNext()) return EmptyIterator
    return iterator<List<T>> {
        val gap = step - size
        if (gap >= 0) {
            var buffer = ArrayList<T>(size)
            var skip = 0
            for (e in iterator) {
                if (skip > 0) { skip -= 1; continue }
                buffer.add(e)
                if (buffer.size == size) {
                    yield(buffer)
                    if (reuseBuffer) buffer.clear() else buffer = ArrayList(size)
                    skip = gap
                }
            }
            if (buffer.isNotEmpty()) {
                if (partialWindows || buffer.size == size) yield(buffer)
            }
        } else {
            val buffer = RingBuffer<T>(size)
            for (e in iterator) {
                buffer.add(e)
                if (buffer.isFull()) {
                    yield(if (reuseBuffer) buffer else ArrayList(buffer))
                    buffer.removeFirst(step)
                }
            }
            if (partialWindows) {
                while (buffer.size > step) {
                    yield(if (reuseBuffer) buffer else ArrayList(buffer))
                    buffer.removeFirst(step)
                }
                if (buffer.isNotEmpty()) yield(buffer)
            }
        }
    }
}

internal class MovingSubList<out E>(private val list: List<E>) : AbstractList<E>(), RandomAccess {
    private var fromIndex: Int = 0
    private var _size: Int = 0

    fun move(fromIndex: Int, toIndex: Int) {
        checkRangeIndexes(fromIndex, toIndex, list.size)
        this.fromIndex = fromIndex
        this._size = toIndex - fromIndex
    }

    override fun get(index: Int): E {
        checkElementIndex(index, _size)

        return list[fromIndex + index]
    }

    override val size: Int get() = _size
}


/**
 * Provides ring buffer implementation.
 *
 * Buffer overflow is not allowed so [add] doesn't overwrite tail but raises an exception.
 */
private class RingBuffer<T>(val capacity: Int) : AbstractList<T>(), RandomAccess {
    init {
        require(capacity >= 0) { "ring buffer capacity should not be negative but it is $capacity" }
    }

    private val buffer = arrayOfNulls<Any?>(capacity)
    private var startIndex: Int = 0

    override var size: Int = 0
        private set

    override fun get(index: Int): T {
        checkElementIndex(index, size)
        @Suppress("UNCHECKED_CAST")
        return buffer[startIndex.forward(index)] as T
    }

    fun isFull() = size == capacity

    override fun iterator(): Iterator<T> = object : AbstractIterator<T>() {
        private var count = size
        private var index = startIndex

        override fun computeNext() {
            if (count == 0) {
                done()
            } else {
                @Suppress("UNCHECKED_CAST")
                setNext(buffer[index] as T)
                index = index.forward(1)
                count--
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> toArray(array: Array<T>): Array<T> {
        val result: Array<T?> =
            if (array.size < this.size) array.copyOf(this.size) else array as Array<T?>

        val size = this.size

        var widx = 0
        var idx = startIndex

        while (widx < size && idx < capacity) {
            result[widx] = buffer[idx] as T
            widx++
            idx++
        }

        idx = 0
        while (widx < size) {
            result[widx] = buffer[idx] as T
            widx++
            idx++
        }
        if (result.size > this.size) result[this.size] = null

        return result as Array<T>
    }

    override fun toArray(): Array<Any?> {
        return toArray(arrayOfNulls(size))
    }

    /**
     * Add [element] to the buffer or fail with [IllegalStateException] if no free space available in the buffer
     */
    fun add(element: T) {
        if (isFull()) {
            throw IllegalStateException("ring buffer is full")
        }

        buffer[startIndex.forward(size)] = element
        size++
    }

    /**
     * Removes [n] first elements from the buffer or fails with [IllegalArgumentException] if not enough elements in the buffer to remove
     */
    fun removeFirst(n: Int) {
        require(n >= 0) { "n shouldn't be negative but it is $n" }
        require(n <= size) { "n shouldn't be greater than the buffer size: n = $n, size = $size" }

        if (n > 0) {
            val start = startIndex
            val end = start.forward(n)

            if (start > end) {
                buffer.fill(null, start, capacity)
                buffer.fill(null, 0, end)
            } else {
                buffer.fill(null, start, end)
            }

            startIndex = end
            size -= n
        }
    }


    @Suppress("NOTHING_TO_INLINE")
    private inline fun Int.forward(n: Int): Int = (this + n) % capacity

    // TODO: replace with Array.fill from stdlib when available in common
    private fun <T> Array<T>.fill(element: T, fromIndex: Int = 0, toIndex: Int = size): Unit {
        for (idx in fromIndex until toIndex) {
            this[idx] = element
        }
    }
}