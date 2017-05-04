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

import kotlin.coroutines.experimental.buildIterator

internal fun checkWindowSizeStep(size: Int, step: Int) {
    require(size > 0 && step > 0) {
        if (size != step)
            "Both size $size and step $step must be greater than zero."
        else
            "size $size must be greater than zero."
    }
}

internal fun <T> Sequence<T>.windowedSequence(size: Int, step: Int, dropTrailing: Boolean, reuseBuffer: Boolean): Sequence<List<T>> {
    checkWindowSizeStep(size, step)
    return Sequence { windowedIterator(iterator(), size, step, dropTrailing, reuseBuffer) }
}

internal fun <T> windowedIterator(iterator: Iterator<T>, size: Int, step: Int, dropTrailing: Boolean, reuseBuffer: Boolean): Iterator<List<T>> {
    if (!iterator.hasNext()) return EmptyIterator
    return buildIterator<List<T>> {
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
                if (!dropTrailing || buffer.size == size) yield(buffer)
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
            if (dropTrailing) {
                if (buffer.size == size) yield(buffer)
            } else {
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
 * Buffer overflow is not allowed so [add] doesn't overwrite tail but raises an exception while [offer] returns `false`
 * If it is going to be public API perhaps this behaviour could be customizable
 */
private class RingBuffer<T>(val capacity: Int): AbstractList<T>(), RandomAccess {
    init {
        require(capacity >= 0) { "ring buffer capacity should not be negative but it is $capacity" }
    }

    private val buffer = arrayOfNulls<Any?>(capacity)
    private var writePosition = 0

    override var size: Int = 0
        private set

    override fun get(index: Int): T {
        checkElementIndex(index, size)
        return getAtUnsafe(writePosition.backward(size - index))
    }

    fun isFull() = size == capacity

    override fun iterator(): Iterator<T> = object : AbstractIterator<T>() {
            private var count = size
            private var index = writePosition.backward(size)

            override fun computeNext() {
                if (count == 0) {
                    done()
                } else {
                    setNext(getAtUnsafe(index))
                    index = index.forward(1)
                    count--
                }
            }
    }

    override fun toArray(): Array<Any?> {
        val size = this.size
        val result = arrayOfNulls<Any?>(size)
        var widx = 0
        var idx = writePosition.backward(size)

        while (widx < size && idx < capacity) {
            result[widx] = buffer[idx]
            widx++
            idx++
        }

        idx = 0
        while (widx < size) {
            result[widx] = buffer[idx]
            widx++
            idx++
        }

        return result
    }

    /**
     * Add [element] to the buffer or fail with [IllegalStateException] if no free space available in the buffer
     */
    fun add(element: T) {
        if (!offer(element)) {
            throw IllegalStateException("Ring buffer is full.")
        }
    }

    /**
     * Offers [element] to the buffer and return `true` if it was added or `false` when there is no free space available in the buffer
     */
    fun offer(element: T): Boolean {
        if (isFull()) {
            return false
        }

        buffer[writePosition] = element
        writePosition = writePosition.forward(1)
        size++
        return true
    }

    /**
     * Removes [n] first elements from the buffer or fails with [IllegalArgumentException] if not enough elements in the buffer to remove
     */
    fun removeFirst(n: Int) {
        require(n >= 0) { "n shouldn't be negative but it is $n" }
        require(n <= size) { "n shouldn't be greater than the buffer size: n = $n, size = $size" }

        if (n > 0) {
            val start = writePosition.backward(size)
            val end = start.forward(n - 1)

            if (start > end) {
                buffer.fill(null, start, capacity)
                buffer.fill(null, 0, end + 1)
            } else {
                buffer.fill(null, start, end + 1)
            }

            size -= n
        }
    }


    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private inline fun getAtUnsafe(idx: Int): T = buffer[idx] as T

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Int.forward(n: Int): Int = (this + n) % capacity

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Int.backward(n: Int): Int = ((this - n) % capacity + capacity) % capacity

    // TODO: replace with Array.fill from stdlib when available in common
    private fun <T> Array<T>.fill(element: T, fromIndex: Int = 0, toIndex: Int = size): Unit {
        for (idx in fromIndex .. toIndex-1) {
            this[idx] = element
        }
    }
}