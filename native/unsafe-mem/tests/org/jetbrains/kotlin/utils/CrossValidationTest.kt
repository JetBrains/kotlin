/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Uses both [UnsafeMemoryAccess] implementations and verifies they produce
 * identical byte-level results for every operation.
 *
 * [implA] is [UnsafeBasedMemoryAccess] (referenced directly).
 * [implB] is `MemorySegmentMemoryAccess` (referred through [unsafeMemoryAccess]).
 *
 * Skipped on JDK < 25 because `MemorySegmentMemoryAccess` is only available on JDK 25+.
 */
class CrossValidationTest {
    private val implA: UnsafeMemoryAccess = UnsafeBasedMemoryAccess
    private val implB: UnsafeMemoryAccess = getMemorySegmentMemoryAccessOrSkip()

    private inline fun withSharedMemory(size: Long, block: (Long) -> Unit) {
        val address = implA.allocateMemory(size)
        try {
            block(address)
        } finally {
            implA.freeMemory(address)
        }
    }

    private fun <T> crossReadTest(
        size: Long,
        put: UnsafeMemoryAccess.(Long, T) -> Unit,
        get: UnsafeMemoryAccess.(Long) -> T,
        valueA: T,
        valueB: T,
    ) {
        withSharedMemory(size) { addr ->
            implA.put(addr, valueA)
            assertEquals(valueA, implB.get(addr))

            implB.put(addr, valueB)
            assertEquals(valueB, implA.get(addr))
        }
    }

    private fun fillZeroAndVerify(addr: Long, writer: UnsafeMemoryAccess, verifier: UnsafeMemoryAccess) {
        val size = 16L
        for (i in 0 until size) writer.putByte(addr + i, 0xFF.toByte())
        writer.zeroMemory(addr, size)
        for (i in 0 until size) assertEquals(0, verifier.getByte(addr + i))
    }

    @Test
    fun testByteCrossRead() = crossReadTest(
        1, UnsafeMemoryAccess::putByte, UnsafeMemoryAccess::getByte,
        0x7F.toByte(), 0x80.toByte(),
    )

    @Test
    fun testShortCrossRead() = crossReadTest(
        2, UnsafeMemoryAccess::putShort, UnsafeMemoryAccess::getShort,
        0x1234.toShort(), 0x5678.toShort(),
    )

    @Test
    fun testIntCrossRead() = crossReadTest(
        4, UnsafeMemoryAccess::putInt, UnsafeMemoryAccess::getInt,
        0xDEADBEEF.toInt(), 0xCAFEBABE.toInt(),
    )

    @Test
    fun testLongCrossRead() = crossReadTest(
        8, UnsafeMemoryAccess::putLong, UnsafeMemoryAccess::getLong,
        0x123456789ABCDEF0L, 0xFEDCBA9876543210uL.toLong(),
    )

    @Test
    fun testFloatCrossRead() = crossReadTest(
        4, UnsafeMemoryAccess::putFloat, UnsafeMemoryAccess::getFloat,
        3.14f, 2.71828f,
    )

    @Test
    fun testDoubleCrossRead() = crossReadTest(
        8, UnsafeMemoryAccess::putDouble, UnsafeMemoryAccess::getDouble,
        3.141592653589793, 2.718281828459045,
    )

    @Test
    fun testCopyFromByteArrayEquivalence() {
        val src: ByteArray = [0x11, 0x22, 0x33, 0x44, 0x55]
        withSharedMemory(src.size.toLong()) { addr ->
            implA.copyFromByteArray(src, addr, src.size)
            val destB = ByteArray(src.size)
            implB.copyToByteArray(addr, destB, src.size)
            assertArrayEquals(src, destB)

            implA.zeroMemory(addr, src.size.toLong())

            implB.copyFromByteArray(src, addr, src.size)
            val destA = ByteArray(src.size)
            implA.copyToByteArray(addr, destA, src.size)
            assertArrayEquals(src, destA)
        }
    }

    @Test
    fun testCopyToByteArrayEquivalence() {
        withSharedMemory(8) { addr ->
            implA.putLong(addr, 0x0102030405060708L)

            val destA = ByteArray(8)
            val destB = ByteArray(8)
            implA.copyToByteArray(addr, destA, 8)
            implB.copyToByteArray(addr, destB, 8)
            assertArrayEquals(destA, destB)
        }
    }

    @Test
    fun testCopyFromCharArrayEquivalence() {
        val src: CharArray = ['H', 'e', 'l', 'l', 'o']
        val byteLen = src.size * Char.SIZE_BYTES
        withSharedMemory(byteLen.toLong()) { addr ->
            implA.copyFromCharArray(src, addr, src.size)
            val destB = ByteArray(byteLen)
            implB.copyToByteArray(addr, destB, byteLen)

            implA.zeroMemory(addr, byteLen.toLong())

            implB.copyFromCharArray(src, addr, src.size)
            val destA = ByteArray(byteLen)
            implA.copyToByteArray(addr, destA, byteLen)

            assertArrayEquals(destB, destA)
        }
    }

    @Test
    fun testZeroMemoryEquivalence() {
        withSharedMemory(16L) { addr ->
            fillZeroAndVerify(addr, implA, implB)
            fillZeroAndVerify(addr, implB, implA)
        }
    }
}
