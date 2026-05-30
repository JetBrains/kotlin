/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.ByteOrder

abstract class UnsafeMemoryAccessTest {
    protected abstract val memory: UnsafeMemoryAccess

    private inline fun withMemory(size: Long, block: (Long) -> Unit) {
        val address = memory.allocateMemory(size)
        try {
            block(address)
        } finally {
            memory.freeMemory(address)
        }
    }

    private fun <T> testPutAndGet(
        size: Long,
        put: UnsafeMemoryAccess.(Long, T) -> Unit,
        get: UnsafeMemoryAccess.(Long) -> T,
        vararg values: T,
    ) {
        withMemory(size) { address ->
            for (value in values) {
                memory.put(address, value)
                assertEquals(value, memory.get(address))
            }
        }
    }

    private fun <T> testPutAndGetFloatingPoint(
        size: Long,
        put: UnsafeMemoryAccess.(Long, T) -> Unit,
        get: UnsafeMemoryAccess.(Long) -> T,
        toRawBits: (T) -> Long,
        vararg values: T,
    ) {
        withMemory(size) { address ->
            for (value in values) {
                memory.put(address, value)
                assertEquals(toRawBits(value), toRawBits(memory.get(address)))
            }
        }
    }

    private fun <T> testUnaligned(
        totalSize: Long,
        offsets: IntRange,
        value: T,
        put: UnsafeMemoryAccess.(Long, T) -> Unit,
        get: UnsafeMemoryAccess.(Long) -> T,
    ) {
        withMemory(totalSize) { address ->
            for (offset in offsets) {
                memory.put(address + offset, value)
                assertEquals(value, memory.get(address + offset), "Failed at unaligned offset $offset")
            }
        }
    }

    private fun toBytes(bits: Long, size: Int): ByteArray {
        assertEquals(ByteOrder.LITTLE_ENDIAN, ByteOrder.nativeOrder())
        return ByteArray(size) { i -> (bits shr (i * 8)).toByte() }
    }

    private fun <T> testByteLayout(
        size: Int,
        value: T,
        put: UnsafeMemoryAccess.(Long, T) -> Unit,
        get: UnsafeMemoryAccess.(Long) -> T,
        toRawBits: (T) -> Long,
    ) {
        val expectedBytes = toBytes(toRawBits(value), size)

        for (offset in [0, 1]) {
            // Direction 1: write value, read bytes and verify.
            withMemory(size.toLong() + offset) { base ->
                val address = base + offset
                memory.put(address, value)
                val actual = ByteArray(size)
                memory.copyToByteArray(address, actual, size)
                assertEquals(expectedBytes.toList(), actual.toList(), "offset=$offset: write value, read bytes")
            }

            // Direction 2: write bytes, read value and verify.
            withMemory(size.toLong() + offset) { base ->
                val address = base + offset
                memory.copyFromByteArray(expectedBytes, address, size)
                assertEquals(value, memory.get(address), "offset=$offset: write bytes, read value")
            }
        }
    }

    @Nested
    inner class BasicAllocation {
        @Test
        fun testAllocateAndFreeMemory() {
            withMemory(64) { address ->
                assertTrue(address != 0L, "Expected a non-zero address from allocateMemory")
            }
        }

        @Test
        fun testAllocateZeroBytesThrows() {
            assertThrows<IllegalArgumentException> { memory.allocateMemory(0) }
        }

        @Test
        fun testAllocateNegativeBytesThrows() {
            assertThrows<IllegalArgumentException> { memory.allocateMemory(-1) }
            assertThrows<IllegalArgumentException> { memory.allocateMemory(Long.MIN_VALUE) }
        }

        @Test
        fun testAllocateExcessiveMemoryThrows() {
            val exception = assertThrows<Throwable> { memory.allocateMemory(Long.MAX_VALUE) }
            assertTrue(
                exception is OutOfMemoryError || exception is IllegalArgumentException,
                "Expected OutOfMemoryError or IllegalArgumentException, but got ${exception::class.java.name}"
            )
        }

        @Test
        fun testAllocatedRegionsAreIndependent() {
            val addr1 = memory.allocateMemory(8)
            val addr2 = memory.allocateMemory(8)
            try {
                assertNotEquals(addr1, addr2, "Two allocations must return distinct addresses")

                memory.putLong(addr1, 0x1111111111111111L)
                memory.putLong(addr2, 0x2222222222222222L)

                assertEquals(0x1111111111111111L, memory.getLong(addr1), "Write to addr2 must not affect addr1")
                assertEquals(0x2222222222222222L, memory.getLong(addr2), "Write to addr1 must not affect addr2")
            } finally {
                memory.freeMemory(addr1)
                memory.freeMemory(addr2)
            }
        }
    }

    @Nested
    inner class PutGetRoundTrips {
        @Test
        fun testPutAndGetByte() = testPutAndGet(
            1,
            UnsafeMemoryAccess::putByte,
            UnsafeMemoryAccess::getByte,
            42.toByte(),
            0.toByte(),
            Byte.MIN_VALUE,
            Byte.MAX_VALUE
        )

        @Test
        fun testPutAndGetShort() = testPutAndGet(
            2,
            UnsafeMemoryAccess::putShort,
            UnsafeMemoryAccess::getShort,
            12345.toShort(),
            0.toShort(),
            Short.MIN_VALUE,
            Short.MAX_VALUE
        )

        @Test
        fun testPutAndGetInt() = testPutAndGet(
            4,
            UnsafeMemoryAccess::putInt,
            UnsafeMemoryAccess::getInt,
            123456789,
            0,
            Int.MIN_VALUE,
            Int.MAX_VALUE
        )

        @Test
        fun testPutAndGetLong() = testPutAndGet(
            8,
            UnsafeMemoryAccess::putLong,
            UnsafeMemoryAccess::getLong,
            123456789123456789L,
            0L,
            Long.MIN_VALUE,
            Long.MAX_VALUE
        )

        @Test
        fun testPutAndGetFloat() = testPutAndGetFloatingPoint(
            4,
            UnsafeMemoryAccess::putFloat,
            UnsafeMemoryAccess::getFloat,
            { it.toRawBits().toLong() },
            3.14f, 0.0f, Float.MIN_VALUE, Float.MAX_VALUE,
            -0.0f, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY
        )

        @Test
        fun testPutAndGetDouble() = testPutAndGetFloatingPoint(
            8,
            UnsafeMemoryAccess::putDouble,
            UnsafeMemoryAccess::getDouble,
            Double::toRawBits,
            3.141592653589793, 0.0, Double.MIN_VALUE, Double.MAX_VALUE,
            -0.0, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY
        )
    }

    @Nested
    inner class UnalignedAccess {
        @Test
        fun testUnalignedShortAccess() = testUnaligned(
            4,
            1..1,
            0x1234.toShort(),
            UnsafeMemoryAccess::putShort,
            UnsafeMemoryAccess::getShort
        )

        @Test
        fun testUnalignedIntAccess() = testUnaligned(
            8,
            1..3,
            0x11223344,
            UnsafeMemoryAccess::putInt,
            UnsafeMemoryAccess::getInt
        )

        @Test
        fun testUnalignedLongAccess() = testUnaligned(
            16,
            1..7,
            0x1122334455667788L,
            UnsafeMemoryAccess::putLong,
            UnsafeMemoryAccess::getLong
        )

        @Test
        fun testUnalignedFloatAccess() = testUnaligned(
            8,
            1..3,
            2.71828f,
            UnsafeMemoryAccess::putFloat,
            UnsafeMemoryAccess::getFloat
        )

        @Test
        fun testUnalignedDoubleAccess() = testUnaligned(
            16,
            1..7,
            2.718281828459045,
            UnsafeMemoryAccess::putDouble,
            UnsafeMemoryAccess::getDouble
        )
    }

    @Nested
    inner class ZeroMemoryOperations {
        @Test
        fun testZeroMemory() {
            val size = 16L
            withMemory(size) { address ->
                for (i in 0..<size) {
                    memory.putByte(address + i, 0xAB.toByte())
                }
                memory.zeroMemory(address, size)
                for (i in 0..<size) {
                    assertEquals(0.toByte(), memory.getByte(address + i), "Non-zero byte at offset $i")
                }
            }
        }

        @Test
        fun testZeroMemoryUnaligned() {
            withMemory(24) { address ->
                for (i in 0L..<24L) {
                    memory.putByte(address + i, 0xAB.toByte())
                }
                memory.zeroMemory(address + 3, 11)
                for (i in 0L..<3L) {
                    assertEquals(0xAB.toByte(), memory.getByte(address + i), "Byte before zeroed region at offset $i")
                }
                for (i in 3L..<14L) {
                    assertEquals(0.toByte(), memory.getByte(address + i), "Non-zero byte at offset $i")
                }
                for (i in 14L..<24L) {
                    assertEquals(0xAB.toByte(), memory.getByte(address + i), "Byte after zeroed region at offset $i")
                }
            }
        }

        @Test
        fun testZeroMemoryPartialRegion() {
            val size = 24L
            withMemory(size) { address ->
                for (i in 0..<size) {
                    memory.putByte(address + i, 0xAA.toByte())
                }
                memory.zeroMemory(address + 8, 8)

                for (i in 0..<8) {
                    assertEquals(0xAA.toByte(), memory.getByte(address + i))
                }
                for (i in 8..<16) {
                    assertEquals(0.toByte(), memory.getByte(address + i))
                }
                for (i in 16..<24L) {
                    assertEquals(0xAA.toByte(), memory.getByte(address + i))
                }
            }
        }

    }

    @Nested
    inner class ByteArrayOperations {
        @Test
        fun testCopyFromAndToByteArray() {
            val src: ByteArray = [10, 20, 30, 40, 50]
            withMemory(src.size.toLong()) { address ->
                memory.copyFromByteArray(src, address, src.size)

                val dest = ByteArray(src.size)
                memory.copyToByteArray(address, dest, dest.size)

                assertEquals(src.toList(), dest.toList())
            }
        }

        @Test
        fun testCopyByteArrayUnaligned() {
            val src: ByteArray = [10, 20, 30, 40, 50]
            val totalSize = src.size.toLong() + 3
            withMemory(totalSize) { address ->
                for (i in 0..<totalSize) {
                    memory.putByte(address + i, 0xAA.toByte())
                }
                memory.copyFromByteArray(src, address + 3, src.size)

                // Read back at a different unaligned offset to verify memory layout
                val dest = ByteArray(src.size + 2)
                memory.copyToByteArray(address + 1, dest, dest.size)
                assertEquals([0xAA.toByte(), 0xAA.toByte(), 10, 20, 30, 40, 50], dest.toList())
            }
        }

        @Test
        fun testCopyByteArrayPartial() {
            val size = 8L
            val src: ByteArray = [1, 2, 3, 4, 5, 6, 7, 8]
            withMemory(size) { address ->
                for (i in 0..<size) {
                    memory.putByte(address + i, 0xAA.toByte())
                }
                memory.copyFromByteArray(src, address, 4)

                val dest = ByteArray(4)
                memory.copyToByteArray(address, dest, 4)
                assertEquals(listOf<Byte>(1, 2, 3, 4), dest.toList())

                for (i in 4..<size) {
                    assertEquals(0xAA.toByte(), memory.getByte(address + i), "Byte at offset $i should be untouched")
                }
            }
        }

        @Test
        fun testCopyByteArrayVerifyViaDirectReads() {
            val src: ByteArray = [0x11, 0x22, 0x33, 0x44]
            withMemory(src.size.toLong()) { address ->
                memory.copyFromByteArray(src, address, src.size)

                assertEquals(0x11.toByte(), memory.getByte(address))
                assertEquals(0x22.toByte(), memory.getByte(address + 1))
                assertEquals(0x33.toByte(), memory.getByte(address + 2))
                assertEquals(0x44.toByte(), memory.getByte(address + 3))
            }
        }

        @Test
        fun testCopyToByteArrayFromDirectWrites() {
            withMemory(4) { address ->
                memory.putByte(address, 0xAA.toByte())
                memory.putByte(address + 1, 0xBB.toByte())
                memory.putByte(address + 2, 0xCC.toByte())
                memory.putByte(address + 3, 0xDD.toByte())

                val dest = ByteArray(4)
                memory.copyToByteArray(address, dest, 4)
                assertEquals([0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte()], dest.toList())
            }
        }
    }

    private fun checkCopyFromCharArray(src: CharArray, offset: Long) {
        withMemory(src.size.toLong() * Char.SIZE_BYTES + offset) { base ->
            val address = base + offset
            memory.copyFromCharArray(src, address, src.size)

            for (i in src.indices) {
                assertEquals(
                    src[i].code.toShort(),
                    memory.getShort(address + i * Char.SIZE_BYTES),
                    "Char '${src[i]}' mismatch at index $i (offset=$offset)"
                )
            }
        }
    }

    @Nested
    inner class CharArrayOperations {
        @Test
        fun testCopyFromCharArray() = checkCopyFromCharArray(['H', 'e', 'l', 'l', 'o'], offset = 0)

        @Test
        fun testCopyCharArrayUnaligned() = checkCopyFromCharArray(['H', 'e', 'l', 'l', 'o'], offset = 3)

        @Test
        fun testCopyCharArrayPartial() {
            val src: CharArray = ['A', 'B', 'C', 'D', 'E']
            val totalBytes = src.size.toLong() * Char.SIZE_BYTES
            withMemory(totalBytes) { address ->
                for (i in 0..<totalBytes) {
                    memory.putByte(address + i, 0xAA.toByte())
                }
                memory.copyFromCharArray(src, address, 3)

                for (i in 0..<3) {
                    assertEquals(
                        src[i].code.toShort(),
                        memory.getShort(address + i * Char.SIZE_BYTES),
                        "Char '${src[i]}' mismatch at index $i"
                    )
                }
                val copiedBytes = 3L * Char.SIZE_BYTES
                for (i in copiedBytes..<totalBytes) {
                    assertEquals(0xAA.toByte(), memory.getByte(address + i), "Byte at offset $i should be untouched")
                }
            }
        }

        @Test
        fun testCopyCharArrayWithNonAscii() =
            checkCopyFromCharArray(['\u0000', '\u00FF', '\u4E16', '\uD7FF', '\uFFFF'], offset = 0)


    }

    @Nested
    inner class MultipleValuesAtDifferentOffsets {
        @Test
        fun testMultipleValuesAtDifferentOffsets() {
            withMemory(24) { address ->
                memory.putInt(address, 0x11223344)
                memory.putLong(address + 8, 0x5566778899AABBCCL)
                memory.putInt(address + 16, 0xDDEEFF00.toInt())

                assertEquals(0x11223344, memory.getInt(address))
                assertEquals(0x5566778899AABBCCL, memory.getLong(address + 8))
                assertEquals(0xDDEEFF00.toInt(), memory.getInt(address + 16))
            }
        }

        @Test
        fun testOverwriteValueAtSameAddress() {
            withMemory(8) { address ->
                memory.putLong(address, 0x1111111111111111L)
                assertEquals(0x1111111111111111L, memory.getLong(address))

                memory.putLong(address, 0x2222222222222222L)
                assertEquals(0x2222222222222222L, memory.getLong(address))

                memory.putLong(address, 0L)
                assertEquals(0L, memory.getLong(address))
            }
        }
    }

    @Nested
    inner class ByteLayout {
        @Test
        fun testShortByteLayout() = testByteLayout(
            2, 0x1234.toShort(),
            UnsafeMemoryAccess::putShort, UnsafeMemoryAccess::getShort,
        ) { it.toLong() }

        @Test
        fun testIntByteLayout() = testByteLayout(
            4, 0xDEADBEEF.toInt(),
            UnsafeMemoryAccess::putInt, UnsafeMemoryAccess::getInt,
        ) { it.toLong() }

        @Test
        fun testLongByteLayout() = testByteLayout(
            8, 0x123456789ABCDEF0L,
            UnsafeMemoryAccess::putLong, UnsafeMemoryAccess::getLong,
        ) { it }

        @Test
        fun testFloatByteLayout() = testByteLayout(
            4, 3.14f,
            UnsafeMemoryAccess::putFloat, UnsafeMemoryAccess::getFloat,
        ) { it.toRawBits().toLong() }

        @Test
        fun testDoubleByteLayout() = testByteLayout(
            8, 3.141592653589793,
            UnsafeMemoryAccess::putDouble, UnsafeMemoryAccess::getDouble,
        ) { it.toRawBits() }
    }

    @Nested
    inner class ZeroLengthOperations {
        @Test
        fun testZeroMemoryZeroLength() {
            withMemory(8) { address ->
                for (i in 0..<8) {
                    memory.putByte(address + i, 0xCC.toByte())
                }
                memory.zeroMemory(address, 0)

                for (i in 0..<8) {
                    assertEquals(0xCC.toByte(), memory.getByte(address + i))
                }
            }
        }

        @Test
        fun testCopyFromByteArrayZeroLength() {
            withMemory(4) { address ->
                for (i in 0..<4) {
                    memory.putByte(address + i, 0xDD.toByte())
                }
                memory.copyFromByteArray([1, 2, 3], address, 0)

                for (i in 0..<4) {
                    assertEquals(0xDD.toByte(), memory.getByte(address + i))
                }
            }
        }

        @Test
        fun testCopyToByteArrayZeroLength() {
            withMemory(4) { address ->
                val dest: ByteArray = [1, 2, 3]
                memory.copyToByteArray(address, dest, 0)

                assertEquals(listOf<Byte>(1, 2, 3), dest.toList())
            }
        }

        @Test
        fun testCopyFromCharArrayZeroLength() {
            withMemory(4) { address ->
                for (i in 0..<4) {
                    memory.putByte(address + i, 0xEE.toByte())
                }
                memory.copyFromCharArray(['A', 'B'], address, 0)

                for (i in 0..<4) {
                    assertEquals(0xEE.toByte(), memory.getByte(address + i))
                }
            }
        }
    }
}
