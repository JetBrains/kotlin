/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class ByteArrayStream(val buf: ByteArray) {
    /*
    The buffer uses the native byte order for performance reasons: this gives the JIT compiler an
    opportunity to optimize reading multibyte values by reading all bytes at once and not performing byte swapping.
     */
    private val buffer = ByteBuffer.wrap(buf).order(ByteOrder.nativeOrder())

    fun hasData() = buffer.hasRemaining()

    fun readInt(): Int {
        return buffer.getInt()
    }

    fun writeInt(value: Int) {
        buffer.putInt(value)
    }

    fun readString(length: Int): String {
        val chars = CharArray(length)
        buffer.asCharBuffer().get(chars)
        buffer.position(buffer.position() + chars.size * Char.SIZE_BYTES)
        return String(chars)
    }

    fun writeString(string: String) {
        val chars = string.toCharArray()
        buffer.asCharBuffer().put(chars)
        buffer.position(buffer.position() + chars.size * Char.SIZE_BYTES)
    }

    fun readIntArray(): IntArray {
        val size = readInt()
        val array = IntArray(size)
        buffer.asIntBuffer().get(array)
        buffer.position(buffer.position() + array.size * Int.SIZE_BYTES)
        return array
    }

    fun writeIntArray(array: IntArray) {
        buffer.putInt(array.size)
        buffer.asIntBuffer().put(array)
        buffer.position(buffer.position() + array.size * Int.SIZE_BYTES)
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