/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A stream over a byte array, used either for reading the given [buf] or for writing.
 *
 * When writing, the underlying array is expanded automatically, so there is no need to know the size in advance
 * (though providing the exact size allows avoiding reallocations). Use [toByteArray] to get the written data.
 */
internal class ByteArrayStream(buf: ByteArray) {
    constructor(initialCapacity: Int = 1024) : this(ByteArray(initialCapacity))

    // The buffer uses the native byte order for performance reasons: this gives the JIT compiler an
    // opportunity to optimize reading multibyte values by reading all bytes at once and not performing byte swapping.
    private var buffer = wrap(buf)

    private fun wrap(array: ByteArray) = ByteBuffer.wrap(array).order(ByteOrder.nativeOrder())

    private fun ensureCapacity(size: Int) {
        if (buffer.remaining() >= size) return
        val position = buffer.position()
        val requiredCapacity = position + size
        val newCapacity = maxOf(requiredCapacity, buffer.capacity() * 2)
        buffer = wrap(buffer.array().copyOf(newCapacity))
        buffer.position(position)
    }

    private fun skip(size: Int) {
        buffer.position(buffer.position() + size)
    }

    fun hasData() = buffer.hasRemaining()

    /**
     * Returns the data written so far.
     * Note: the underlying array is returned as is (without copying) if it has been filled entirely.
     */
    fun toByteArray(): ByteArray {
        val array = buffer.array()
        val size = buffer.position()
        return if (size == array.size) array else array.copyOf(size)
    }

    fun readByte(): Byte {
        return buffer.get()
    }

    fun writeByte(value: Byte) {
        ensureCapacity(Byte.SIZE_BYTES)
        buffer.put(value)
    }

    fun readBoolean() = readByte() != 0.toByte()

    fun writeBoolean(value: Boolean) = writeByte(if (value) 1 else 0)

    fun readInt(): Int {
        return buffer.getInt()
    }

    fun writeInt(value: Int) {
        ensureCapacity(Int.SIZE_BYTES)
        buffer.putInt(value)
    }

    fun readLong(): Long {
        return buffer.getLong()
    }

    fun writeLong(value: Long) {
        ensureCapacity(Long.SIZE_BYTES)
        buffer.putLong(value)
    }

    fun readString(): String {
        val length = readInt()
        val chars = CharArray(length)
        buffer.asCharBuffer().get(chars)
        skip(length * Char.SIZE_BYTES)
        return String(chars)
    }

    fun writeString(string: String) {
        writeInt(string.length)
        val dataSize = string.length * Char.SIZE_BYTES
        ensureCapacity(dataSize)
        buffer.asCharBuffer().put(string)
        skip(dataSize)
    }

    fun readIntArray(): IntArray {
        val size = readInt()
        val array = IntArray(size)
        buffer.asIntBuffer().get(array)
        skip(size * Int.SIZE_BYTES)
        return array
    }

    fun writeIntArray(array: IntArray) {
        writeInt(array.size)
        val dataSize = array.size * Int.SIZE_BYTES
        ensureCapacity(dataSize)
        buffer.asIntBuffer().put(array)
        skip(dataSize)
    }

    inline fun <reified T> readArray(itemReader: ByteArrayStream.() -> T) =
            Array(readInt()) { this.itemReader() }

    inline fun <T> writeArray(array: Array<T>, itemWriter: ByteArrayStream.(T) -> Unit) {
        writeInt(array.size)
        array.forEach { this.itemWriter(it) }
    }

    inline fun <T> readNullable(valueReader: ByteArrayStream.() -> T) =
            if (readBoolean()) this.valueReader() else null

    inline fun <T> writeNullable(value: T?, valueWriter: ByteArrayStream.(T) -> Unit) {
        writeBoolean(value != null)
        if (value != null)
            this.valueWriter(value)
    }

    fun readNullableInt() = readNullable { readInt() }

    fun writeNullableInt(value: Int?) = writeNullable(value) { writeInt(it) }

    fun readNullableString() = readNullable { readString() }

    fun writeNullableString(string: String?) = writeNullable(string) { writeString(it) }
}

internal class StringTableBuilder {
    private val indices = mutableMapOf<String, Int>()
    private var index = 0

    operator fun String.unaryPlus() {
        this@StringTableBuilder.indices.computeIfAbsent(this) { index++ }
    }

    fun build() = StringTable(indices)
}

internal inline fun buildStringTable(block: StringTableBuilder.() -> Unit): StringTable {
    val builder = StringTableBuilder()
    builder.block()
    return builder.build()
}

internal class StringTable(val indices: Map<String, Int>) {
    val sizeBytes: Int get() = Int.SIZE_BYTES + indices.keys.sumOf { Int.SIZE_BYTES + it.length * Char.SIZE_BYTES }

    fun serialize(stream: ByteArrayStream) {
        stream.writeInt(indices.size)
        val strings = Array(indices.size) { "" }
        indices.forEach { [string, index] ->
            strings[index] = string
        }
        strings.forEach { stream.writeString(it) }
    }

    companion object {
        fun deserialize(stream: ByteArrayStream) = Array(stream.readInt()) { stream.readString() }
    }
}