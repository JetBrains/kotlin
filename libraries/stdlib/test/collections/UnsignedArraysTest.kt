/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED")
package test.collections

import kotlin.test.*

fun assertArrayContentEquals(expected: UIntArray, actual: UIntArray, message: String = "") { assertTrue(expected contentEquals actual, message) }


class UnsignedArraysTest {

    @Test
    fun ubyteArrayInit() {
        val zeroArray = UByteArray(42)
        assertEquals(42, zeroArray.size)
        for (index in zeroArray.indices) {
            assertEquals(0u, zeroArray[index])
        }

        val initArray = UByteArray(42) { it.toUByte() }
        for (index in initArray.indices) {
            assertEquals(index.toUByte(), initArray[index])
        }
    }
    
    @Test
    fun ushortArrayInit() {
        val zeroArray = UShortArray(42)
        assertEquals(42, zeroArray.size)
        for (index in zeroArray.indices) {
            assertEquals(0u, zeroArray[index])
        }

        val initArray = UShortArray(42) { it.toUShort() }
        for (index in initArray.indices) {
            assertEquals(index.toUShort(), initArray[index])
        }
    }
    
    @Test
    fun uintArrayInit() {
        val zeroArray = UIntArray(42)
        assertEquals(42, zeroArray.size)
        for (index in zeroArray.indices) {
            assertEquals(0u, zeroArray[index])
        }

        val initArray = UIntArray(42) { it.toUInt() }
        for (index in initArray.indices) {
            assertEquals(index.toUInt(), initArray[index])
        }
    }
    
    @Test
    fun ulongArrayInit() {
        val zeroArray = ULongArray(42)
        assertEquals(42, zeroArray.size)
        for (index in zeroArray.indices) {
            assertEquals(0u, zeroArray[index])
        }

        val initArray = ULongArray(42) { it.toULong() }
        for (index in initArray.indices) {
            assertEquals(index.toULong(), initArray[index])
        }
    }


    @Test
    fun contentHashCode() {
        ulongArrayOf(1uL, ULong.MAX_VALUE, ULong.MIN_VALUE).let { assertEquals(it.toList().hashCode(), it.contentHashCode()) }
        uintArrayOf(1u, UInt.MAX_VALUE, UInt.MIN_VALUE).let { assertEquals(it.toList().hashCode(), it.contentHashCode()) }
        ushortArrayOf(1u, UShort.MAX_VALUE, UShort.MIN_VALUE).let { assertEquals(it.toList().hashCode(), it.contentHashCode()) }
        ubyteArrayOf(1u, UByte.MAX_VALUE, UByte.MIN_VALUE).let { assertEquals(it.toList().hashCode(), it.contentHashCode()) }
    }

    @Test
    fun contentToString() {
        ulongArrayOf(1uL, ULong.MAX_VALUE, ULong.MIN_VALUE).let { assertEquals(it.toList().toString(), it.contentToString()) }
        uintArrayOf(1u, UInt.MAX_VALUE, UInt.MIN_VALUE).let { assertEquals(it.toList().toString(), it.contentToString()) }
        ushortArrayOf(1u, UShort.MAX_VALUE, UShort.MIN_VALUE).let { assertEquals(it.toList().toString(), it.contentToString()) }
        ubyteArrayOf(1u, UByte.MAX_VALUE, UByte.MIN_VALUE).let { assertEquals(it.toList().toString(), it.contentToString()) }
    }

    @Test
    fun contentEquals() {
        uintArrayOf(1u, UInt.MAX_VALUE, UInt.MIN_VALUE).let { arr ->
            assertTrue(arr contentEquals UIntArray(arr.size, arr::get))
            assertFalse(arr contentEquals UIntArray(arr.size - 1, arr::get))
        }
        ulongArrayOf(1u, ULong.MAX_VALUE, ULong.MIN_VALUE).let { arr ->
            assertTrue(arr contentEquals ULongArray(arr.size, arr::get))
            assertFalse(arr contentEquals ULongArray(arr.size - 1, arr::get))
        }
        ushortArrayOf(1u, UShort.MAX_VALUE, UShort.MIN_VALUE).let { arr ->
            assertTrue(arr contentEquals UShortArray(arr.size, arr::get))
            assertFalse(arr contentEquals UShortArray(arr.size - 1, arr::get))
        }
        ubyteArrayOf(1u, UByte.MAX_VALUE, UByte.MIN_VALUE).let { arr ->
            assertTrue(arr contentEquals UByteArray(arr.size, arr::get))
            assertFalse(arr contentEquals UByteArray(arr.size - 1, arr::get))
        }
    }

    @Test
    fun asArray() {
        val uintArray = uintArrayOf(1, UInt.MAX_VALUE)
        val intArray = uintArray.asIntArray()
        assertTrue(intArray contentEquals intArrayOf(1, -1))

        intArray.reverse()
        val uintArray2 = intArray.asUIntArray()

        assertTrue(uintArray contentEquals uintArray2)
        assertTrue(uintArray contentEquals uintArrayOf(UInt.MAX_VALUE, 1))


        val ulongArray = ulongArrayOf(1, ULong.MAX_VALUE)
        val longArray = ulongArray.asLongArray()
        assertTrue(longArray contentEquals longArrayOf(1, -1))

        longArray.reverse()
        val ulongArray2 = longArray.asULongArray()
        assertTrue(ulongArray contentEquals ulongArray2)
        assertTrue(ulongArray contentEquals ulongArrayOf(ULong.MAX_VALUE, 1))
    }


    @Test
    fun toArray() {
        val uintArray = uintArrayOf(UInt.MAX_VALUE)
        val intArray = uintArray.toIntArray()
        assertTrue(intArray contentEquals intArrayOf(-1))

        intArray[0] = 0
        val uintArray2 = intArray.toUIntArray()
        assertEquals(UInt.MAX_VALUE, uintArray[0])

        intArray[0] = 1
        assertEquals(0u, uintArray2[0])


        val ulongArray = ulongArrayOf(ULong.MAX_VALUE)
        val longArray = ulongArray.toLongArray()
        assertTrue(longArray contentEquals longArrayOf(-1))

        longArray[0] = 0
        val ulongArray2 = longArray.toULongArray()
        assertEquals(ULong.MAX_VALUE, ulongArray[0])

        longArray[0] = 1
        assertEquals(0u, ulongArray2[0])
    }

    @Test
    fun toTypedArray() {
        uintArrayOf(UInt.MIN_VALUE, UInt.MAX_VALUE).let { assertTrue(it.toTypedArray() contentEquals it.toList().toTypedArray()) }
        ulongArrayOf(ULong.MIN_VALUE, ULong.MAX_VALUE).let { assertTrue(it.toTypedArray() contentEquals it.toList().toTypedArray()) }
        ushortArrayOf(UShort.MIN_VALUE, UShort.MAX_VALUE).let { assertTrue(it.toTypedArray() contentEquals it.toList().toTypedArray()) }
        ubyteArrayOf(UByte.MIN_VALUE, UByte.MAX_VALUE).let { assertTrue(it.toTypedArray() contentEquals it.toList().toTypedArray()) }
    }

    @Test
    fun copyOf() {
        uintArrayOf(UInt.MAX_VALUE).let { arr ->
            val copy = arr.copyOf()
            assertTrue(arr contentEquals copy)
            copy[0] = 1u
            assertFalse(arr contentEquals copy)
            arr[0] = 2u
            assertFalse(arr contentEquals copy)
        }
        ulongArrayOf(ULong.MAX_VALUE).let { arr ->
            val copy = arr.copyOf()
            assertTrue(arr contentEquals copy)
            copy[0] = 1u
            assertFalse(arr contentEquals copy)
            arr[0] = 2u
            assertFalse(arr contentEquals copy)
        }
    }

    @Test
    fun copyAndResize() {
        assertTrue(uintArrayOf(1, 2) contentEquals uintArrayOf(1, 2, 3).copyOf(2))
        assertTrue(uintArrayOf(1, 2, 0) contentEquals uintArrayOf(1, 2).copyOf(3))

        assertTrue(ulongArrayOf(1, 2) contentEquals ulongArrayOf(1, 2, 3).copyOf(2))
        assertTrue(ulongArrayOf(1, 2, 0) contentEquals ulongArrayOf(1, 2).copyOf(3))


        assertTrue(uintArrayOf() contentEquals uintArrayOf(1).copyOf(0))
        assertTrue(ulongArrayOf() contentEquals ulongArrayOf(1).copyOf(0))

        // RuntimeException is the most specific common of JVM and JS implementations
        assertFailsWith<RuntimeException> { uintArrayOf().copyOf(-1) }
        assertFailsWith<RuntimeException> { ulongArrayOf().copyOf(-1) }
    }


    @Test
    fun copyOfRange() {
        assertTrue(ubyteArrayOf(0, 1, 2) contentEquals ubyteArrayOf(0, 1, 2, 3, 4, 5).copyOfRange(0, 3))
        assertTrue(ushortArrayOf(0, 1, 2) contentEquals ushortArrayOf(0, 1, 2, 3, 4, 5).copyOfRange(0, 3))
        assertTrue(uintArrayOf(0, 1, 2) contentEquals uintArrayOf(0, 1, 2, 3, 4, 5).copyOfRange(0, 3))
        assertTrue(ulongArrayOf(0, 1, 2) contentEquals ulongArrayOf(0, 1, 2, 3, 4, 5).copyOfRange(0, 3))

        for (pos in 0..3) {
            assertTrue(uintArrayOf() contentEquals uintArrayOf(1, 2, 3).copyOfRange(pos, pos))
            assertTrue(ulongArrayOf() contentEquals ULongArray(3) { it.toULong() }.copyOfRange(pos, pos))
        }

        for ((start, end) in listOf(-1 to 0, 0 to 2, 2 to 2, 1 to 0)) {
            val bounds = "start: $start, end: $end"
            val exClass = if (start > end) IllegalArgumentException::class else IndexOutOfBoundsException::class
            assertFailsWith(exClass, bounds) { uintArrayOf(1).copyOfRange(start, end) }
            assertFailsWith(exClass, bounds) { ulongArrayOf(1uL).copyOfRange(start, end) }
        }
    }

    @Test
    fun plus() {
        assertArrayContentEquals(uintArrayOf(1u, 2u, 3u), uintArrayOf(1u, 2u) + 3u)
        assertArrayContentEquals(uintArrayOf(1u, 2u, 3u, 4u), uintArrayOf(1u, 2u) + listOf(3u, 4u))
        assertArrayContentEquals(uintArrayOf(1u, 2u, 3u, 4u), uintArrayOf(1u, 2u) + uintArrayOf(3u, 4u))
    }

    @Test
    fun indexOf() {
        expect(-1) { ubyteArrayOf(1, 2, 3).indexOf(0) }
        expect(0) { ushortArrayOf(1, 2, 3).indexOf(1) }
        expect(1) { uintArrayOf(1, 2, 3).indexOf(2) }
        expect(2) { ulongArrayOf(1, 2, 3).indexOf(3) }
    }

    @Test
    fun indexOfFirst() {
        expect(-1) { ubyteArrayOf(1, 2, 3).indexOfFirst { it == 5.toUByte() } }
        expect(0) { ushortArrayOf(1, 2, 3).indexOfFirst { it % 2u == 1u } }
        expect(1) { uintArrayOf(1, 2, 3).indexOfFirst { it % 2u == 0u } }
        expect(2) { ulongArrayOf(1, 2, 3).indexOfFirst { it == 3.toULong() } }
    }

    @Test
    fun lastIndexOf() {
        expect(-1) { ubyteArrayOf(1, 2, 3).lastIndexOf(0) }
        expect(0) { ushortArrayOf(1, 2, 3).lastIndexOf(1) }
        expect(1) { uintArrayOf(2, 2, 3).lastIndexOf(2) }
        expect(2) { ulongArrayOf(3, 2, 3).lastIndexOf(3) }
    }

    @Test
    fun indexOfLast() {
        expect(-1) { ubyteArrayOf(1, 2, 3).indexOfLast { it == 5.toUByte() } }
        expect(2) { ushortArrayOf(1, 2, 3).indexOfLast { it % 2u == 1u } }
        expect(1) { uintArrayOf(1, 2, 3).indexOfLast { it % 2u == 0u } }
        expect(0) { ulongArrayOf(1, 2, 3).indexOfLast { it == 1.toULong() } }
    }

    @Test
    fun indices() {
        expect(0 until 0) { ubyteArrayOf().indices }
        expect(0 until 1) { ushortArrayOf(1).indices }
        expect(0 until 2) { uintArrayOf(1, 2).indices }
        expect(0 until 3) { ulongArrayOf(1, 2, 3).indices }
    }

    @Test
    fun lastIndex() {
        expect(-1) { ubyteArrayOf().lastIndex }
        expect(0) { ushortArrayOf(1).lastIndex }
        expect(1) { uintArrayOf(1, 2).lastIndex }
        expect(2) { ulongArrayOf(1, 2, 3).lastIndex }
    }

    @Test
    fun all() {
        assertTrue(ubyteArrayOf(0, 1, 2).all { it < 3 })
        assertFalse(ushortArrayOf(0, 1, 2).all { it % 2u == 0u })
        assertTrue(uintArrayOf(0, 2, 4).all { it % 2u == 0u })
        assertTrue(ulongArrayOf(2, 3, 4).all { it > 1 })
    }

    @Test
    fun none() {
        assertTrue(ubyteArrayOf(0, 1, 2).none { it > 2 })
        assertFalse(ushortArrayOf(0, 1, 2).none { it % 2u == 0u })
        assertTrue(uintArrayOf(0, 2, 4).none { it % 2u != 0u })
        assertTrue(ulongArrayOf(2, 3, 4).none { it < 2 })
    }

    @Test
    fun any() {
        assertTrue(ubyteArrayOf(0, 1, 2).any { it >= 2 })
        assertFalse(ushortArrayOf(0, 1, 2).any { it == 5.toUShort() })
        assertTrue(uintArrayOf(0, 2, 4).any { it % 3u == 1u })
        assertTrue(ulongArrayOf(2, 3, 4).any { it % 3u == 0.toULong() })
    }

    @Test
    fun count() {
        assertEquals(1, ubyteArrayOf(0, 1, 2).count { it >= 2 })
        assertEquals(2, ushortArrayOf(0, 1, 2).count { it % 2u == 0u })
        assertEquals(0, uintArrayOf(0, 2, 4).count { it % 2u != 0u })
        assertEquals(3, ulongArrayOf(2, 3, 4).count { it > 1 })
    }

    @Test
    fun sumBy() {
        assertEquals(3u, ubyteArrayOf(0, 1, 2).sumBy { it.toUInt() })
        assertEquals(1u, ushortArrayOf(0, 1, 2).sumBy { it % 2u })
        assertEquals(0u, uintArrayOf(0, 2, 4).sumBy { it % 2u })
        assertEquals(6u, ulongArrayOf(2, 3, 4).sumBy { (it - 1u).toUInt() })
    }

    @Test
    fun sumByDouble() {
        // TODO: .toInt().toDouble() -> .toDouble() when conversion from unsigned primitives to Double gets implemented.
        assertEquals(3.0, ubyteArrayOf(0, 1, 2).sumByDouble { it.toInt().toDouble() })
        assertEquals(1.0, ushortArrayOf(0, 1, 2).sumByDouble { (it % 2u).toInt().toDouble() })
        assertEquals(0.0, uintArrayOf(0, 2, 4).sumByDouble { (it % 2u).toInt().toDouble() })
        assertEquals(6.0, ulongArrayOf(2, 3, 4).sumByDouble { (it - 1u).toInt().toDouble() })
    }

    @Test
    fun toUnsignedArray() {
        val uintList = listOf(1u, 2u, 3u)
        val uintArray: UIntArray = uintList.toUIntArray()
        expect(3) { uintArray.size }
        assertEquals(uintList, uintArray.toList())

        val genericArray: Array<ULong> = arrayOf<ULong>(1, 2, 3)
        val ulongArray: ULongArray = genericArray.toULongArray()
        expect(3) { ulongArray.size }
        assertEquals(genericArray.toList(), ulongArray.toList())
    }

}