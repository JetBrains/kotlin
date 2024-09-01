package io

import base.Endianness
import base.Endianness.BIG
import base.Endianness.LITTLE
import base.toLongUnsigned
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.PushbackInputStream

/** Returns new input stream which will read up-to the given number of bytes from this stream. */
fun InputStream.newWithLimit(limit: Int): InputStream = let { base ->
    object : InputStream() {
        var remaining = limit

        override fun read(): Int = when (remaining) {
            0 -> -1
            else -> base.read().also {
                if (it != -1) {
                    remaining -= 1
                }
            }
        }
    }
}

/**
 * Invokes the given function with an input stream which will read exactly the given number of bytes
 * from this stream.
 */
fun <V> InputStream.readWithSize(size: Int, fn: InputStream.() -> V): V =
        newWithLimit(size).let { stream ->
            stream.fn().also { stream.skipAll() }
        }


fun InputStream.readByteInt(): Int = read().takeIf { it != -1 } ?: throw EOFException()

fun InputStream.readByte(): Byte = readByteInt().toByte()

fun InputStream.readShortInt(endianness: Endianness): Int {
    val i1 = this.readByteInt()
    val i2 = this.readByteInt()
    return when (endianness) {
        LITTLE -> i2.shl(8).or(i1)
        BIG -> i1.shl(8).or(i2)
    }
}

fun InputStream.readShort(endianness: Endianness): Short =
        readShortInt(endianness).toShort()

fun InputStream.readInt(endianness: Endianness): Int {
    val i1 = readShortInt(endianness)
    val i2 = readShortInt(endianness)
    return when (endianness) {
        LITTLE -> i2.shl(16).or(i1)
        BIG -> i1.shl(16).or(i2)
    }
}

fun InputStream.readLong(endianness: Endianness): Long {
    val l1 = readInt(endianness).toLongUnsigned()
    val l2 = readInt(endianness).toLongUnsigned()
    return when (endianness) {
        LITTLE -> l2.shl(32).or(l1)
        BIG -> l1.shl(32).or(l2)
    }
}

fun InputStream.readByteArray(size: Int): ByteArray =
        ByteArray(size).also {
            var off = 0
            var len = size
            while (true) {
                if (len == 0) break
                val read = read(it, off, len)
                if (read == -1) throw EOFException()
                off += read
                len -= read
            }
        }

fun InputStream.skipAll() {
    val byteArray = ByteArray(1024)
    while (true) {
        val read = read(byteArray)
        if (read == -1) break
    }
}

fun <V> InputStream.readList(size: Int, fn: InputStream.() -> V): List<V> =
        buildList {
            repeat(size) {
                add(fn())
            }
        }

fun InputStream.readByteArray(): ByteArray =
        ByteArrayOutputStream().also {
            val byteArray = ByteArray(1024)
            while (true) {
                val read = read(byteArray)
                if (read == -1) break
                it.write(byteArray, 0, read)
            }
        }.toByteArray()

fun InputStream.readCString(): String =
        ByteArrayOutputStream().also {
            while (true) {
                val byte = this.readByteInt()
                if (byte == 0) break
                it.write(byte)
            }
        }.toByteArray().toString(Charsets.UTF_8)

fun PushbackInputStream.peek(): Int = read().also { unread(it) }

fun PushbackInputStream.isEof(): Boolean =
        peek() == -1

fun <V> PushbackInputStream.readList(fn: PushbackInputStream.() -> V): List<V> =
        buildList {
            while (!isEof()) {
                add(fn())
            }
        }
