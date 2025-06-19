/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import test.assertArrayContentEquals
import test.collections.behaviors.collectionBehavior
import test.collections.behaviors.listBehavior
import test.collections.behaviors.iteratorBehavior
import kotlin.test.*


class UnsignedArraysTest {

    @Test
    fun collectionBehavior() {
        compare(listOf<UByte>(), ubyteArrayOf()) { collectionBehavior() }
        compare(listOf<UShort>(1u), ushortArrayOf(1u)) { collectionBehavior() }
        compare(listOf<UInt>(1u, 2u), uintArrayOf(1u, 2u)) { collectionBehavior() }
        compare(listOf<ULong>(1u, 2u, 3u), ulongArrayOf(1u, 2u, 3u)) { collectionBehavior() }
    }

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

        (null as ULongArray?).let { assertEquals(it?.toList().hashCode(), it.contentHashCode()) }
    }

    @Test
    fun contentToString() {
        ulongArrayOf(1uL, ULong.MAX_VALUE, ULong.MIN_VALUE).let { assertEquals(it.toList().toString(), it.contentToString()) }
        uintArrayOf(1u, UInt.MAX_VALUE, UInt.MIN_VALUE).let { assertEquals(it.toList().toString(), it.contentToString()) }
        ushortArrayOf(1u, UShort.MAX_VALUE, UShort.MIN_VALUE).let { assertEquals(it.toList().toString(), it.contentToString()) }
        ubyteArrayOf(1u, UByte.MAX_VALUE, UByte.MIN_VALUE).let { assertEquals(it.toList().toString(), it.contentToString()) }

        (null as UIntArray?).let { assertEquals(it?.toList().toString(), it.contentToString()) }
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

        assertTrue((null as UShortArray?) contentEquals null)
        assertFalse(null contentEquals ubyteArrayOf(1u))
        assertFalse(ulongArrayOf() contentEquals null)
    }

    @Test
    fun asArray() {
        val uintArray = uintArrayOf(1u, UInt.MAX_VALUE)
        val intArray = uintArray.asIntArray()
        assertTrue(intArray contentEquals intArrayOf(1, -1))

        intArray.reverse()
        val uintArray2 = intArray.asUIntArray()

        assertTrue(uintArray contentEquals uintArray2)
        assertTrue(uintArray contentEquals uintArrayOf(UInt.MAX_VALUE, 1u))


        val ulongArray = ulongArrayOf(1u, ULong.MAX_VALUE)
        val longArray = ulongArray.asLongArray()
        assertTrue(longArray contentEquals longArrayOf(1, -1))

        longArray.reverse()
        val ulongArray2 = longArray.asULongArray()
        assertTrue(ulongArray contentEquals ulongArray2)
        assertTrue(ulongArray contentEquals ulongArrayOf(ULong.MAX_VALUE, 1u))
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
        assertTrue(uintArrayOf(1u, 2u) contentEquals uintArrayOf(1u, 2u, 3u).copyOf(2))
        assertTrue(uintArrayOf(1u, 2u, 0u) contentEquals uintArrayOf(1u, 2u).copyOf(3))

        assertTrue(ulongArrayOf(1u, 2u) contentEquals ulongArrayOf(1u, 2u, 3u).copyOf(2))
        assertTrue(ulongArrayOf(1u, 2u, 0u) contentEquals ulongArrayOf(1u, 2u).copyOf(3))


        assertTrue(uintArrayOf() contentEquals uintArrayOf(1u).copyOf(0))
        assertTrue(ulongArrayOf() contentEquals ulongArrayOf(1u).copyOf(0))

        // RuntimeException is the most specific common of JVM and JS implementations
        assertFailsWith<RuntimeException> { uintArrayOf().copyOf(-1) }
        assertFailsWith<RuntimeException> { ulongArrayOf().copyOf(-1) }
    }


    @Test
    fun copyOfRange() {
        assertTrue(ubyteArrayOf(0u, 1u, 2u) contentEquals ubyteArrayOf(0u, 1u, 2u, 3u, 4u, 5u).copyOfRange(0, 3))
        assertTrue(ushortArrayOf(0u, 1u, 2u) contentEquals ushortArrayOf(0u, 1u, 2u, 3u, 4u, 5u).copyOfRange(0, 3))
        assertTrue(uintArrayOf(0u, 1u, 2u) contentEquals uintArrayOf(0u, 1u, 2u, 3u, 4u, 5u).copyOfRange(0, 3))
        assertTrue(ulongArrayOf(0u, 1u, 2u) contentEquals ulongArrayOf(0u, 1u, 2u, 3u, 4u, 5u).copyOfRange(0, 3))

        for (pos in 0..3) {
            assertTrue(uintArrayOf() contentEquals uintArrayOf(1u, 2u, 3u).copyOfRange(pos, pos))
            assertTrue(ulongArrayOf() contentEquals ULongArray(3) { it.toULong() }.copyOfRange(pos, pos))
        }

        for ((start, end) in listOf(-1 to 0, 0 to 2, 2 to 2, 1 to 0)) {
            val bounds = "start: $start, end: $end"
            val exClass = if (start > end) IllegalArgumentException::class else IndexOutOfBoundsException::class
            assertFailsWith(exClass, bounds) { uintArrayOf(1u).copyOfRange(start, end) }
            assertFailsWith(exClass, bounds) { ulongArrayOf(1uL).copyOfRange(start, end) }
        }
    }

    @Test
    fun copyOfWithInitializer() {
        assertArrayContentEquals(uintArrayOf(), uintArrayOf(1u, 2u, 3u).copyOf(0) { 4u })
        assertArrayContentEquals(uintArrayOf(1u, 2u, 3u), uintArrayOf(1u, 2u, 3u).copyOf(3) { 4u })
        assertArrayContentEquals(uintArrayOf(1u, 2u, 3u, 4u, 4u), uintArrayOf(1u, 2u, 3u).copyOf(5) { 4u })
        assertArrayContentEquals(uintArrayOf(0u, 1u, 2u), uintArrayOf().copyOf(3) { it.toUInt() })
        assertFailsWith<IllegalArgumentException> { uintArrayOf(1u, 2u, 3u).copyOf(-1) { 0u } }

        assertArrayContentEquals(ulongArrayOf(), ulongArrayOf(1uL, 2uL, 3uL).copyOf(0) { 4uL })
        assertArrayContentEquals(ulongArrayOf(1uL, 2uL, 3uL), ulongArrayOf(1uL, 2uL, 3uL).copyOf(3) { 4uL })
        assertArrayContentEquals(ulongArrayOf(1uL, 2uL, 3uL, 4uL, 4uL), ulongArrayOf(1uL, 2uL, 3uL).copyOf(5) { 4uL })
        assertArrayContentEquals(ulongArrayOf(0uL, 1uL, 2uL), ulongArrayOf().copyOf(3) { it.toULong() })
        assertFailsWith<IllegalArgumentException> { ulongArrayOf(1uL, 2uL, 3uL).copyOf(-1) { 0uL } }

        assertArrayContentEquals(ushortArrayOf(), ushortArrayOf(1u, 2u, 3u).copyOf(0) { 4u })
        assertArrayContentEquals(ushortArrayOf(1u, 2u, 3u), ushortArrayOf(1u, 2u, 3u).copyOf(3) { 4u })
        assertArrayContentEquals(ushortArrayOf(1u, 2u, 3u, 4u, 4u), ushortArrayOf(1u, 2u, 3u).copyOf(5) { 4u })
        assertArrayContentEquals(ushortArrayOf(0u, 1u, 2u), ushortArrayOf().copyOf(3) { it.toUShort() })
        assertFailsWith<IllegalArgumentException> { ushortArrayOf(1u, 2u, 3u).copyOf(-1) { 0u } }

        assertArrayContentEquals(ubyteArrayOf(), ubyteArrayOf(1u, 2u, 3u).copyOf(0) { 4u })
        assertArrayContentEquals(ubyteArrayOf(1u, 2u, 3u), ubyteArrayOf(1u, 2u, 3u).copyOf(3) { 4u })
        assertArrayContentEquals(ubyteArrayOf(1u, 2u, 3u, 4u, 4u), ubyteArrayOf(1u, 2u, 3u).copyOf(5) { 4u })
        assertArrayContentEquals(ubyteArrayOf(0u, 1u, 2u), ubyteArrayOf().copyOf(3) { it.toUByte() })
        assertFailsWith<IllegalArgumentException> { ubyteArrayOf(1u, 2u, 3u).copyOf(-1) { 0u } }
    }

    @Test
    fun plus() {
        assertArrayContentEquals(uintArrayOf(1u, 2u, 3u), uintArrayOf(1u, 2u) + 3u)
        assertArrayContentEquals(uintArrayOf(1u, 2u, 3u, 4u), uintArrayOf(1u, 2u) + listOf(3u, 4u))
        assertArrayContentEquals(uintArrayOf(1u, 2u, 3u, 4u), uintArrayOf(1u, 2u) + uintArrayOf(3u, 4u))
    }

    @Test
    fun indexOf() {
        expect(-1) { ubyteArrayOf(1u, 2u, 3u).indexOf(0u) }
        expect(0) { ushortArrayOf(1u, 2u, 3u).indexOf(1u) }
        expect(1) { uintArrayOf(1u, 2u, 3u).indexOf(2u) }
        expect(2) { ulongArrayOf(1u, 2u, 3u).indexOf(3u) }
    }

    @Test
    fun indexOfFirst() {
        expect(-1) { ubyteArrayOf(1u, 2u, 3u).indexOfFirst { it == 5.toUByte() } }
        expect(0) { ushortArrayOf(1u, 2u, 3u).indexOfFirst { it % 2u == 1u } }
        expect(1) { uintArrayOf(1u, 2u, 3u).indexOfFirst { it % 2u == 0u } }
        expect(2) { ulongArrayOf(1u, 2u, 3u).indexOfFirst { it == 3.toULong() } }
    }

    @Test
    fun lastIndexOf() {
        expect(-1) { ubyteArrayOf(1u, 2u, 3u).lastIndexOf(0u) }
        expect(0) { ushortArrayOf(1u, 2u, 3u).lastIndexOf(1u) }
        expect(1) { uintArrayOf(2u, 2u, 3u).lastIndexOf(2u) }
        expect(2) { ulongArrayOf(3u, 2u, 3u).lastIndexOf(3u) }
    }

    @Test
    fun indexOfLast() {
        expect(-1) { ubyteArrayOf(1u, 2u, 3u).indexOfLast { it == 5.toUByte() } }
        expect(2) { ushortArrayOf(1u, 2u, 3u).indexOfLast { it % 2u == 1u } }
        expect(1) { uintArrayOf(1u, 2u, 3u).indexOfLast { it % 2u == 0u } }
        expect(0) { ulongArrayOf(1u, 2u, 3u).indexOfLast { it == 1.toULong() } }
    }

    @Test
    fun indices() {
        expect(0 until 0) { ubyteArrayOf().indices }
        expect(0 until 1) { ushortArrayOf(1u).indices }
        expect(0 until 2) { uintArrayOf(1u, 2u).indices }
        expect(0 until 3) { ulongArrayOf(1u, 2u, 3u).indices }
    }

    @Test
    fun lastIndex() {
        expect(-1) { ubyteArrayOf().lastIndex }
        expect(0) { ushortArrayOf(1u).lastIndex }
        expect(1) { uintArrayOf(1u, 2u).lastIndex }
        expect(2) { ulongArrayOf(1u, 2u, 3u).lastIndex }
    }

    @Test
    fun all() {
        assertTrue(ubyteArrayOf(0u, 1u, 2u).all { it < 3u })
        assertFalse(ushortArrayOf(0u, 1u, 2u).all { it % 2u == 0u })
        assertTrue(uintArrayOf(0u, 2u, 4u).all { it % 2u == 0u })
        assertTrue(ulongArrayOf(2u, 3u, 4u).all { it > 1uL })
    }

    @Test
    fun none() {
        assertTrue(ubyteArrayOf(0u, 1u, 2u).none { it > 2u })
        assertFalse(ushortArrayOf(0u, 1u, 2u).none { it % 2u == 0u })
        assertTrue(uintArrayOf(0u, 2u, 4u).none { it % 2u != 0u })
        assertTrue(ulongArrayOf(2u, 3u, 4u).none { it < 2uL })
    }

    @Test
    fun any() {
        assertTrue(ubyteArrayOf(0u, 1u, 2u).any { it >= 2u })
        assertFalse(ushortArrayOf(0u, 1u, 2u).any { it == 5.toUShort() })
        assertTrue(uintArrayOf(0u, 2u, 4u).any { it % 3u == 1u })
        assertTrue(ulongArrayOf(2u, 3u, 4u).any { it % 3uL == 0uL })
    }

    @Test
    fun count() {
        assertEquals(1, ubyteArrayOf(0u, 1u, 2u).count { it >= 2u })
        assertEquals(2, ushortArrayOf(0u, 1u, 2u).count { it % 2u == 0u })
        assertEquals(0, uintArrayOf(0u, 2u, 4u).count { it % 2u != 0u })
        assertEquals(3, ulongArrayOf(2u, 3u, 4u).count { it > 1uL })
    }

    @Suppress("DEPRECATION")
    @Test
    fun sumBy() {
        assertEquals(3u, ubyteArrayOf(0u, 1u, 2u).sumBy { it.toUInt() })
        assertEquals(1u, ushortArrayOf(0u, 1u, 2u).sumBy { it % 2u })
        assertEquals(0u, uintArrayOf(0u, 2u, 4u).sumBy { it % 2u })
        assertEquals(6u, ulongArrayOf(2u, 3u, 4u).sumBy { (it - 1u).toUInt() })
    }

    @Suppress("DEPRECATION")
    @Test
    fun sumByDouble() {
        assertEquals(3.0, ubyteArrayOf(0u, 1u, 2u).sumByDouble { it.toDouble() })
        assertEquals(1.0, ushortArrayOf(0u, 1u, 2u).sumByDouble { (it % 2u).toDouble() })
        assertEquals(0.0, uintArrayOf(0u, 2u, 4u).sumByDouble { (it % 2u).toDouble() })
        assertEquals(6.0, ulongArrayOf(2u, 3u, 4u).sumByDouble { (it - 1u).toDouble() })
    }

    @Test
    fun toUnsignedArray() {
        val uintList = listOf(1u, 2u, 3u)
        val uintArray: UIntArray = uintList.toUIntArray()
        expect(3) { uintArray.size }
        assertEquals(uintList, uintArray.toList())

        val genericArray: Array<ULong> = arrayOf<ULong>(1u, 2u, 3u)
        val ulongArray: ULongArray = genericArray.toULongArray()
        expect(3) { ulongArray.size }
        assertEquals(genericArray.toList(), ulongArray.toList())
    }

    @Test
    fun reversed() {
        expect(listOf(3u, 2u, 1u)) { uintArrayOf(1u, 2u, 3u).reversed() }
        expect(listOf<UByte>(3u, 2u, 1u)) { ubyteArrayOf(1u, 2u, 3u).reversed() }
        expect(listOf<UShort>(3u, 2u, 1u)) { ushortArrayOf(1u, 2u, 3u).reversed() }
        expect(listOf<ULong>(3u, 2u, 1u)) { ulongArrayOf(1u, 2u, 3u).reversed() }
    }

    @Test
    fun reversedArray() {
        assertArrayContentEquals(uintArrayOf(3u, 2u, 1u), uintArrayOf(1u, 2u, 3u).reversedArray())
        assertArrayContentEquals(ubyteArrayOf(3u, 2u, 1u), ubyteArrayOf(1u, 2u, 3u).reversedArray())
        assertArrayContentEquals(ushortArrayOf(3u, 2u, 1u), ushortArrayOf(1u, 2u, 3u).reversedArray())
        assertArrayContentEquals(ulongArrayOf(3u, 2u, 1u), ulongArrayOf(1u, 2u, 3u).reversedArray())
    }

    @Test
    fun asList() {
        compare(listOf<UByte>(), ubyteArrayOf().asList()) { listBehavior() }
        compare(listOf<UShort>(1u), ushortArrayOf(1.toUShort()).asList()) { listBehavior() }
        compare(listOf<UInt>(1u, 2u), uintArrayOf(1u, 2u).asList()) { listBehavior() }
        compare(listOf<UInt>(1u, 2u, 3u), uintArrayOf(1u, 2u, 3u).asList()) { listBehavior() }

        val ulongs = ulongArrayOf(1uL, 5uL, 7uL)
        val ulongsAsList = ulongs.asList()
        assertEquals(5uL, ulongsAsList[1])
        ulongs[1] = 10u
        assertEquals(10uL, ulongsAsList[1], "Should reflect changes in original array")
    }

    @Test
    fun slice() {
        assertEquals(listOf<UInt>(), uintArrayOf(1u, 2u, 3u).slice(5..1))
        assertEquals(listOf<UInt>(2u, 3u, 9u), uintArrayOf(2u, 3u, 9u, 2u, 3u, 9u).slice(listOf(3, 1, 2)))
        assertEquals(listOf<UByte>(127u, 100u), ubyteArrayOf(50u, 100u, 127u).slice(2 downTo 1))
        assertEquals(listOf<UShort>(200u, 100u), ushortArrayOf(50u, 100u, 200u).slice(2 downTo 1))
        assertEquals(listOf<ULong>(100u, 200u, 30u), ulongArrayOf(50u, 100u, 200u, 30u).slice(1..3))

        for (range in listOf(-1 until 0, 0 until 2, 2..2)) {
            val bounds = "range: $range"
            val exClass = IndexOutOfBoundsException::class
            assertFailsWith(exClass, bounds) { uintArrayOf(1u).slice(range) }
            assertFailsWith(exClass, bounds) { ulongArrayOf(1u).slice(range) }
        }
    }

    @Test
    fun sliceArray() {
        assertArrayContentEquals(uintArrayOf(), uintArrayOf(1u, 2u, 3u).sliceArray(5..1))
        assertArrayContentEquals(uintArrayOf(2u, 3u, 9u), uintArrayOf(2u, 3u, 9u, 2u, 3u, 9u).sliceArray(listOf(3, 1, 2)))
        assertArrayContentEquals(ubyteArrayOf(127u, 100u), ubyteArrayOf(50u, 100u, 127u).sliceArray(listOf(2, 1)))
        assertArrayContentEquals(ushortArrayOf(200u, 100u), ushortArrayOf(50u, 100u, 200u).sliceArray(listOf(2, 1)))
        assertArrayContentEquals(ulongArrayOf(100u, 200u, 30u), ulongArrayOf(50u, 100u, 200u, 30u).sliceArray(1..3))

        for (range in listOf(-1 until 0, 0 until 2, 2..2)) {
            val bounds = "range: $range"
            val exClass = IndexOutOfBoundsException::class
            assertFailsWith(exClass, bounds) { ubyteArrayOf(1u).sliceArray(range) }
            assertFailsWith(exClass, bounds) { ushortArrayOf(1u).sliceArray(range) }
        }
    }

    @Test
    fun minOrNull() {
        expect(null) { arrayOf<UByte>().minOrNull() }
        expect(1u) { arrayOf<UShort>(1u).minOrNull() }
        expect(2u) { arrayOf<UInt>(2u, 3u).minOrNull() }
        expect(2uL) { arrayOf<ULong>(3u, 2u).minOrNull() }
    }

    @Test
    fun minOrNullInUnsignedArrays() {
        expect(null) { ubyteArrayOf().minOrNull() }
        expect(1u) { ushortArrayOf(1u).minOrNull() }
        expect(2u) { uintArrayOf(2u, 3u).minOrNull() }
        expect(2uL) { ulongArrayOf(3u, 2u).minOrNull() }
    }

    @Test
    fun maxOrNull() {
        expect(null) { arrayOf<UByte>().maxOrNull() }
        expect(1u) { arrayOf<UShort>(1u).maxOrNull() }
        expect(3u) { arrayOf<UInt>(2u, 3u).maxOrNull() }
        expect(3uL) { arrayOf<ULong>(3u, 2u).maxOrNull() }
    }

    @Test
    fun maxOrNullInUnsignedArrays() {
        expect(null) { ubyteArrayOf().maxOrNull() }
        expect(1u) { ushortArrayOf(1u).maxOrNull() }
        expect(3u) { uintArrayOf(2u, 3u).maxOrNull() }
        expect(3uL) { ulongArrayOf(3u, 2u).maxOrNull() }
    }

    @Test
    fun minWitOrNullh() {
        expect(null) { arrayOf<UByte>().minWithOrNull(naturalOrder()) }
        expect(1u) { arrayOf<UShort>(1u).minWithOrNull(naturalOrder()) }
        expect(2u) { arrayOf<UInt>(2u, 3u).minWithOrNull(naturalOrder()) }
        expect(2uL) { arrayOf<ULong>(3u, 2u).minWithOrNull(naturalOrder()) }
    }

    @Test
    fun minWithOrNullInUnsignedArrays() {
        expect(null) { ubyteArrayOf().minWithOrNull(reverseOrder()) }
        expect(1u) { ushortArrayOf(1u).minWithOrNull(reverseOrder()) }
        expect(3u) { uintArrayOf(2u, 3u).minWithOrNull(reverseOrder()) }
        expect(3uL) { ulongArrayOf(3u, 2u).minWithOrNull(reverseOrder()) }
    }

    @Test
    fun maxWithOrNull() {
        expect(null) { arrayOf<UByte>().maxWithOrNull(naturalOrder()) }
        expect(1u) { arrayOf<UShort>(1u).maxWithOrNull(naturalOrder()) }
        expect(3u) { arrayOf<UInt>(2u, 3u).maxWithOrNull(naturalOrder()) }
        expect(3uL) { arrayOf<ULong>(3u, 2u).maxWithOrNull(naturalOrder()) }
    }

    @Test
    fun maxWithOrNullInUnsignedArrays() {
        expect(null) { ubyteArrayOf().maxWithOrNull(reverseOrder()) }
        expect(1u) { ushortArrayOf(1u).maxWithOrNull(reverseOrder()) }
        expect(2u) { uintArrayOf(2u, 3u).maxWithOrNull(reverseOrder()) }
        expect(2uL) { ulongArrayOf(3u, 2u).maxWithOrNull(reverseOrder()) }
    }

    @Test
    fun minByOrNull() {
        expect(null) { arrayOf<UByte>().minByOrNull { it * it } }
        expect(1u) { arrayOf<UShort>(1u).minByOrNull { it * it } }
        expect(2u) { arrayOf<UInt>(2u, 3u).minByOrNull { it * it } }
        expect(3uL) { arrayOf<ULong>(3u, 2u).minByOrNull { it - 3u } }
    }

    @Test
    fun minByOrNullInUnsignedArrays() {
        expect(null) { ubyteArrayOf().minByOrNull { it * it } }
        expect(1u) { ushortArrayOf(1u).minByOrNull { it * it } }
        expect(2u) { uintArrayOf(2u, 3u).minByOrNull { it * it } }
        expect(3uL) { ulongArrayOf(3u, 2u).minByOrNull { it - 3u } }
    }

    @Test
    fun maxByOrNull() {
        expect(null) { arrayOf<UByte>().maxByOrNull { it + 1u } }
        expect(1u) { arrayOf<UShort>(1u).maxByOrNull { it + 1u } }
        expect(2u) { arrayOf<UInt>(2u, 3u).maxByOrNull { it - 3u } }
        expect(3uL) { arrayOf<ULong>(3u, 2u).maxByOrNull { it + 1u } }
    }

    @Test
    fun maxByOrNullInUnsignedArrays() {
        expect(null) { ubyteArrayOf().maxByOrNull { it + 1u } }
        expect(1u) { ushortArrayOf(1u).maxByOrNull { it + 1u } }
        expect(2u) { uintArrayOf(2u, 3u).maxByOrNull { it - 3u } }
        expect(3uL) { ulongArrayOf(3u, 2u).maxByOrNull { it + 1u } }
    }

    @Test
    fun reduce() {
        expect(0u) { ubyteArrayOf(3u, 2u, 1u).reduce { acc, e -> (acc - e).toUByte() } }
        expect(0u) { ushortArrayOf(3u, 2u, 1u).reduce { acc, e -> (acc - e).toUShort() } }
        expect((-4).toUInt()) { uintArrayOf(1u, 2u, 3u).reduce { acc, e -> acc - e } }
        expect((-4).toULong()) { ulongArrayOf(1u, 2u, 3u).reduce { acc, e -> acc - e } }

        assertFailsWith<UnsupportedOperationException> {
            uintArrayOf().reduce { acc, e -> acc + e }
        }
    }

    @Test
    fun reduceOrNull() {
        expect(0u) { ubyteArrayOf(3u, 2u, 1u).reduceOrNull { acc, e -> (acc - e).toUByte() } }
        expect(0u) { ushortArrayOf(3u, 2u, 1u).reduceOrNull { acc, e -> (acc - e).toUShort() } }
        expect((-4).toUInt()) { uintArrayOf(1u, 2u, 3u).reduceOrNull { acc, e -> acc - e } }
        expect((-4).toULong()) { ulongArrayOf(1u, 2u, 3u).reduceOrNull { acc, e -> acc - e } }

        assertNull(uintArrayOf().reduceOrNull { acc, e -> acc + e })
    }

    @Test
    fun reduceIndexed() {
        expect(1u) { ubyteArrayOf(3u, 2u, 1u).reduceIndexed { index, acc, e -> if (index != 2) (e - acc).toUByte() else e } }
        expect(1u) { ushortArrayOf(3u, 2u, 1u).reduceIndexed { index, acc, e -> if (index != 2) (e - acc).toUShort() else e } }
        expect(UInt.MAX_VALUE) { uintArrayOf(1u, 2u, 3u).reduceIndexed { index, acc, e -> index.toUInt() + acc - e } }
        expect(ULong.MAX_VALUE) { ulongArrayOf(1u, 2u, 3u).reduceIndexed { index, acc, e -> index.toULong() + acc - e } }

        assertFailsWith<UnsupportedOperationException> {
            uintArrayOf().reduceIndexed { index, acc, e -> index.toUInt() + e + acc }
        }
    }

    @Test
    fun reduceIndexedOrNull() {
        expect(1u) { ubyteArrayOf(3u, 2u, 1u).reduceIndexedOrNull { index, acc, e -> if (index != 2) (e - acc).toUByte() else e } }
        expect(1u) { ushortArrayOf(3u, 2u, 1u).reduceIndexedOrNull { index, acc, e -> if (index != 2) (e - acc).toUShort() else e } }
        expect(UInt.MAX_VALUE) { uintArrayOf(1u, 2u, 3u).reduceIndexedOrNull { index, acc, e -> index.toUInt() + acc - e } }
        expect(ULong.MAX_VALUE) { ulongArrayOf(1u, 2u, 3u).reduceIndexedOrNull { index, acc, e -> index.toULong() + acc - e } }

        expect(null, { uintArrayOf().reduceIndexedOrNull { index, acc, e -> index.toUInt() + e + acc } })
    }

    @Test
    fun reduceRight() {
        expect(2u) { ubyteArrayOf(1u, 2u, 3u).reduceRightOrNull { e, acc -> (e - acc).toUByte() } }
        expect(2u) { ushortArrayOf(1u, 2u, 3u).reduceRightOrNull { e, acc -> (e - acc).toUShort() } }
        expect(2u) { uintArrayOf(1u, 2u, 3u).reduceRightOrNull { e, acc -> e - acc } }
        expect(2uL) { ulongArrayOf(1u, 2u, 3u).reduceRightOrNull { e, acc -> e - acc } }

        assertNull(uintArrayOf().reduceRightOrNull { e, acc -> e + acc })
    }

    @Test
    fun reduceRightOrNull() {
        expect(2u) { ubyteArrayOf(1u, 2u, 3u).reduceRight { e, acc -> (e - acc).toUByte() } }
        expect(2u) { ushortArrayOf(1u, 2u, 3u).reduceRight { e, acc -> (e - acc).toUShort() } }
        expect(2u) { uintArrayOf(1u, 2u, 3u).reduceRight { e, acc -> e - acc } }
        expect(2uL) { ulongArrayOf(1u, 2u, 3u).reduceRight { e, acc -> e - acc } }

        assertFailsWith<UnsupportedOperationException> {
            uintArrayOf().reduceRight { e, acc -> e + acc }
        }
    }

    @Test
    fun reduceRightIndexed() {
        expect(1u) { ubyteArrayOf(3u, 2u, 1u).reduceRightIndexed { index, e, acc -> if (index != 1) (e - acc).toUByte() else e } }
        expect(1u) { ushortArrayOf(3u, 2u, 1u).reduceRightIndexed { index, e, acc -> if (index != 1) (e - acc).toUShort() else e } }
        expect(1u) { uintArrayOf(1u, 2u, 3u).reduceRightIndexed { index, e, acc -> index.toUInt() + e - acc } }
        expect(1uL) { ulongArrayOf(1u, 2u, 3u).reduceRightIndexed { index, e, acc -> index.toULong() + e - acc } }

        assertFailsWith<UnsupportedOperationException> {
            uintArrayOf().reduceRightIndexed { index, e, acc -> index.toUInt() + e + acc }
        }
    }

    @Test
    fun reduceRightIndexedOrNull() {
        expect(1u) { ubyteArrayOf(3u, 2u, 1u).reduceRightIndexedOrNull { index, e, acc -> if (index != 1) (e - acc).toUByte() else e } }
        expect(1u) { ushortArrayOf(3u, 2u, 1u).reduceRightIndexedOrNull { index, e, acc -> if (index != 1) (e - acc).toUShort() else e } }
        expect(1u) { uintArrayOf(1u, 2u, 3u).reduceRightIndexedOrNull { index, e, acc -> index.toUInt() + e - acc } }
        expect(1uL) { ulongArrayOf(1u, 2u, 3u).reduceRightIndexedOrNull { index, e, acc -> index.toULong() + e - acc } }

        expect(null, { uintArrayOf().reduceRightIndexedOrNull { index, e, acc -> index.toUInt() + e + acc } })
    }

    @Test
    fun forEach() {
        var i = 0
        val a = ubyteArrayOf(3u, 2u, 1u)
        a.forEach { e -> assertEquals(e, a[i++]) }
    }

    @Test
    fun forEachIndexed() {
        val a = ubyteArrayOf(3u, 2u, 1u)
        a.forEachIndexed { index, e -> assertEquals(e, a[index]) }
    }

    @Test
    fun sum() {
        expect(14u) { arrayOf(2u, 3u, 9u).asSequence().sum() }
        expect(400u) { arrayOf<UByte>(200u, 200u).asList().sum() }
        expect(50000u) { arrayOf<UShort>(20000u, 30000u).asIterable().sum() }
        expect(12_000_000_000_000_000_000uL) { arrayOf(10_000_000_000_000_000_000uL, 2_000_000_000_000_000_000uL).sum() }
    }

    @Test
    fun sumInUnsignedArrays() {
        expect(14u) { uintArrayOf(2u, 3u, 9u).sum() }
        expect(400u) { ubyteArrayOf(200u, 200u).sum() }
        expect(50000u) { ushortArrayOf(20000u, 30000u).sum() }
        expect(12_000_000_000_000_000_000uL) { ulongArrayOf(10_000_000_000_000_000_000uL, 2_000_000_000_000_000_000uL).sum() }
    }

    @Test
    fun fold() {
        expect(6u) { ubyteArrayOf(1u, 2u, 3u).fold(0u) { acc, e -> acc + e } }
        expect(13u) { ushortArrayOf(1u, 2u, 3u).fold(1u) { acc, e -> acc + 2u * e } }
        expect(6u) { uintArrayOf(1u, 2u, 3u).fold(1u) { acc, e -> acc * e } }
        expect("0123") { ulongArrayOf(1u, 2u, 3u).fold("0") { acc, e -> "$acc$e" } }
    }

    @Test
    fun foldIndexed() {
        expect(8u) { ubyteArrayOf(1u, 2u, 3u).foldIndexed(0u) { i, acc, e -> acc + i.toUByte() * e } }
        expect(10) { ushortArrayOf(1u, 2u, 3u).foldIndexed(1) { i, acc, e -> acc + i + e.toInt() } }
        expect(15u) { uintArrayOf(1u, 2u, 3u).foldIndexed(1u) { i, acc, e -> acc * (i.toUInt() + e) } }
        expect(" 0-1 1-2 2-3") { ulongArrayOf(1u, 2u, 3u).foldIndexed("") { i, acc, e -> "$acc $i-$e" } }
    }

    @Test
    fun foldRight() {
        expect(6u) { ubyteArrayOf(1u, 2u, 3u).foldRight(0u) { e, acc -> acc + e } }
        expect(13u) { ushortArrayOf(1u, 2u, 3u).foldRight(1u) { e, acc -> acc + 2u * e } }
        expect(6u) { uintArrayOf(1u, 2u, 3u).foldRight(1u) { e, acc -> acc * e } }
        expect("0321") { ulongArrayOf(1u, 2u, 3u).foldRight("0") { e, acc -> "$acc$e" } }
    }

    @Test
    fun foldRightIndexed() {
        expect(8u) { ubyteArrayOf(1u, 2u, 3u).foldRightIndexed(0u) { i, e, acc -> acc + i.toUByte() * e } }
        expect(10) { ushortArrayOf(1u, 2u, 3u).foldRightIndexed(1) { i, e, acc -> acc + i + e.toInt() } }
        expect(15u) { uintArrayOf(1u, 2u, 3u).foldRightIndexed(1u) { i, e, acc -> acc * (i.toUInt() + e) } }
        expect(" 2-3 1-2 0-1") { ulongArrayOf(1u, 2u, 3u).foldRightIndexed("") { i, e, acc -> "$acc $i-$e" } }
    }

    @Test
    fun scan() {
        for (size in 0 until 4) {
            val expected = listOf("", "0", "01", "012", "0123").subList(0, size + 1)
            assertEquals(expected, UByteArray(size) { it.toUByte() }.scan("") { acc, e -> acc + e })
            assertEquals(expected, UShortArray(size) { it.toUShort() }.scan("") { acc, e -> acc + e })
            assertEquals(expected, UIntArray(size) { it.toUInt() }.scan("") { acc, e -> acc + e })
            assertEquals(expected, ULongArray(size) { it.toULong() }.scan("") { acc, e -> acc + e })
        }
    }

    @Test
    fun runningFold() {
        for (size in 0 until 4) {
            val expected = listOf("", "0", "01", "012", "0123").subList(0, size + 1)
            assertEquals(expected, UByteArray(size) { it.toUByte() }.runningFold("") { acc, e -> acc + e })
            assertEquals(expected, UShortArray(size) { it.toUShort() }.runningFold("") { acc, e -> acc + e })
            assertEquals(expected, UIntArray(size) { it.toUInt() }.runningFold("") { acc, e -> acc + e })
            assertEquals(expected, ULongArray(size) { it.toULong() }.runningFold("") { acc, e -> acc + e })
        }
    }

    @Test
    fun scanIndexed() {
        for (size in 0 until 4) {
            val expected = listOf("+", "+[0: a]", "+[0: a][1: b]", "+[0: a][1: b][2: c]", "+[0: a][1: b][2: c][3: d]").subList(0, size + 1)
            assertEquals(
                expected,
                UByteArray(size) { it.toUByte() }.scanIndexed("+") { index, acc, e -> "$acc[$index: ${'a' + e.toInt()}]" }
            )
            assertEquals(
                expected,
                UShortArray(size) { it.toUShort() }.scanIndexed("+") { index, acc, e -> "$acc[$index: ${'a' + e.toInt()}]" }
            )
            assertEquals(
                expected,
                UIntArray(size) { it.toUInt() }.scanIndexed("+") { index, acc, e -> "$acc[$index: ${'a' + e.toInt()}]" }
            )
            assertEquals(
                expected,
                ULongArray(size) { it.toULong() }.scanIndexed("+") { index, acc, e -> "$acc[$index: ${'a' + e.toInt()}]" }
            )
        }
    }

    @Test
    fun runningFoldIndexed() {
        for (size in 0 until 4) {
            val expected = listOf("+", "+[0: a]", "+[0: a][1: b]", "+[0: a][1: b][2: c]", "+[0: a][1: b][2: c][3: d]").subList(0, size + 1)
            assertEquals(
                expected,
                UByteArray(size) { it.toUByte() }.runningFoldIndexed("+") { index, acc, e -> "$acc[$index: ${'a' + e.toInt()}]" }
            )
            assertEquals(
                expected,
                UShortArray(size) { it.toUShort() }.runningFoldIndexed("+") { index, acc, e -> "$acc[$index: ${'a' + e.toInt()}]" }
            )
            assertEquals(
                expected,
                UIntArray(size) { it.toUInt() }.runningFoldIndexed("+") { index, acc, e -> "$acc[$index: ${'a' + e.toInt()}]" }
            )
            assertEquals(
                expected,
                ULongArray(size) { it.toULong() }.runningFoldIndexed("+") { index, acc, e -> "$acc[$index: ${'a' + e.toInt()}]" }
            )
        }
    }

    @Test
    fun runningReduce() {
        for (size in 0 until 4) {
            val expected = listOf(0, 1, 3, 6).subList(0, size)
            assertEquals(
                expected.map { it.toUByte() },
                UByteArray(size) { it.toUByte() }.runningReduce { acc, e -> (acc + e).toUByte() }
            )
            assertEquals(
                expected.map { it.toUShort() },
                UShortArray(size) { it.toUShort() }.runningReduce { acc, e -> (acc + e).toUShort() }
            )
            assertEquals(
                expected.map { it.toUInt() },
                UIntArray(size) { it.toUInt() }.runningReduce { acc, e -> acc + e }
            )
            assertEquals(
                expected.map { it.toULong() },
                ULongArray(size) { it.toULong() }.runningReduce { acc, e -> acc + e }
            )
        }
    }

    @Test
    fun runningReduceIndexed() {
        for (size in 0 until 4) {
            val expected = listOf(0, 1, 6, 27).subList(0, size)
            assertEquals(
                expected.map { it.toUByte() },
                UByteArray(size) { it.toUByte() }.runningReduceIndexed { index, acc, e -> (index.toUInt() * (acc + e)).toUByte() }
            )
            assertEquals(
                expected.map { it.toUShort() },
                UShortArray(size) { it.toUShort() }.runningReduceIndexed { index, acc, e -> (index.toUInt() * (acc + e)).toUShort() }
            )
            assertEquals(
                expected.map { it.toUInt() },
                UIntArray(size) { it.toUInt() }.runningReduceIndexed { index, acc, e -> index.toUInt() * (acc + e) }
            )
            assertEquals(
                expected.map { it.toULong() },
                ULongArray(size) { it.toULong() }.runningReduceIndexed { index, acc, e -> index.toULong() * (acc + e) }
            )
        }
    }

    @Test
    fun associateWithPrimitives() {
        assertEquals(
            mapOf(1u to "1", 2u to "2", 3u to "3"),
            uintArrayOf(1u, 2u, 3u).associateWith { it.toString() }
        )
        assertEquals(
            mapOf(1.toUByte() to "1", 2.toUByte() to "2", 3.toUByte() to "3"),
            ubyteArrayOf(1u, 2u, 3u).associateWith { it.toString() }
        )
        assertEquals(
            mapOf(1.toUShort() to "1", 2.toUShort() to "2", 3.toUShort() to "3"),
            ushortArrayOf(1u, 2u, 3u).associateWith { it.toString() }
        )
        assertEquals(
            mapOf(1UL to "1", 2UL to "2", 3UL to "3"),
            ulongArrayOf(1u, 2u, 3u).associateWith { it.toString() }
        )
    }

    @Test
    fun associateWithToPrimitives() {
        val expected = mapOf(1u to "one", 2u to "two", 3u to "three")
        assertEquals(
            mapOf(1u to "one", 2u to "2", 3u to "3"),
            uintArrayOf(2u, 3u).associateWithTo(expected.toMutableMap()) { it.toString() }
        )
        assertEquals(
            mapOf(1.toUByte() to "one", 2.toUByte() to "2", 3.toUByte() to "3"),
            ubyteArrayOf(2u, 3u).associateWithTo(expected.mapKeys { it.key.toUByte() }.toMutableMap()) { it.toString() }
        )
        assertEquals(
            mapOf(1.toUShort() to "one", 2.toUShort() to "2", 3.toUShort() to "3"),
            ushortArrayOf(2u, 3u).associateWithTo(expected.mapKeys { it.key.toUShort() }.toMutableMap()) { it.toString() }
        )
        assertEquals(
            mapOf(1UL to "one", 2UL to "2", 3UL to "3"),
            ulongArrayOf(2u, 3u).associateWithTo(expected.mapKeys { it.key.toULong() }.toMutableMap()) { it.toString() }
        )
    }

    @Test
    fun elementAt() {
        expect(0u) { ubyteArrayOf(0u, 1u, 2u).elementAt(0) }
        expect(1u) { ushortArrayOf(0u, 1u, 2u).elementAt(1) }
        expect(2u) { uintArrayOf(0u, 1u, 2u).elementAt(2) }

        assertFailsWith<IndexOutOfBoundsException> { uintArrayOf().elementAt(0) }
        assertFailsWith<IndexOutOfBoundsException> { ulongArrayOf(0u, 1u, 2u).elementAt(-1) }
    }

    @Test
    fun elementAtOrElse() {
        expect(0u) { ubyteArrayOf(0u, 1u, 2u).elementAtOrElse(0) { UByte.MAX_VALUE } }
        expect(UShort.MAX_VALUE) { ushortArrayOf(0u, 1u, 2u).elementAtOrElse(-1) { UShort.MAX_VALUE } }
        expect(UInt.MAX_VALUE) { uintArrayOf(0u, 1u, 2u).elementAtOrElse(3) { UInt.MAX_VALUE } }
        expect(ULong.MAX_VALUE) { ulongArrayOf(0u, 1u, 2u).elementAtOrElse(100) { ULong.MAX_VALUE } }
    }

    @Test
    fun elementAtOrNull() {
        expect(0u) { ubyteArrayOf(0u, 1u, 2u).elementAtOrNull(0) }
        expect(null) { ushortArrayOf(0u, 1u, 2u).elementAtOrNull(-1) }
        expect(null) { uintArrayOf(0u, 1u, 2u).elementAtOrNull(3) }
        expect(null) { ulongArrayOf(0u, 1u, 2u).elementAtOrNull(100) }
    }

    @Test
    fun find() {
        expect(0u) { ubyteArrayOf(0u, 1u, 2u).find { it == 0.toUByte() } }
        expect(0u) { ushortArrayOf(0u, 1u, 2u).find { it % 2u == 0u } }
        expect(1u) { uintArrayOf(0u, 1u, 2u).find { it % 2u == 1u } }
        expect(null) { ulongArrayOf(0u, 1u, 2u).find { it == 3uL } }
    }

    @Test
    fun findLast() {
        expect(0u) { ubyteArrayOf(0u, 1u, 2u).findLast { it == 0.toUByte() } }
        expect(2u) { ushortArrayOf(0u, 1u, 2u).findLast { it % 2u == 0u } }
        expect(1u) { uintArrayOf(0u, 1u, 2u).findLast { it % 2u == 1u } }
        expect(null) { ulongArrayOf(0u, 1u, 2u).findLast { it == 3uL } }
    }

    @Test
    fun first() {
        expect(0u) { ubyteArrayOf(0u, 1u, 2u).first() }
        expect(0u) { ushortArrayOf(0u, 1u, 2u).first { it % 2u == 0u } }
        expect(1u) { uintArrayOf(0u, 1u, 2u).first { it % 2u == 1u } }
        assertFailsWith<NoSuchElementException> { uintArrayOf().first() }
        assertFailsWith<NoSuchElementException> { ulongArrayOf(0u, 1u, 2u).first { it == 3uL } }
    }

    @Test
    fun firstOrNull() {
        expect(0u) { ubyteArrayOf(0u, 1u, 2u).firstOrNull() }
        expect(0u) { ushortArrayOf(0u, 1u, 2u).firstOrNull { it % 2u == 0u } }
        expect(1u) { uintArrayOf(0u, 1u, 2u).firstOrNull { it % 2u == 1u } }
        expect(null) { uintArrayOf().firstOrNull() }
        expect(null) { ulongArrayOf(0u, 1u, 2u).firstOrNull { it == 3uL } }
    }

    @Test
    fun getOrElse() {
        expect(0u) { ubyteArrayOf(0u, 1u, 2u).getOrElse(0) { UByte.MAX_VALUE } }
        expect(UShort.MAX_VALUE) { ushortArrayOf(0u, 1u, 2u).getOrElse(-1) { UShort.MAX_VALUE } }
        expect(UInt.MAX_VALUE) { uintArrayOf(0u, 1u, 2u).getOrElse(3) { UInt.MAX_VALUE } }
        expect(ULong.MAX_VALUE) { ulongArrayOf(0u, 1u, 2u).getOrElse(100) { ULong.MAX_VALUE } }
    }

    @Test
    fun getOrNull() {
        expect(0u) { ubyteArrayOf(0u, 1u, 2u).getOrNull(0) }
        expect(null) { ushortArrayOf(0u, 1u, 2u).getOrNull(-1) }
        expect(null) { uintArrayOf(0u, 1u, 2u).getOrNull(3) }
        expect(null) { ulongArrayOf(0u, 1u, 2u).getOrNull(100) }
    }

    @Test
    fun last() {
        expect(2u) { ubyteArrayOf(0u, 1u, 2u).last() }
        expect(2u) { ushortArrayOf(0u, 1u, 2u).last { it % 2u == 0u } }
        expect(1u) { uintArrayOf(0u, 1u, 2u).last { it % 2u == 1u } }
        assertFailsWith<NoSuchElementException> { uintArrayOf().last() }
        assertFailsWith<NoSuchElementException> { ulongArrayOf(0u, 1u, 2u).last { it == 3uL } }
    }

    @Test
    fun lastOrNull() {
        expect(2u) { ubyteArrayOf(0u, 1u, 2u).lastOrNull() }
        expect(2u) { ushortArrayOf(0u, 1u, 2u).lastOrNull { it % 2u == 0u } }
        expect(1u) { uintArrayOf(0u, 1u, 2u).lastOrNull { it % 2u == 1u } }
        expect(null) { uintArrayOf().lastOrNull() }
        expect(null) { ulongArrayOf(0u, 1u, 2u).lastOrNull { it == 3uL } }
    }

    @Test
    fun single() {
        expect(0u) { ubyteArrayOf(0u).single() }
        expect(2u) { ushortArrayOf(0u, 1u, 2u).single { it == 2.toUShort() } }
        expect(1u) { uintArrayOf(0u, 1u, 2u).single { it % 2u == 1u } }
        assertFailsWith<NoSuchElementException> { uintArrayOf().single() }
        assertFailsWith<NoSuchElementException> { ulongArrayOf(0u, 1u, 2u).single { it == 3uL } }
        assertFailsWith<IllegalArgumentException> { ulongArrayOf(0u, 1u, 2u).single { it % 2uL == 0uL } }
    }

    @Test
    fun singleOrNull() {
        expect(0u) { ubyteArrayOf(0u).singleOrNull() }
        expect(2u) { ushortArrayOf(0u, 1u, 2u).singleOrNull { it == 2.toUShort() } }
        expect(1u) { uintArrayOf(0u, 1u, 2u).singleOrNull { it % 2u == 1u } }
        expect(null) { uintArrayOf().singleOrNull() }
        expect(null) { ulongArrayOf(0u, 1u, 2u).singleOrNull { it % 2uL == 0uL } }
    }

    @Test
    fun map() {
        assertEquals(listOf(), ubyteArrayOf().map { it })
        assertEquals(listOf<UShort>(1u, 2u, 3u), ushortArrayOf(1u, 2u, 3u).map { it })
        assertEquals(listOf<UInt>(2u, 4u, 6u), uintArrayOf(1u, 2u, 3u).map { 2u * it })
        assertEquals(listOf<ULong>(0u, 0u, 0u), ulongArrayOf(1u, 2u, 3u).map { 0uL })
    }

    @Test
    fun mapIndexed() {
        assertEquals(listOf(), ubyteArrayOf().mapIndexed { _, e -> e })
        assertEquals(listOf<UShort>(1u, 2u, 3u), ushortArrayOf(1u, 2u, 3u).mapIndexed { _, e -> e })
        assertEquals(listOf(0, 1, 2), uintArrayOf(1u, 2u, 3u).mapIndexed { index, _ -> index })
        assertEquals(listOf(0, 0, 0), ulongArrayOf(1u, 2u, 3u).mapIndexed { _, _ -> 0 })
    }

    @Test
    fun groupBy() {
        assertEquals(mapOf(), ubyteArrayOf().groupBy { k -> k })
        assertEquals(
            mapOf(
                1.toUShort() to listOf<UShort>(1u),
                2.toUShort() to listOf<UShort>(2u),
                3.toUShort() to listOf<UShort>(3u)
            ),
            ushortArrayOf(1u, 2u, 3u).groupBy { k -> k }
        )
        assertEquals(
            mapOf(
                0.toUInt() to listOf("2"),
                1.toUInt() to listOf("1", "3")
            ),
            uintArrayOf(1u, 2u, 3u).groupBy({ k -> k % 2u }, { v -> v.toString() })
        )
        assertEquals(
            mapOf(
                0 to listOf(0, 0, 0)
            ),
            ulongArrayOf(1u, 2u, 3u).groupBy({ 0 }, { 0 })
        )
    }

    @Test
    fun flatMap() {
        assertEquals(listOf(), ubyteArrayOf().flatMap { listOf(it) })
        assertEquals(listOf<UShort>(1u, 2u, 3u), ushortArrayOf(1u, 2u, 3u).flatMap { listOf(it) })
        assertEquals(listOf<UInt>(1u, 1u, 2u, 2u, 3u, 3u), uintArrayOf(1u, 2u, 3u).flatMap { listOf(it, it) })
        assertEquals(listOf(), ulongArrayOf(1u, 2u, 3u).flatMap { listOf<ULong>() })
    }

    @Test
    fun flatMapIndexed() {
        assertEquals(listOf(), ubyteArrayOf().flatMapIndexed { index, _ -> listOf(index) })
        assertEquals(listOf<UShort>(2u, 3u, 3u), ushortArrayOf(1u, 2u, 3u).flatMapIndexed { index, e -> List(index) { e } })
        assertEquals(listOf<UInt>(2u, 2u, 3u, 3u, 3u, 3u), uintArrayOf(1u, 2u, 3u).flatMapIndexed { index, e -> List(index * 2) { e } })
        assertEquals(listOf(), ulongArrayOf(1u, 2u, 3u).flatMapIndexed { _, _ -> listOf<ULong>() })
    }

    @Test
    fun withIndex() {
        fun <T> assertIterableContentEquals(expected: Iterable<T>, actual: Iterable<T>) {
            compare(expected.iterator(), actual.iterator()) { iteratorBehavior() }
        }

        assertIterableContentEquals(listOf(), ubyteArrayOf().withIndex())
        assertIterableContentEquals(
            listOf(
                IndexedValue(0, 1.toUShort()),
                IndexedValue(1, 2.toUShort()),
                IndexedValue(2, 3.toUShort())
            ),
            ushortArrayOf(1u, 2u, 3u).withIndex()
        )
        assertEquals(IndexedValue(1, 2.toUInt()), uintArrayOf(1u, 2u, 3u).withIndex().minByOrNull { it.value % 2u })
        assertIterableContentEquals(listOf(0, 1, 2), ulongArrayOf(1u, 2u, 3u).withIndex().map { it.index })
    }

    @Test
    fun zip() {
        assertEquals(listOf(), ubyteArrayOf().zip(ubyteArrayOf()))
        assertEquals(
            listOf(
                1.toUShort() to 1.toUShort(),
                2.toUShort() to 2.toUShort()
            ),
            ushortArrayOf(1u, 2u, 3u).zip(ushortArrayOf(1u, 2u))
        )
        assertEquals(
            listOf("1a", "2b", "3c"),
            uintArrayOf(1u, 2u, 3u).zip(arrayOf("a", "b", "c", "d")) { a, b -> a.toString() + b }
        )
        assertEquals(
            listOf<ULong>(11u, 12u, 13u),
            ulongArrayOf(1u, 2u, 3u).zip(listOf<ULong>(10u, 10u, 10u)) { a, b -> a + b }
        )
    }

    @Test
    fun onEach() {
        assertEquals(listOf<UInt>(1u, 2u, 3u), mutableListOf<UInt>().apply { uintArrayOf(1u, 2u, 3u).onEach { add(it) } })
        assertEquals(listOf<UByte>(1u, 2u, 3u), mutableListOf<UByte>().apply { ubyteArrayOf(1u, 2u, 3u).onEach { add(it) } })
        assertEquals(listOf<UShort>(1u, 2u, 3u), mutableListOf<UShort>().apply { ushortArrayOf(1u, 2u, 3u).onEach { add(it) } })
        assertEquals(listOf<ULong>(1u, 2u, 3u), mutableListOf<ULong>().apply { ulongArrayOf(1u, 2u, 3u).onEach { add(it) } })
    }

    @Test
    fun onEachIndexed() {
        assertEquals(listOf<UInt>(1u, 3u, 5u), mutableListOf<UInt>().apply { uintArrayOf(1u, 2u, 3u).onEachIndexed { i, e -> add(i.toUInt() + e) } })
        assertEquals(listOf<UInt>(1u, 3u, 5u), mutableListOf<UInt>().apply { ubyteArrayOf(1u, 2u, 3u).onEachIndexed { i, e -> add(i.toUByte() + e) } })
        assertEquals(listOf<UInt>(1u, 3u, 5u), mutableListOf<UInt>().apply { ushortArrayOf(1u, 2u, 3u).onEachIndexed { i, e -> add(i.toUShort() + e) } })
        assertEquals(listOf<ULong>(1u, 3u, 5u), mutableListOf<ULong>().apply { ulongArrayOf(1u, 2u, 3u).onEachIndexed { i, e -> add(i.toULong() + e) } })

        val empty = arrayOf<UInt>()
        assertSame(empty, empty.onEachIndexed { i, e -> fail("Should be unreachable: $i, $e") })

        // Identity equality for arguments of types ULongArray and ULongArray is forbidden
//        val nonEmpty = ulongArrayOf(1, 2, 3)
//        assertSame(nonEmpty, nonEmpty.onEachIndexed { _, _ -> })
    }

    @Test
    fun drop() {
        expect(listOf(1.toUByte())) { ubyteArrayOf(1u).drop(0) }
        expect(listOf()) { ushortArrayOf().drop(1) }
        expect(listOf()) { uintArrayOf(1u).drop(1) }
        expect(listOf(3uL)) { ulongArrayOf(2u, 3u).drop(1) }
        assertFails {
            uintArrayOf(2u).drop(-1)
        }
    }

    @Test
    fun dropLast() {
        expect(listOf()) { ubyteArrayOf().dropLast(1) }
        expect(listOf()) { ushortArrayOf(1u).dropLast(1) }
        expect(listOf(1u)) { uintArrayOf(1u).dropLast(0) }
        expect(listOf(2uL)) { ulongArrayOf(2u, 3u).dropLast(1) }
        assertFails {
            ulongArrayOf(1u).dropLast(-1)
        }
    }

    @Test
    fun dropWhile() {
        expect(listOf(3.toUByte(), 1.toUByte())) { ubyteArrayOf(2u, 3u, 1u).dropWhile { it < 3u } }
        expect(listOf()) { ushortArrayOf().dropWhile { it < 3u } }
        expect(listOf()) { uintArrayOf(1u).dropWhile { it < 3u } }
        expect(listOf(3uL, 1uL)) { ulongArrayOf(2u, 3u, 1u).dropWhile { it < 3uL } }
    }

    @Test
    fun dropLastWhile() {
        expect(listOf(2.toUByte(), 3.toUByte())) { ubyteArrayOf(2u, 3u, 1u).dropLastWhile { it < 3u } }
        expect(listOf()) { ushortArrayOf().dropLastWhile { it < 3u } }
        expect(listOf()) { uintArrayOf(1u).dropLastWhile { it < 3u } }
        expect(listOf(2uL, 3uL)) { ulongArrayOf(2u, 3u, 1u).dropLastWhile { it < 3uL } }
    }

    @Test
    fun take() {
        expect(listOf()) { ubyteArrayOf().take(1) }
        expect(listOf()) { ushortArrayOf(1u).take(0) }
        expect(listOf(1u)) { uintArrayOf(1u).take(1) }
        expect(listOf(2uL)) { ulongArrayOf(2u, 3u).take(1) }
        assertFails {
            ubyteArrayOf(1u).take(-1)
        }
    }

    @Test
    fun takeLast() {
        expect(listOf()) { ubyteArrayOf().takeLast(1) }
        expect(listOf()) { ushortArrayOf(1u).takeLast(0) }
        expect(listOf(1u)) { uintArrayOf(1u).takeLast(1) }
        expect(listOf(3uL)) { ulongArrayOf(2u, 3u).takeLast(1) }
        assertFails {
            ushortArrayOf(1u).takeLast(-1)
        }
    }

    @Test
    fun takeWhile() {
        expect(listOf(2.toUByte())) { ubyteArrayOf(2u, 3u, 1u).takeWhile { it < 3u } }
        expect(listOf()) { ushortArrayOf().takeWhile { it < 3u } }
        expect(listOf(1u)) { uintArrayOf(1u).takeWhile { it < 3u } }
        expect(listOf(2uL)) { ulongArrayOf(2u, 3u, 1u).takeWhile { it < 3uL } }
    }

    @Test
    fun takeLastWhile() {
        expect(listOf()) { ubyteArrayOf().takeLastWhile { it < 3u } }
        expect(listOf(1.toUShort())) { ushortArrayOf(1u).takeLastWhile { it < 3u } }
        expect(listOf(1u)) { uintArrayOf(2u, 3u, 1u).takeLastWhile { it < 3u } }
        expect(listOf(1uL)) { ulongArrayOf(2u, 3u, 1u).takeLastWhile { it < 3uL } }
    }

    @Test
    fun filter() {
        expect(listOf(3.toByte())) { byteArrayOf(2, 3).filter { it > 2 } }
        expect(listOf()) { ushortArrayOf().filter { it > 2u } }
        expect(listOf()) { uintArrayOf(1u).filter { it > 2u } }
        expect(listOf(3uL)) { ulongArrayOf(2u, 3u).filter { it > 2uL } }
    }

    @Test
    fun filterIndexed() {
        expect(listOf<UByte>(2u, 5u, 8u)) {
            ubyteArrayOf(2u, 4u, 3u, 5u, 8u).filterIndexed { index, value -> index % 2 == (value % 2u).toInt() }
        }
        expect(listOf()) {
            ushortArrayOf().filterIndexed { i, v -> i > v.toInt() }
        }
        expect(listOf(2u, 5u, 8u)) {
            uintArrayOf(2u, 4u, 3u, 5u, 8u).filterIndexed { index, value -> index % 2 == (value % 2u).toInt() }
        }
        expect(listOf<ULong>(2u, 5u, 8u)) {
            ulongArrayOf(2u, 4u, 3u, 5u, 8u).filterIndexed { index, value -> index % 2 == (value % 2uL).toInt() }
        }
    }

    @Test
    fun filterNot() {
        expect(listOf(2.toUByte())) { ubyteArrayOf(2u, 3u).filterNot { it > 2u } }
        expect(listOf()) { ushortArrayOf().filterNot { it > 2u } }
        expect(listOf(1u)) { uintArrayOf(1u).filterNot { it > 2u } }
        expect(listOf(2uL)) { ulongArrayOf(2u, 3u).filterNot { it > 2uL } }
    }

    @Test
    fun sort() {
        val ubyteArray = ubyteArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UByte.MAX_VALUE, 250u)
        assertArrayContentEquals(ubyteArrayOf(0u, 1u, 2u, 5u, 9u, 80u, 250u, UByte.MAX_VALUE), ubyteArray.apply { sort() })

        val ushortArray = ushortArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UShort.MAX_VALUE, 65501u)
        assertArrayContentEquals(ushortArrayOf(0u, 1u, 2u, 5u, 9u, 80u, 65501u, UShort.MAX_VALUE), ushortArray.apply { sort() })

        val uintArray = uintArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UInt.MAX_VALUE, 4294967200u)
        assertArrayContentEquals(uintArrayOf(0u, 1u, 2u, 5u, 9u, 80u, 4294967200u, UInt.MAX_VALUE), uintArray.apply { sort() })

        val ulongArray = ulongArrayOf(5u, 2u, 1u, 9u, 80u, 0u, ULong.MAX_VALUE, ULong.MAX_VALUE - 123u)
        assertArrayContentEquals(ulongArrayOf(0u, 1u, 2u, 5u, 9u, 80u, ULong.MAX_VALUE - 123u, ULong.MAX_VALUE), ulongArray.apply { sort() })
    }

    @Test
    fun sortDescending() {
        val ubyteArray = ubyteArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UByte.MAX_VALUE, 250u)
        assertArrayContentEquals(ubyteArrayOf(UByte.MAX_VALUE, 250u, 80u, 9u, 5u, 2u, 1u, 0u), ubyteArray.apply { sortDescending() })

        val ushortArray = ushortArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UShort.MAX_VALUE, 65501u)
        assertArrayContentEquals(ushortArrayOf(UShort.MAX_VALUE, 65501u, 80u, 9u, 5u, 2u, 1u, 0u), ushortArray.apply { sortDescending() })

        val uintArray = uintArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UInt.MAX_VALUE, 4294967200u)
        assertArrayContentEquals(uintArrayOf(UInt.MAX_VALUE, 4294967200u, 80u, 9u, 5u, 2u, 1u, 0u), uintArray.apply { sortDescending() })

        val ulongArray = ulongArrayOf(5u, 2u, 1u, 9u, 80u, 0u, ULong.MAX_VALUE, ULong.MAX_VALUE - 123u)
        assertArrayContentEquals(ulongArrayOf(ULong.MAX_VALUE, ULong.MAX_VALUE - 123u, 80u, 9u, 5u, 2u, 1u, 0u), ulongArray.apply { sortDescending() })
    }

    @Test
    fun sorted() {
        assertTrue(uintArrayOf().sorted().none())
        assertEquals(listOf(1u), uintArrayOf(1u).sorted())

        val ubyteArray = ubyteArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UByte.MAX_VALUE, 250u)
        assertEquals(listOf<UByte>(0u, 1u, 2u, 5u, 9u, 80u, 250u, UByte.MAX_VALUE), ubyteArray.sorted())

        val ushortArray = ushortArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UShort.MAX_VALUE, 65501u)
        assertEquals(listOf<UShort>(0u, 1u, 2u, 5u, 9u, 80u, 65501u, UShort.MAX_VALUE), ushortArray.sorted())

        val uintArray = uintArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UInt.MAX_VALUE, 4294967200u)
        assertEquals(listOf<UInt>(0u, 1u, 2u, 5u, 9u, 80u, 4294967200u, UInt.MAX_VALUE), uintArray.sorted())

        val ulongArray = ulongArrayOf(5u, 2u, 1u, 9u, 80u, 0u, ULong.MAX_VALUE, ULong.MAX_VALUE - 123u)
        assertEquals(listOf<ULong>(0u, 1u, 2u, 5u, 9u, 80u, ULong.MAX_VALUE - 123u, ULong.MAX_VALUE), ulongArray.sorted())
    }

    @Test
    fun sortedDescending() {
        assertTrue(uintArrayOf().sortedDescending().none())
        assertEquals(listOf(1u), uintArrayOf(1u).sortedDescending())

        val ubyteArray = ubyteArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UByte.MAX_VALUE, 250u)
        assertEquals(listOf<UByte>(UByte.MAX_VALUE, 250u, 80u, 9u, 5u, 2u, 1u, 0u), ubyteArray.sortedDescending())

        val ushortArray = ushortArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UShort.MAX_VALUE, 65501u)
        assertEquals(listOf<UShort>(UShort.MAX_VALUE, 65501u, 80u, 9u, 5u, 2u, 1u, 0u), ushortArray.sortedDescending())

        val uintArray = uintArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UInt.MAX_VALUE, 4294967200u)
        assertEquals(listOf<UInt>(UInt.MAX_VALUE, 4294967200u, 80u, 9u, 5u, 2u, 1u, 0u), uintArray.sortedDescending())

        val ulongArray = ulongArrayOf(5u, 2u, 1u, 9u, 80u, 0u, ULong.MAX_VALUE, ULong.MAX_VALUE - 123u)
        assertEquals(listOf<ULong>(ULong.MAX_VALUE, ULong.MAX_VALUE - 123u, 80u, 9u, 5u, 2u, 1u, 0u), ulongArray.sortedDescending())
    }

    @Test
    fun sortedBy() {
        assertTrue(uintArrayOf().sortedBy { it.toString() }.none())
        assertEquals(listOf(1u), uintArrayOf(1u).sortedBy { it.toString() })

        val ubyteArray = ubyteArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UByte.MAX_VALUE, 250u)
        assertEquals(listOf<UByte>(250u, UByte.MAX_VALUE, 0u, 1u, 2u, 5u, 9u, 80u), ubyteArray.sortedBy { it.toByte() })

        val ushortArray = ushortArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UShort.MAX_VALUE, 65501u)
        assertEquals(listOf<UShort>(65501u, UShort.MAX_VALUE, 0u, 1u, 2u, 5u, 9u, 80u), ushortArray.sortedBy { it.toShort() })

        val uintArray = uintArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UInt.MAX_VALUE, 4294967200u)
        assertEquals(listOf<UInt>(4294967200u, UInt.MAX_VALUE, 0u, 1u, 2u, 5u, 9u, 80u), uintArray.sortedBy { it.toInt() })

        val ulongArray = ulongArrayOf(5u, 2u, 1u, 9u, 80u, 0u, ULong.MAX_VALUE, ULong.MAX_VALUE - 123u)
        assertEquals(listOf<ULong>(ULong.MAX_VALUE - 123u, ULong.MAX_VALUE, 0u, 1u, 2u, 5u, 9u, 80u), ulongArray.sortedBy { it.toLong() })
    }

    @Test
    fun sortedArray() {
        val ubyteArray = ubyteArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UByte.MAX_VALUE, 250u)
        assertArrayContentEquals(ubyteArrayOf(0u, 1u, 2u, 5u, 9u, 80u, 250u, UByte.MAX_VALUE), ubyteArray.sortedArray())

        val ushortArray = ushortArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UShort.MAX_VALUE, 65501u)
        assertArrayContentEquals(ushortArrayOf(0u, 1u, 2u, 5u, 9u, 80u, 65501u, UShort.MAX_VALUE), ushortArray.sortedArray())

        val uintArray = uintArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UInt.MAX_VALUE, 4294967200u)
        assertArrayContentEquals(uintArrayOf(0u, 1u, 2u, 5u, 9u, 80u, 4294967200u, UInt.MAX_VALUE), uintArray.sortedArray())

        val ulongArray = ulongArrayOf(5u, 2u, 1u, 9u, 80u, 0u, ULong.MAX_VALUE, ULong.MAX_VALUE - 123u)
        assertArrayContentEquals(ulongArrayOf(0u, 1u, 2u, 5u, 9u, 80u, ULong.MAX_VALUE - 123u, ULong.MAX_VALUE), ulongArray.sortedArray())
    }

    @Test
    fun sortedArrayDescending() {
        val ubyteArray = ubyteArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UByte.MAX_VALUE, 250u)
        assertArrayContentEquals(ubyteArrayOf(UByte.MAX_VALUE, 250u, 80u, 9u, 5u, 2u, 1u, 0u), ubyteArray.sortedArrayDescending())

        val ushortArray = ushortArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UShort.MAX_VALUE, 65501u)
        assertArrayContentEquals(ushortArrayOf(UShort.MAX_VALUE, 65501u, 80u, 9u, 5u, 2u, 1u, 0u), ushortArray.sortedArrayDescending())

        val uintArray = uintArrayOf(5u, 2u, 1u, 9u, 80u, 0u, UInt.MAX_VALUE, 4294967200u)
        assertArrayContentEquals(uintArrayOf(UInt.MAX_VALUE, 4294967200u, 80u, 9u, 5u, 2u, 1u, 0u), uintArray.sortedArrayDescending())

        val ulongArray = ulongArrayOf(5u, 2u, 1u, 9u, 80u, 0u, ULong.MAX_VALUE, ULong.MAX_VALUE - 123u)
        assertArrayContentEquals(ulongArrayOf(ULong.MAX_VALUE, ULong.MAX_VALUE - 123u, 80u, 9u, 5u, 2u, 1u, 0u), ulongArray.sortedArrayDescending())
    }

    @Test
    fun fill() {
        fun <A, E> testFailures(array: A, fill: A.(E, Int, Int) -> Unit, element: E, arraySize: Int) {
            assertFailsWith<IndexOutOfBoundsException> {
                array.fill(element, -1, arraySize)
            }
            assertFailsWith<IndexOutOfBoundsException> {
                array.fill(element, 0, arraySize + 1)
            }
            assertFailsWith<IllegalArgumentException> {
                array.fill(element, 1, 0)
            }
        }

        testFailures(UByteArray(5) { it.toUByte() }, UByteArray::fill, 0u, 5)
        testFailures(UShortArray(5) { it.toUShort() }, UShortArray::fill, 0u, 5)
        testFailures(UIntArray(5) { it.toUInt() }, UIntArray::fill, 0u, 5)
        testFailures(ULongArray(5) { it.toULong() }, ULongArray::fill, 0u, 5)

        fun <A, E> test(
            array: UIntArray,
            fill: A.(E, Int, Int) -> Unit,
            operations: List<OperationOnRange<UInt, UIntArray>>,
            arrayTransform: UIntArray.() -> A,
            elementTransform: UInt.() -> E,
            contentEquals: A.(A) -> Boolean
        ) {
            for (o in operations) {
                val result = array.arrayTransform()
                result.fill(o.element.elementTransform(), o.fromIndex, o.toIndex)
                assertTrue(o.expectedResult.arrayTransform().contentEquals(result))
            }
        }

        val array = UIntArray(5) { it.toUInt() }

        val operations = listOf(
            OperationOnRange(5u, 1, 4, uintArrayOf(0u, 5u, 5u, 5u, 4u)),
            OperationOnRange(1u, 0, 5, uintArrayOf(1u, 1u, 1u, 1u, 1u)),
            OperationOnRange(2u, 0, 3, uintArrayOf(2u, 2u, 2u, 3u, 4u)),
            OperationOnRange(3u, 2, 5, uintArrayOf(0u, 1u, 3u, 3u, 3u))
        )

        test(array, UByteArray::fill, operations, UIntArray::toUByteArray, UInt::toUByte, UByteArray::contentEquals)
        test(array, UShortArray::fill, operations, UIntArray::toUShortArray, UInt::toUShort, UShortArray::contentEquals)
        test(array, UIntArray::fill, operations, UIntArray::copyOf, UInt::toUInt, UIntArray::contentEquals)
        test(array, ULongArray::fill, operations, UIntArray::toULongArray, UInt::toULong, ULongArray::contentEquals)
    }

    private class OperationOnRange<E, R>(
        val element: E,
        val fromIndex: Int,
        val toIndex: Int,
        val expectedResult: R
    )
}


private fun UIntArray.toUByteArray(): UByteArray {
    return UByteArray(size) { get(it).toUByte() }
}

private fun UIntArray.toUShortArray(): UShortArray {
    return UShortArray(size) { get(it).toUShort() }
}

private fun UIntArray.toULongArray(): ULongArray {
    return ULongArray(size) { get(it).toULong() }
}
