/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.*
import kotlinx.cinterop.*

@SymbolName("Kotlin_Any_share")
external private fun Any.share()

@SymbolName("Kotlin_CPointer_CopyMemory")
external private fun CopyMemory(to: COpaquePointer?, from: COpaquePointer?, count: Int)

@SymbolName("ReadHeapRefNoLock")
internal external fun readHeapRefNoLock(where: Any, index: Int): Any?

/**
 * Mutable concurrently accessible data buffer. Could be accessed from several workers simulteniously.
 */
@Frozen
@NoReorderFields
public class MutableData constructor(capacity: Int = 16) {
    init {
        if (capacity <= 0) throw IllegalArgumentException()
        // Instance of MutableData is shared.
        share()
    }

    private var buffer_ = ByteArray(capacity).apply { share() }
    private var buffer: ByteArray
        get() =
            when (kotlin.native.Platform.memoryModel) {
                kotlin.native.MemoryModel.EXPERIMENTAL -> buffer_
                else -> readHeapRefNoLock(this, 0) as ByteArray
            }
        set(value) { buffer_ = value}
    private var size_ = 0
    private val lock = Lock()

    private fun resizeDataLocked(newSize: Int): Int {
        assert(newSize >= size)
        if (newSize > buffer.size) {
            val actualSize = maxOf(buffer.size * 3 / 2 + 1, newSize)
            val newBuffer = ByteArray(actualSize)
            buffer.copyInto(newBuffer, startIndex = 0, endIndex = size)
            newBuffer.share()
            buffer = newBuffer
        }
        val position = size
        size_ = newSize
        return position
    }

    /**
     * Current data size, may concurrently change later on.
     */
    public val size: Int
        get() = size_

    /**
     * Reset the data buffer, makings its size 0.
     */
    public fun reset() = locked(lock) {
        size_ = 0
    }

    /**
     * Appends data to the buffer.
     */
    public fun append(data: MutableData) = locked(lock) {
        val toCopy = data.size
        val where = resizeDataLocked(size + toCopy)
        data.copyInto(buffer, 0, toCopy, where)
    }

    /**
     * Appends byte array to the buffer.
     */
    public fun append(data: ByteArray, fromIndex: Int = 0, toIndex: Int = data.size): Unit = locked(lock) {
        if (fromIndex > toIndex)
            throw IndexOutOfBoundsException("$fromIndex is bigger than $toIndex")
        if (fromIndex == toIndex) return
        val where = resizeDataLocked(this.size + (toIndex - fromIndex))
        data.copyInto(buffer, where, fromIndex, toIndex)
    }

    /**
     * Appends C data to the buffer, if `data` is null or `count` is non-positive - return.
     */
    public fun append(data: COpaquePointer?, count: Int): Unit = locked(lock) {
        if (data == null || count <= 0) return
        val where = resizeDataLocked(this.size + count)
        buffer.usePinned {
            it -> CopyMemory(it.addressOf(where), data, count)
        }
    }

    /**
     * Copies range of mutable data to the byte array.
     */
    public fun copyInto(output: ByteArray, destinationIndex: Int, startIndex: Int, endIndex: Int): Unit = locked(lock) {
        buffer.copyInto(output, destinationIndex, startIndex, endIndex)
    }

    /**
     * Get a byte from the mutable data.
     *
     * @Throws IndexOutOfBoundsException if index is beyond range.
     */
    public operator fun get(index: Int): Byte = locked(lock) {
        // index < 0 is checked below by array access.
        if (index >= size)
            throw IndexOutOfBoundsException("$index is not below $size")
        buffer[index]
    }

    /**
     * Executes provided block under lock with raw pointer to the data stored in the buffer.
     * Block is executed under the spinlock, and must be short.
     */
    public fun <R> withPointerLocked(block: (COpaquePointer, dataSize: Int) -> R) = locked(lock) {
        buffer.usePinned {
            it -> block(it.addressOf(0), size)
        }
    }

    /**
     * Executes provided block under lock with the raw data buffer.
     * Block is executed under the spinlock, and must be short.
     */
    public fun <R> withBufferLocked(block: (array: ByteArray, dataSize: Int) -> R) = locked(lock) {
        block(buffer, size)
    }
}
