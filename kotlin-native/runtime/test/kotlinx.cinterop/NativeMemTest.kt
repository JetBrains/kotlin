/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package test.kotlinx.cinterop

import kotlinx.cinterop.*
import kotlin.test.*

class NativeMemTest {
    private val chunkBytes = 256 * 1024

    @Test
    fun byteArrayTransfers() = memScoped {
        for (size in listOf(15, 16, 17, chunkBytes - 1, chunkBytes, chunkBytes + 1)) {
            val expected = ByteArray(size) { (it + it / 256 + 1).toByte() }
            expected[size - 1] = 42
            val source = expected.copyOf()
            val native = allocArrayOf(source)
            source.fill(0)
            val copy = native.readBytes(size)
            native[size - 1] = 0
            assertContentEquals(expected, copy, "size=$size")
        }
    }

    @Test
    fun charArrayTransfers() = memScoped {
        val chunkChars = chunkBytes / 2
        for (size in listOf(7, 8, 9, chunkChars - 1, chunkChars, chunkChars + 1)) {
            val expected = CharArray(size) { (it * 31).toChar() }
            expected[size - 1] = '\uFFFF'
            val native = expected.concatToString().wcstr.getPointer(this)
            val copy = CharArray(size + 1) { '#' }
            nativeMemUtils.getCharArray(native.pointed, copy, size)
            assertContentEquals(expected, copy.copyOf(size), "size=$size")
            assertEquals('#', copy[size])
            assertEquals(0.toUShort(), native[size])
        }
    }

    @Test
    fun nativeMemoryCopyAndZero() = memScoped {
        for (size in listOf(0, 15, 16, 17, chunkBytes - 1, chunkBytes, chunkBytes + 1)) {
            val expected = ByteArray(size + 2) { (it + 1).toByte() }
            expected[size + 1] = 42
            val source = allocArrayOf(expected)
            val dest = allocArrayOf(ByteArray(size + 2) { -1 })
            val start = (dest + 1)!!
            nativeMemUtils.copyMemory(start.pointed, size, (source + 1)!!.pointed)
            expected[0] = -1
            expected[size + 1] = -1
            assertContentEquals(expected, dest.readBytes(size + 2), "copy size=$size")
            zeroValue<ByteVar>(size, 1).place(start)
            expected.fill(0, 1, size + 1)
            assertContentEquals(expected, dest.readBytes(size + 2), "zero size=$size")
        }
    }
}
