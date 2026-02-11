/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.js

import java.nio.ByteBuffer
import java.nio.charset.Charset

abstract class AbstractDeserializer(private val source: ByteArray) {
    private val buffer: ByteBuffer = ByteBuffer.wrap(source)
    protected open val charset: Charset get() = Charsets.UTF_8

    val stringTable: Array<String> by lazy {
        readArray { readStringDirect() }
    }

    fun readByte(): Byte {
        return buffer.get()
    }

    fun readBoolean(): Boolean {
        return readByte() != 0.toByte()
    }

    fun readInt(): Int {
        return buffer.int
    }

    fun readDouble(): Double {
        return buffer.double
    }

    private inline fun <R> readBytes(transform: (offset: Int, length: Int) -> R): R {
        val length = readInt()
        val offset = buffer.position()
        val result = transform(offset, length)
        buffer.position(offset + length)
        return result
    }

    fun readByteArray(): ByteArray = readBytes { offset, length ->
        source.copyOfRange(offset, offset + length)
    }

    fun readStringDirect(): String = readBytes { offset, length ->
        String(source, offset, length, charset)
    }

    open fun readString(): String = readStringDirect()

    inline fun <reified T> readArray(readElement: () -> T): Array<T> {
        return Array(readInt()) { readElement() }
    }

    inline fun readRepeated(readElement: () -> Unit) {
        var length = readInt()
        while (length-- > 0) {
            readElement()
        }
    }

    inline fun <T> readList(readElement: () -> T): List<T> {
        val length = readInt()
        val result = ArrayList<T>(length)
        repeat(length) {
            result.add(readElement())
        }
        return result
    }

    inline fun <K, V> readMap(readEntry: () -> Pair<K, V>): Map<K, V> {
        val length = readInt()
        val result = LinkedHashMap<K, V>(length)
        repeat(length) {
            val (key, value) = readEntry()
            result[key] = value
        }
        return result
    }

    inline fun <T> ifTrue(then: () -> T): T? {
        return if (readBoolean()) then() else null
    }
}