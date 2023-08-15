/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import sun.misc.Unsafe

private val unsafe = with(Unsafe::class.java.getDeclaredField("theUnsafe")) {
    isAccessible = true
    return@with this.get(null) as Unsafe
}
private val byteArrayBaseOffset = unsafe.arrayBaseOffset(ByteArray::class.java).toLong()
private val charArrayBaseOffset = unsafe.arrayBaseOffset(CharArray::class.java).toLong()
private val intArrayBaseOffset = unsafe.arrayBaseOffset(IntArray::class.java).toLong()

internal class ByteArrayStream(val buf: ByteArray) {
    private var offset = 0

    fun hasData() = offset < buf.size

    fun readInt(): Int {
        checkSize(offset + Int.SIZE_BYTES) { "Can't read an int at $offset, size = ${buf.size}" }
        return unsafe.getInt(buf, byteArrayBaseOffset + offset).also { offset += Int.SIZE_BYTES }
    }

    fun writeInt(value: Int) {
        checkSize(offset + Int.SIZE_BYTES) { "Can't write an int at $offset, size = ${buf.size}" }
        unsafe.putInt(buf, byteArrayBaseOffset + offset, value).also { offset += Int.SIZE_BYTES }
    }

    fun readString(length: Int): String {
        checkSize(offset + Char.SIZE_BYTES * length) {
            "Can't read a string of length $length at $offset, size = ${buf.size}"
        }
        val chars = CharArray(length)
        unsafe.copyMemory(buf, byteArrayBaseOffset + offset, chars, charArrayBaseOffset, length * Char.SIZE_BYTES.toLong())
        offset += length * Char.SIZE_BYTES
        return String(chars)
    }

    fun writeString(string: String) {
        checkSize(offset + Char.SIZE_BYTES * string.length) {
            "Can't write a string of length ${string.length} at $offset, size = ${buf.size}"
        }
        unsafe.copyMemory(string.toCharArray(), charArrayBaseOffset, buf, byteArrayBaseOffset + offset, string.length * Char.SIZE_BYTES.toLong())
        offset += string.length * Char.SIZE_BYTES
    }

    fun readIntArray(): IntArray {
        val size = readInt()
        checkSize(offset + Int.SIZE_BYTES * size) {
            "Can't read an int array of size $size at $offset, size = ${buf.size}"
        }
        val array = IntArray(size)
        unsafe.copyMemory(buf, byteArrayBaseOffset + offset, array, intArrayBaseOffset, size * Int.SIZE_BYTES.toLong())
        offset += size * Int.SIZE_BYTES
        return array
    }

    fun writeIntArray(array: IntArray) {
        checkSize(offset + Int.SIZE_BYTES + Int.SIZE_BYTES * array.size) {
            "Can't write an int array of size ${array.size} at $offset, size = ${buf.size}"
        }
        unsafe.putInt(buf, byteArrayBaseOffset + offset, array.size).also { offset += Int.SIZE_BYTES }
        unsafe.copyMemory(array, intArrayBaseOffset, buf, byteArrayBaseOffset + offset, array.size * Int.SIZE_BYTES.toLong())
        offset += array.size * Int.SIZE_BYTES
    }

    private fun checkSize(at: Int, messageBuilder: () -> String) {
        if (at > buf.size) error(messageBuilder())
    }
}

internal class StringTableBuilder {
    private val indices = mutableMapOf<String, Int>()
    private var index = 0

    operator fun String.unaryPlus() {
        this@StringTableBuilder.indices.getOrPut(this) { index++ }
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
        val lengths = IntArray(indices.size)
        val strings = Array(indices.size) { "" }
        indices.forEach { (string, index) ->
            lengths[index] = string.length
            strings[index] = string
        }
        stream.writeIntArray(lengths)
        strings.forEach { stream.writeString(it) }
    }

    companion object {
        fun deserialize(stream: ByteArrayStream): Array<String> {
            val lengths = stream.readIntArray()
            return Array(lengths.size) { stream.readString(lengths[it]) }
        }
    }
}