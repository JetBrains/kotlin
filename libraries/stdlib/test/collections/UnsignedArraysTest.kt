/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED")
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
        compare(listOf<UShort>(1), ushortArrayOf(1)) { collectionBehavior() }
        compare(listOf<UInt>(1, 2), uintArrayOf(1u, 2u)) { collectionBehavior() }
        compare(listOf<ULong>(1, 2, 3), ulongArrayOf(1u, 2u, 3u)) { collectionBehavior() }
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
        assertEquals(3.0, ubyteArrayOf(0, 1, 2).sumByDouble { it.toDouble() })
        assertEquals(1.0, ushortArrayOf(0, 1, 2).sumByDouble { (it % 2u).toDouble() })
        assertEquals(0.0, uintArrayOf(0, 2, 4).sumByDouble { (it % 2u).toDouble() })
        assertEquals(6.0, ulongArrayOf(2, 3, 4).sumByDouble { (it - 1u).toDouble() })
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
        compare(listOf<UShort>(1), ushortArrayOf(1.toUShort()).asList()) { listBehavior() }
        compare(listOf<UInt>(1, 2), uintArrayOf(1u, 2u).asList()) { listBehavior() }
        compare(listOf<UInt>(1, 2, 3), uintArrayOf(1u, 2u, 3u).asList()) { listBehavior() }

        val ulongs = ulongArrayOf(1uL, 5uL, 7uL)
        val ulongsAsList = ulongs.asList()
        assertEquals(5uL, ulongsAsList[1])
        ulongs[1] = 10
        assertEquals(10uL, ulongsAsList[1], "Should reflect changes in original array")
    }

    @Test
    fun slice() {
        assertEquals(listOf<UInt>(), uintArrayOf(1, 2, 3).slice(5..1))
        assertEquals(listOf<UInt>(2, 3, 9), uintArrayOf(2, 3, 9, 2, 3, 9).slice(listOf(3, 1, 2)))
        assertEquals(listOf<UByte>(127, 100), ubyteArrayOf(50, 100, 127).slice(2 downTo 1))
        assertEquals(listOf<UShort>(200, 100), ushortArrayOf(50, 100, 200).slice(2 downTo 1))
        assertEquals(listOf<ULong>(100, 200, 30), ulongArrayOf(50, 100, 200, 30).slice(1..3))

        for (range in listOf(-1 until 0, 0 until 2, 2..2)) {
            val bounds = "range: $range"
            val exClass = IndexOutOfBoundsException::class
            assertFailsWith(exClass, bounds) { uintArrayOf(1).slice(range) }
            assertFailsWith(exClass, bounds) { ulongArrayOf(1).slice(range) }
        }
    }

    @Test
    fun sliceArray() {
        assertArrayContentEquals(uintArrayOf(), uintArrayOf(1, 2, 3).sliceArray(5..1))
        assertArrayContentEquals(uintArrayOf(2, 3, 9), uintArrayOf(2, 3, 9, 2, 3, 9).sliceArray(listOf(3, 1, 2)))
        assertArrayContentEquals(ubyteArrayOf(127, 100), ubyteArrayOf(50, 100, 127).sliceArray(listOf(2, 1)))
        assertArrayContentEquals(ushortArrayOf(200, 100), ushortArrayOf(50, 100, 200).sliceArray(listOf(2, 1)))
        assertArrayContentEquals(ulongArrayOf(100, 200, 30), ulongArrayOf(50, 100, 200, 30).sliceArray(1..3))

        for (range in listOf(-1 until 0, 0 until 2, 2..2)) {
            val bounds = "range: $range"
            val exClass = IndexOutOfBoundsException::class
            assertFailsWith(exClass, bounds) { ubyteArrayOf(1).sliceArray(range) }
            assertFailsWith(exClass, bounds) { ushortArrayOf(1).sliceArray(range) }
        }
    }

    @Test
    fun minOrNull() {
        expect(null) { arrayOf<UByte>().minOrNull() }
        expect(1u) { arrayOf<UShort>(1).minOrNull() }
        expect(2u) { arrayOf<UInt>(2, 3).minOrNull() }
        expect(2uL) { arrayOf<ULong>(3, 2).minOrNull() }
    }

    @Test
    fun minOrNullInUnsignedArrays() {
        expect(null) { ubyteArrayOf().minOrNull() }
        expect(1u) { ushortArrayOf(1).minOrNull() }
        expect(2u) { uintArrayOf(2, 3).minOrNull() }
        expect(2uL) { ulongArrayOf(3, 2).minOrNull() }
    }

    @Test
    fun maxOrNull() {
        expect(null) { arrayOf<UByte>().maxOrNull() }
        expect(1u) { arrayOf<UShort>(1).maxOrNull() }
        expect(3u) { arrayOf<UInt>(2, 3).maxOrNull() }
        expect(3uL) { arrayOf<ULong>(3, 2).maxOrNull() }
    }

    @Test
    fun maxOrNullInUnsignedArrays() {
        expect(null) { ubyteArrayOf().maxOrNull() }
        expect(1u) { ushortArrayOf(1).maxOrNull() }
        expect(3u) { uintArrayOf(2, 3).maxOrNull() }
        expect(3uL) { ulongArrayOf(3, 2).maxOrNull() }
    }

    @Test
    fun minWitOrNullh() {
        expect(null) { arrayOf<UByte>().minWithOrNull(naturalOrder()) }
        expect(1u) { arrayOf<UShort>(1).minWithOrNull(naturalOrder()) }
        expect(2u) { arrayOf<UInt>(2, 3).minWithOrNull(naturalOrder()) }
        expect(2uL) { arrayOf<ULong>(3, 2).minWithOrNull(naturalOrder()) }
    }

    @Test
    fun minWithOrNullInUnsignedArrays() {
        expect(null) { ubyteArrayOf().minWithOrNull(reverseOrder()) }
        expect(1u) { ushortArrayOf(1).minWithOrNull(reverseOrder()) }
        expect(3u) { uintArrayOf(2, 3).minWithOrNull(reverseOrder()) }
        expect(3uL) { ulongArrayOf(3, 2).minWithOrNull(reverseOrder()) }
    }

    @Test
    fun maxWithOrNull() {
        expect(null) { arrayOf<UByte>().maxWithOrNull(naturalOrder()) }
        expect(1u) { arrayOf<UShort>(1).maxWithOrNull(naturalOrder()) }
        expect(3u) { arrayOf<UInt>(2, 3).maxWithOrNull(naturalOrder()) }
        expect(3uL) { arrayOf<ULong>(3, 2).maxWithOrNull(naturalOrder()) }
    }

    @Test
    fun maxWithOrNullInUnsignedArrays() {
        expect(null) { ubyteArrayOf().maxWithOrNull(reverseOrder()) }
        expect(1u) { ushortArrayOf(1).maxWithOrNull(reverseOrder()) }
        expect(2u) { uintArrayOf(2, 3).maxWithOrNull(reverseOrder()) }
        expect(2uL) { ulongArrayOf(3, 2).maxWithOrNull(reverseOrder()) }
    }

    @Test
    fun minByOrNull() {
        expect(null) { arrayOf<UByte>().minByOrNull { it * it } }
        expect(1u) { arrayOf<UShort>(1).minByOrNull { it * it } }
        expect(2u) { arrayOf<UInt>(2, 3).minByOrNull { it * it } }
        expect(3uL) { arrayOf<ULong>(3, 2).minByOrNull { it - 3 } }
    }

    @Test
    fun minByOrNullInUnsignedArrays() {
        expect(null) { ubyteArrayOf().minByOrNull { it * it } }
        expect(1u) { ushortArrayOf(1).minByOrNull { it * it } }
        expect(2u) { uintArrayOf(2, 3).minByOrNull { it * it } }
        expect(3uL) { ulongArrayOf(3, 2).minByOrNull { it - 3 } }
    }

    @Test
    fun maxByOrNull() {
        expect(null) { arrayOf<UByte>().maxByOrNull { it + 1 } }
        expect(1u) { arrayOf<UShort>(1).maxByOrNull { it + 1 } }
        expect(2u) { arrayOf<UInt>(2, 3).maxByOrNull { it - 3 } }
        expect(3uL) { arrayOf<ULong>(3, 2).maxByOrNull { it + 1 } }
    }

    @Test
    fun maxByOrNullInUnsignedArrays() {
        expect(null) { ubyteArrayOf().maxByOrNull { it + 1 } }
        expect(1u) { ushortArrayOf(1).maxByOrNull { it + 1 } }
        expect(2u) { uintArrayOf(2, 3).maxByOrNull { it - 3 } }
        expect(3uL) { ulongArrayOf(3, 2).maxByOrNull { it + 1 } }
    }

    @Test
    fun reduce() {
        expect(0u) { ubyteArrayOf(3, 2, 1).reduce { acc, e -> (acc - e).toUByte() } }
        expect(0u) { ushortArrayOf(3, 2, 1).reduce { acc, e -> (acc - e).toUShort() } }
        expect((-4).toUInt()) { uintArrayOf(1, 2, 3).reduce { acc, e -> acc - e } }
        expect((-4).toULong()) { ulongArrayOf(1, 2, 3).reduce { acc, e -> acc - e } }

        assertFailsWith<UnsupportedOperationException> {
            uintArrayOf().reduce { acc, e -> acc + e }
        }
    }

    @Test
    fun reduceOrNull() {
        expect(0u) { ubyteArrayOf(3, 2, 1).reduceOrNull { acc, e -> (acc - e).toUByte() } }
        expect(0u) { ushortArrayOf(3, 2, 1).reduceOrNull { acc, e -> (acc - e).toUShort() } }
        expect((-4).toUInt()) { uintArrayOf(1, 2, 3).reduceOrNull { acc, e -> acc - e } }
        expect((-4).toULong()) { ulongArrayOf(1, 2, 3).reduceOrNull { acc, e -> acc - e } }

        assertNull(uintArrayOf().reduceOrNull { acc, e -> acc + e })
    }

    @Test
    fun reduceIndexed() {
        expect(1u) { ubyteArrayOf(3, 2, 1).reduceIndexed { index, acc, e -> if (index != 2) (e - acc).toUByte() else e } }
        expect(1u) { ushortArrayOf(3, 2, 1).reduceIndexed { index, acc, e -> if (index != 2) (e - acc).toUShort() else e } }
        expect(UInt.MAX_VALUE) { uintArrayOf(1, 2, 3).reduceIndexed { index, acc, e -> index.toUInt() + acc - e } }
        expect(ULong.MAX_VALUE) { ulongArrayOf(1, 2, 3).reduceIndexed { index, acc, e -> index.toULong() + acc - e } }

        assertFailsWith<UnsupportedOperationException> {
            uintArrayOf().reduceIndexed { index, acc, e -> index.toUInt() + e + acc }
        }
    }

    @Test
    fun reduceIndexedOrNull() {
        expect(1u) { ubyteArrayOf(3, 2, 1).reduceIndexedOrNull { index, acc, e -> if (index != 2) (e - acc).toUByte() else e } }
        expect(1u) { ushortArrayOf(3, 2, 1).reduceIndexedOrNull { index, acc, e -> if (index != 2) (e - acc).toUShort() else e } }
        expect(UInt.MAX_VALUE) { uintArrayOf(1, 2, 3).reduceIndexedOrNull { index, acc, e -> index.toUInt() + acc - e } }
        expect(ULong.MAX_VALUE) { ulongArrayOf(1, 2, 3).reduceIndexedOrNull { index, acc, e -> index.toULong() + acc - e } }

        expect(null, { uintArrayOf().reduceIndexedOrNull { index, acc, e -> index.toUInt() + e + acc } })
    }

    @Test
    fun reduceRight() {
        expect(2u) { ubyteArrayOf(1, 2, 3).reduceRightOrNull { e, acc -> (e - acc).toUByte() } }
        expect(2u) { ushortArrayOf(1, 2, 3).reduceRightOrNull { e, acc -> (e - acc).toUShort() } }
        expect(2u) { uintArrayOf(1, 2, 3).reduceRightOrNull { e, acc -> e - acc } }
        expect(2uL) { ulongArrayOf(1, 2, 3).reduceRightOrNull { e, acc -> e - acc } }

        assertNull(uintArrayOf().reduceRightOrNull { e, acc -> e + acc })
    }

    @Test
    fun reduceRightOrNull() {
        expect(2u) { ubyteArrayOf(1, 2, 3).reduceRight { e, acc -> (e - acc).toUByte() } }
        expect(2u) { ushortArrayOf(1, 2, 3).reduceRight { e, acc -> (e - acc).toUShort() } }
        expect(2u) { uintArrayOf(1, 2, 3).reduceRight { e, acc -> e - acc } }
        expect(2uL) { ulongArrayOf(1, 2, 3).reduceRight { e, acc -> e - acc } }

        assertFailsWith<UnsupportedOperationException> {
            uintArrayOf().reduceRight { e, acc -> e + acc }
        }
    }

    @Test
    fun reduceRightIndexed() {
        expect(1u) { ubyteArrayOf(3, 2, 1).reduceRightIndexed { index, e, acc -> if (index != 1) (e - acc).toUByte() else e } }
        expect(1u) { ushortArrayOf(3, 2, 1).reduceRightIndexed { index, e, acc -> if (index != 1) (e - acc).toUShort() else e } }
        expect(1u) { uintArrayOf(1, 2, 3).reduceRightIndexed { index, e, acc -> index.toUInt() + e - acc } }
        expect(1uL) { ulongArrayOf(1, 2, 3).reduceRightIndexed { index, e, acc -> index.toULong() + e - acc } }

        assertFailsWith<UnsupportedOperationException> {
            uintArrayOf().reduceRightIndexed { index, e, acc -> index.toUInt() + e + acc }
        }
    }

    @Test
    fun reduceRightIndexedOrNull() {
        expect(1u) { ubyteArrayOf(3, 2, 1).reduceRightIndexedOrNull { index, e, acc -> if (index != 1) (e - acc).toUByte() else e } }
        expect(1u) { ushortArrayOf(3, 2, 1).reduceRightIndexedOrNull { index, e, acc -> if (index != 1) (e - acc).toUShort() else e } }
        expect(1u) { uintArrayOf(1, 2, 3).reduceRightIndexedOrNull { index, e, acc -> index.toUInt() + e - acc } }
        expect(1uL) { ulongArrayOf(1, 2, 3).reduceRightIndexedOrNull { index, e, acc -> index.toULong() + e - acc } }

        expect(null, { uintArrayOf().reduceRightIndexedOrNull { index, e, acc -> index.toUInt() + e + acc } })
    }

    @Test
    fun forEach() {
        var i = 0
        val a = ubyteArrayOf(3, 2, 1)
        a.forEach { e -> assertEquals(e, a[i++]) }
    }

    @Test
    fun forEachIndexed() {
        val a = ubyteArrayOf(3, 2, 1)
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
        expect(6u) { ubyteArrayOf(1, 2, 3).fold(0u) { acc, e -> acc + e } }
        expect(13u) { ushortArrayOf(1, 2, 3).fold(1u) { acc, e -> acc + 2u * e } }
        expect(6u) { uintArrayOf(1, 2, 3).fold(1u) { acc, e -> acc * e } }
        expect("0123") { ulongArrayOf(1, 2, 3).fold("0") { acc, e -> "$acc$e" } }
    }

    @Test
    fun foldIndexed() {
        expect(8u) { ubyteArrayOf(1, 2, 3).foldIndexed(0u) { i, acc, e -> acc + i.toUByte() * e } }
        expect(10) { ushortArrayOf(1, 2, 3).foldIndexed(1) { i, acc, e -> acc + i + e.toInt() } }
        expect(15u) { uintArrayOf(1, 2, 3).foldIndexed(1u) { i, acc, e -> acc * (i.toUInt() + e) } }
        expect(" 0-1 1-2 2-3") { ulongArrayOf(1, 2, 3).foldIndexed("") { i, acc, e -> "$acc $i-$e" } }
    }

    @Test
    fun foldRight() {
        expect(6u) { ubyteArrayOf(1, 2, 3).foldRight(0u) { e, acc -> acc + e } }
        expect(13u) { ushortArrayOf(1, 2, 3).foldRight(1u) { e, acc -> acc + 2u * e } }
        expect(6u) { uintArrayOf(1, 2, 3).foldRight(1u) { e, acc -> acc * e } }
        expect("0321") { ulongArrayOf(1, 2, 3).foldRight("0") { e, acc -> "$acc$e" } }
    }

    @Test
    fun foldRightIndexed() {
        expect(8u) { ubyteArrayOf(1, 2, 3).foldRightIndexed(0u) { i, e, acc -> acc + i.toUByte() * e } }
        expect(10) { ushortArrayOf(1, 2, 3).foldRightIndexed(1) { i, e, acc -> acc + i + e.toInt() } }
        expect(15u) { uintArrayOf(1, 2, 3).foldRightIndexed(1u) { i, e, acc -> acc * (i.toUInt() + e) } }
        expect(" 2-3 1-2 0-1") { ulongArrayOf(1, 2, 3).foldRightIndexed("") { i, e, acc -> "$acc $i-$e" } }
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
            uintArrayOf(1, 2, 3).associateWith { it.toString() }
        )
        assertEquals(
            mapOf(1.toUByte() to "1", 2.toUByte() to "2", 3.toUByte() to "3"),
            ubyteArrayOf(1, 2, 3).associateWith { it.toString() }
        )
        assertEquals(
            mapOf(1.toUShort() to "1", 2.toUShort() to "2", 3.toUShort() to "3"),
            ushortArrayOf(1, 2, 3).associateWith { it.toString() }
        )
        assertEquals(
            mapOf(1UL to "1", 2UL to "2", 3UL to "3"),
            ulongArrayOf(1, 2, 3).associateWith { it.toString() }
        )
    }

    @Test
    fun associateWithToPrimitives() {
        val expected = mapOf(1u to "one", 2u to "two", 3u to "three")
        assertEquals(
            mapOf(1u to "one", 2u to "2", 3u to "3"),
            uintArrayOf(2, 3).associateWithTo(expected.toMutableMap()) { it.toString() }
        )
        assertEquals(
            mapOf(1.toUByte() to "one", 2.toUByte() to "2", 3.toUByte() to "3"),
            ubyteArrayOf(2, 3).associateWithTo(expected.mapKeys { it.key.toUByte() }.toMutableMap()) { it.toString() }
        )
        assertEquals(
            mapOf(1.toUShort() to "one", 2.toUShort() to "2", 3.toUShort() to "3"),
            ushortArrayOf(2, 3).associateWithTo(expected.mapKeys { it.key.toUShort() }.toMutableMap()) { it.toString() }
        )
        assertEquals(
            mapOf(1UL to "one", 2UL to "2", 3UL to "3"),
            ulongArrayOf(2, 3).associateWithTo(expected.mapKeys { it.key.toULong() }.toMutableMap()) { it.toString() }
        )
    }

    @Test
    fun elementAt() {
        expect(0u) { ubyteArrayOf(0, 1, 2).elementAt(0) }
        expect(1u) { ushortArrayOf(0, 1, 2).elementAt(1) }
        expect(2u) { uintArrayOf(0, 1, 2).elementAt(2) }

        assertFailsWith<IndexOutOfBoundsException> { uintArrayOf().elementAt(0) }
        assertFailsWith<IndexOutOfBoundsException> { ulongArrayOf(0, 1, 2).elementAt(-1) }
    }

    @Test
    fun elementAtOrElse() {
        expect(0u) { ubyteArrayOf(0, 1, 2).elementAtOrElse(0) { UByte.MAX_VALUE } }
        expect(UShort.MAX_VALUE) { ushortArrayOf(0, 1, 2).elementAtOrElse(-1) { UShort.MAX_VALUE } }
        expect(UInt.MAX_VALUE) { uintArrayOf(0, 1, 2).elementAtOrElse(3) { UInt.MAX_VALUE } }
        expect(ULong.MAX_VALUE) { ulongArrayOf(0, 1, 2).elementAtOrElse(100) { ULong.MAX_VALUE } }
    }

    @Test
    fun elementAtOrNull() {
        expect(0u) { ubyteArrayOf(0, 1, 2).elementAtOrNull(0) }
        expect(null) { ushortArrayOf(0, 1, 2).elementAtOrNull(-1) }
        expect(null) { uintArrayOf(0, 1, 2).elementAtOrNull(3) }
        expect(null) { ulongArrayOf(0, 1, 2).elementAtOrNull(100) }
    }

    @Test
    fun find() {
        expect(0u) { ubyteArrayOf(0, 1, 2).find { it == 0.toUByte() } }
        expect(0u) { ushortArrayOf(0, 1, 2).find { it % 2u == 0u } }
        expect(1u) { uintArrayOf(0, 1, 2).find { it % 2u == 1u } }
        expect(null) { ulongArrayOf(0, 1, 2).find { it == 3uL } }
    }

    @Test
    fun findLast() {
        expect(0u) { ubyteArrayOf(0, 1, 2).findLast { it == 0.toUByte() } }
        expect(2u) { ushortArrayOf(0, 1, 2).findLast { it % 2u == 0u } }
        expect(1u) { uintArrayOf(0, 1, 2).findLast { it % 2u == 1u } }
        expect(null) { ulongArrayOf(0, 1, 2).findLast { it == 3uL } }
    }

    @Test
    fun first() {
        expect(0u) { ubyteArrayOf(0, 1, 2).first() }
        expect(0u) { ushortArrayOf(0, 1, 2).first { it % 2u == 0u } }
        expect(1u) { uintArrayOf(0, 1, 2).first { it % 2u == 1u } }
        assertFailsWith<NoSuchElementException> { uintArrayOf().first() }
        assertFailsWith<NoSuchElementException> { ulongArrayOf(0, 1, 2).first { it == 3uL } }
    }

    @Test
    fun firstOrNull() {
        expect(0u) { ubyteArrayOf(0, 1, 2).firstOrNull() }
        expect(0u) { ushortArrayOf(0, 1, 2).firstOrNull { it % 2u == 0u } }
        expect(1u) { uintArrayOf(0, 1, 2).firstOrNull { it % 2u == 1u } }
        expect(null) { uintArrayOf().firstOrNull() }
        expect(null) { ulongArrayOf(0, 1, 2).firstOrNull { it == 3uL } }
    }

    @Test
    fun getOrElse() {
        expect(0u) { ubyteArrayOf(0, 1, 2).getOrElse(0) { UByte.MAX_VALUE } }
        expect(UShort.MAX_VALUE) { ushortArrayOf(0, 1, 2).getOrElse(-1) { UShort.MAX_VALUE } }
        expect(UInt.MAX_VALUE) { uintArrayOf(0, 1, 2).getOrElse(3) { UInt.MAX_VALUE } }
        expect(ULong.MAX_VALUE) { ulongArrayOf(0, 1, 2).getOrElse(100) { ULong.MAX_VALUE } }
    }

    @Test
    fun getOrNull() {
        expect(0u) { ubyteArrayOf(0, 1, 2).getOrNull(0) }
        expect(null) { ushortArrayOf(0, 1, 2).getOrNull(-1) }
        expect(null) { uintArrayOf(0, 1, 2).getOrNull(3) }
        expect(null) { ulongArrayOf(0, 1, 2).getOrNull(100) }
    }

    @Test
    fun last() {
        expect(2u) { ubyteArrayOf(0, 1, 2).last() }
        expect(2u) { ushortArrayOf(0, 1, 2).last { it % 2u == 0u } }
        expect(1u) { uintArrayOf(0, 1, 2).last { it % 2u == 1u } }
        assertFailsWith<NoSuchElementException> { uintArrayOf().last() }
        assertFailsWith<NoSuchElementException> { ulongArrayOf(0, 1, 2).last { it == 3uL } }
    }

    @Test
    fun lastOrNull() {
        expect(2u) { ubyteArrayOf(0, 1, 2).lastOrNull() }
        expect(2u) { ushortArrayOf(0, 1, 2).lastOrNull { it % 2u == 0u } }
        expect(1u) { uintArrayOf(0, 1, 2).lastOrNull { it % 2u == 1u } }
        expect(null) { uintArrayOf().lastOrNull() }
        expect(null) { ulongArrayOf(0, 1, 2).lastOrNull { it == 3uL } }
    }

    @Test
    fun single() {
        expect(0u) { ubyteArrayOf(0).single() }
        expect(2u) { ushortArrayOf(0, 1, 2).single { it == 2.toUShort() } }
        expect(1u) { uintArrayOf(0, 1, 2).single { it % 2u == 1u } }
        assertFailsWith<NoSuchElementException> { uintArrayOf().single() }
        assertFailsWith<NoSuchElementException> { ulongArrayOf(0, 1, 2).single { it == 3uL } }
        assertFailsWith<IllegalArgumentException> { ulongArrayOf(0, 1, 2).single { it % 2uL == 0uL } }
    }

    @Test
    fun singleOrNull() {
        expect(0u) { ubyteArrayOf(0).singleOrNull() }
        expect(2u) { ushortArrayOf(0, 1, 2).singleOrNull { it == 2.toUShort() } }
        expect(1u) { uintArrayOf(0, 1, 2).singleOrNull { it % 2u == 1u } }
        expect(null) { uintArrayOf().singleOrNull() }
        expect(null) { ulongArrayOf(0, 1, 2).singleOrNull { it % 2uL == 0uL } }
    }

    @Test
    fun map() {
        assertEquals(listOf(), ubyteArrayOf().map { it })
        assertEquals(listOf<UShort>(1, 2, 3), ushortArrayOf(1, 2, 3).map { it })
        assertEquals(listOf<UInt>(2, 4, 6), uintArrayOf(1, 2, 3).map { 2u * it })
        assertEquals(listOf<ULong>(0, 0, 0), ulongArrayOf(1, 2, 3).map { 0uL })
    }

    @Test
    fun mapIndexed() {
        assertEquals(listOf(), ubyteArrayOf().mapIndexed { _, e -> e })
        assertEquals(listOf<UShort>(1, 2, 3), ushortArrayOf(1, 2, 3).mapIndexed { _, e -> e })
        assertEquals(listOf(0, 1, 2), uintArrayOf(1, 2, 3).mapIndexed { index, _ -> index })
        assertEquals(listOf(0, 0, 0), ulongArrayOf(1, 2, 3).mapIndexed { _, _ -> 0 })
    }

    @Test
    fun groupBy() {
        assertEquals(mapOf(), ubyteArrayOf().groupBy { k -> k })
        assertEquals(
            mapOf(
                1.toUShort() to listOf<UShort>(1),
                2.toUShort() to listOf<UShort>(2),
                3.toUShort() to listOf<UShort>(3)
            ),
            ushortArrayOf(1, 2, 3).groupBy { k -> k }
        )
        assertEquals(
            mapOf(
                0.toUInt() to listOf("2"),
                1.toUInt() to listOf("1", "3")
            ),
            uintArrayOf(1, 2, 3).groupBy({ k -> k % 2 }, { v -> v.toString() })
        )
        assertEquals(
            mapOf(
                0 to listOf(0, 0, 0)
            ),
            ulongArrayOf(1, 2, 3).groupBy({ 0 }, { 0 })
        )
    }

    @Test
    fun flatMap() {
        assertEquals(listOf(), ubyteArrayOf().flatMap { listOf(it) })
        assertEquals(listOf<UShort>(1, 2, 3), ushortArrayOf(1, 2, 3).flatMap { listOf(it) })
        assertEquals(listOf<UInt>(1, 1, 2, 2, 3, 3), uintArrayOf(1, 2, 3).flatMap { listOf(it, it) })
        assertEquals(listOf(), ulongArrayOf(1, 2, 3).flatMap { listOf<ULong>() })
    }

    @Test
    fun flatMapIndexed() {
        assertEquals(listOf(), ubyteArrayOf().flatMapIndexed { index, _ -> listOf(index) })
        assertEquals(listOf<UShort>(2, 3, 3), ushortArrayOf(1, 2, 3).flatMapIndexed { index, e -> List(index) { e } })
        assertEquals(listOf<UInt>(2, 2, 3, 3, 3, 3), uintArrayOf(1, 2, 3).flatMapIndexed { index, e -> List(index * 2) { e } })
        assertEquals(listOf(), ulongArrayOf(1, 2, 3).flatMapIndexed { _, _ -> listOf<ULong>() })
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
            ushortArrayOf(1, 2, 3).withIndex()
        )
        assertEquals(IndexedValue(1, 2.toUInt()), uintArrayOf(1, 2, 3).withIndex().minByOrNull { it.value % 2 })
        assertIterableContentEquals(listOf(0, 1, 2), ulongArrayOf(1, 2, 3).withIndex().map { it.index })
    }

    @Test
    fun zip() {
        assertEquals(listOf(), ubyteArrayOf().zip(ubyteArrayOf()))
        assertEquals(
            listOf(
                1.toUShort() to 1.toUShort(),
                2.toUShort() to 2.toUShort()
            ),
            ushortArrayOf(1, 2, 3).zip(ushortArrayOf(1, 2))
        )
        assertEquals(
            listOf("1a", "2b", "3c"),
            uintArrayOf(1, 2, 3).zip(arrayOf("a", "b", "c", "d")) { a, b -> a.toString() + b }
        )
        assertEquals(
            listOf<ULong>(11, 12, 13),
            ulongArrayOf(1, 2, 3).zip(listOf<ULong>(10, 10, 10)) { a, b -> a + b }
        )
    }

    @Test
    fun onEach() {
        assertEquals(listOf<UInt>(1, 2, 3), mutableListOf<UInt>().apply { uintArrayOf(1, 2, 3).onEach { add(it) } })
        assertEquals(listOf<UByte>(1, 2, 3), mutableListOf<UByte>().apply { ubyteArrayOf(1, 2, 3).onEach { add(it) } })
        assertEquals(listOf<UShort>(1, 2, 3), mutableListOf<UShort>().apply { ushortArrayOf(1, 2, 3).onEach { add(it) } })
        assertEquals(listOf<ULong>(1, 2, 3), mutableListOf<ULong>().apply { ulongArrayOf(1, 2, 3).onEach { add(it) } })
    }

    @Test
    fun onEachIndexed() {
        assertEquals(listOf<UInt>(1, 3, 5), mutableListOf<UInt>().apply { uintArrayOf(1, 2, 3).onEachIndexed { i, e -> add(i.toUInt() + e) } })
        assertEquals(listOf<UInt>(1, 3, 5), mutableListOf<UInt>().apply { ubyteArrayOf(1, 2, 3).onEachIndexed { i, e -> add(i.toUByte() + e) } })
        assertEquals(listOf<UInt>(1, 3, 5), mutableListOf<UInt>().apply { ushortArrayOf(1, 2, 3).onEachIndexed { i, e -> add(i.toUShort() + e) } })
        assertEquals(listOf<ULong>(1, 3, 5), mutableListOf<ULong>().apply { ulongArrayOf(1, 2, 3).onEachIndexed { i, e -> add(i.toULong() + e) } })

        val empty = arrayOf<UInt>()
        assertSame(empty, empty.onEachIndexed { i, e -> fail("Should be unreachable: $i, $e") })

        // Identity equality for arguments of types ULongArray and ULongArray is forbidden
//        val nonEmpty = ulongArrayOf(1, 2, 3)
//        assertSame(nonEmpty, nonEmpty.onEachIndexed { _, _ -> })
    }

    @Test
    fun drop() {
        expect(listOf(1.toUByte())) { ubyteArrayOf(1).drop(0) }
        expect(listOf()) { ushortArrayOf().drop(1) }
        expect(listOf()) { uintArrayOf(1).drop(1) }
        expect(listOf(3uL)) { ulongArrayOf(2, 3).drop(1) }
        assertFails {
            uintArrayOf(2).drop(-1)
        }
    }

    @Test
    fun dropLast() {
        expect(listOf()) { ubyteArrayOf().dropLast(1) }
        expect(listOf()) { ushortArrayOf(1).dropLast(1) }
        expect(listOf(1u)) { uintArrayOf(1).dropLast(0) }
        expect(listOf(2uL)) { ulongArrayOf(2, 3).dropLast(1) }
        assertFails {
            ulongArrayOf(1).dropLast(-1)
        }
    }

    @Test
    fun dropWhile() {
        expect(listOf(3.toUByte(), 1.toUByte())) { ubyteArrayOf(2, 3, 1).dropWhile { it < 3 } }
        expect(listOf()) { ushortArrayOf().dropWhile { it < 3 } }
        expect(listOf()) { uintArrayOf(1).dropWhile { it < 3 } }
        expect(listOf(3uL, 1uL)) { ulongArrayOf(2, 3, 1).dropWhile { it < 3 } }
    }

    @Test
    fun dropLastWhile() {
        expect(listOf(2.toUByte(), 3.toUByte())) { ubyteArrayOf(2, 3, 1).dropLastWhile { it < 3 } }
        expect(listOf()) { ushortArrayOf().dropLastWhile { it < 3 } }
        expect(listOf()) { uintArrayOf(1).dropLastWhile { it < 3 } }
        expect(listOf(2uL, 3uL)) { ulongArrayOf(2, 3, 1).dropLastWhile { it < 3 } }
    }

    @Test
    fun take() {
        expect(listOf()) { ubyteArrayOf().take(1) }
        expect(listOf()) { ushortArrayOf(1).take(0) }
        expect(listOf(1u)) { uintArrayOf(1).take(1) }
        expect(listOf(2uL)) { ulongArrayOf(2, 3).take(1) }
        assertFails {
            ubyteArrayOf(1).take(-1)
        }
    }

    @Test
    fun takeLast() {
        expect(listOf()) { ubyteArrayOf().takeLast(1) }
        expect(listOf()) { ushortArrayOf(1).takeLast(0) }
        expect(listOf(1u)) { uintArrayOf(1).takeLast(1) }
        expect(listOf(3uL)) { ulongArrayOf(2, 3).takeLast(1) }
        assertFails {
            ushortArrayOf(1).takeLast(-1)
        }
    }

    @Test
    fun takeWhile() {
        expect(listOf(2.toUByte())) { ubyteArrayOf(2, 3, 1).takeWhile { it < 3 } }
        expect(listOf()) { ushortArrayOf().takeWhile { it < 3 } }
        expect(listOf(1u)) { uintArrayOf(1).takeWhile { it < 3 } }
        expect(listOf(2uL)) { ulongArrayOf(2, 3, 1).takeWhile { it < 3 } }
    }

    @Test
    fun takeLastWhile() {
        expect(listOf()) { ubyteArrayOf().takeLastWhile { it < 3 } }
        expect(listOf(1.toUShort())) { ushortArrayOf(1).takeLastWhile { it < 3 } }
        expect(listOf(1u)) { uintArrayOf(2, 3, 1).takeLastWhile { it < 3 } }
        expect(listOf(1uL)) { ulongArrayOf(2, 3, 1).takeLastWhile { it < 3 } }
    }

    @Test
    fun filter() {
        expect(listOf(3.toByte())) { byteArrayOf(2, 3).filter { it > 2 } }
        expect(listOf()) { ushortArrayOf().filter { it > 2 } }
        expect(listOf()) { uintArrayOf(1).filter { it > 2 } }
        expect(listOf(3uL)) { ulongArrayOf(2, 3).filter { it > 2 } }
    }

    @Test
    fun filterIndexed() {
        expect(listOf<UByte>(2, 5, 8)) { ubyteArrayOf(2, 4, 3, 5, 8).filterIndexed { index, value -> index % 2 == (value % 2).toInt() } }
        expect(listOf()) { ushortArrayOf().filterIndexed { i, v -> i > v.toInt() } }
        expect(listOf<UInt>(2, 5, 8)) { uintArrayOf(2, 4, 3, 5, 8).filterIndexed { index, value -> index % 2 == (value % 2).toInt() } }
        expect(listOf<ULong>(2, 5, 8)) { ulongArrayOf(2, 4, 3, 5, 8).filterIndexed { index, value -> index % 2 == (value % 2).toInt() } }
    }

    @Test
    fun filterNot() {
        expect(listOf(2.toUByte())) { ubyteArrayOf(2, 3).filterNot { it > 2 } }
        expect(listOf()) { ushortArrayOf().filterNot { it > 2 } }
        expect(listOf(1u)) { uintArrayOf(1).filterNot { it > 2 } }
        expect(listOf(2uL)) { ulongArrayOf(2, 3).filterNot { it > 2 } }
    }

    @Test
    fun sort() {
        val ubyteArray = ubyteArrayOf(5, 2, 1, 9, 80, 0, UByte.MAX_VALUE, 250)
        assertArrayContentEquals(ubyteArrayOf(0, 1, 2, 5, 9, 80, 250, UByte.MAX_VALUE), ubyteArray.apply { sort() })

        val ushortArray = ushortArrayOf(5, 2, 1, 9, 80, 0, UShort.MAX_VALUE, 65501)
        assertArrayContentEquals(ushortArrayOf(0, 1, 2, 5, 9, 80, 65501, UShort.MAX_VALUE), ushortArray.apply { sort() })

        val uintArray = uintArrayOf(5, 2, 1, 9, 80, 0, UInt.MAX_VALUE, 4294967200)
        assertArrayContentEquals(uintArrayOf(0, 1, 2, 5, 9, 80, 4294967200, UInt.MAX_VALUE), uintArray.apply { sort() })

        val ulongArray = ulongArrayOf(5, 2, 1, 9, 80, 0, ULong.MAX_VALUE, ULong.MAX_VALUE - 123)
        assertArrayContentEquals(ulongArrayOf(0, 1, 2, 5, 9, 80, ULong.MAX_VALUE - 123, ULong.MAX_VALUE), ulongArray.apply { sort() })
    }

    @Test
    fun sortDescending() {
        val ubyteArray = ubyteArrayOf(5, 2, 1, 9, 80, 0, UByte.MAX_VALUE, 250)
        assertArrayContentEquals(ubyteArrayOf(UByte.MAX_VALUE, 250, 80, 9, 5, 2, 1, 0), ubyteArray.apply { sortDescending() })

        val ushortArray = ushortArrayOf(5, 2, 1, 9, 80, 0, UShort.MAX_VALUE, 65501)
        assertArrayContentEquals(ushortArrayOf(UShort.MAX_VALUE, 65501, 80, 9, 5, 2, 1, 0), ushortArray.apply { sortDescending() })

        val uintArray = uintArrayOf(5, 2, 1, 9, 80, 0, UInt.MAX_VALUE, 4294967200)
        assertArrayContentEquals(uintArrayOf(UInt.MAX_VALUE, 4294967200, 80, 9, 5, 2, 1, 0), uintArray.apply { sortDescending() })

        val ulongArray = ulongArrayOf(5, 2, 1, 9, 80, 0, ULong.MAX_VALUE, ULong.MAX_VALUE - 123)
        assertArrayContentEquals(ulongArrayOf(ULong.MAX_VALUE, ULong.MAX_VALUE - 123, 80, 9, 5, 2, 1, 0), ulongArray.apply { sortDescending() })
    }

    @Test
    fun sorted() {
        assertTrue(uintArrayOf().sorted().none())
        assertEquals(listOf(1u), uintArrayOf(1).sorted())

        val ubyteArray = ubyteArrayOf(5, 2, 1, 9, 80, 0, UByte.MAX_VALUE, 250)
        assertEquals(listOf<UByte>(0, 1, 2, 5, 9, 80, 250, UByte.MAX_VALUE), ubyteArray.sorted())

        val ushortArray = ushortArrayOf(5, 2, 1, 9, 80, 0, UShort.MAX_VALUE, 65501)
        assertEquals(listOf<UShort>(0, 1, 2, 5, 9, 80, 65501, UShort.MAX_VALUE), ushortArray.sorted())

        val uintArray = uintArrayOf(5, 2, 1, 9, 80, 0, UInt.MAX_VALUE, 4294967200)
        assertEquals(listOf<UInt>(0, 1, 2, 5, 9, 80, 4294967200, UInt.MAX_VALUE), uintArray.sorted())

        val ulongArray = ulongArrayOf(5, 2, 1, 9, 80, 0, ULong.MAX_VALUE, ULong.MAX_VALUE - 123)
        assertEquals(listOf<ULong>(0, 1, 2, 5, 9, 80, ULong.MAX_VALUE - 123, ULong.MAX_VALUE), ulongArray.sorted())
    }

    @Test
    fun sortedDescending() {
        assertTrue(uintArrayOf().sortedDescending().none())
        assertEquals(listOf(1u), uintArrayOf(1).sortedDescending())

        val ubyteArray = ubyteArrayOf(5, 2, 1, 9, 80, 0, UByte.MAX_VALUE, 250)
        assertEquals(listOf<UByte>(UByte.MAX_VALUE, 250, 80, 9, 5, 2, 1, 0), ubyteArray.sortedDescending())

        val ushortArray = ushortArrayOf(5, 2, 1, 9, 80, 0, UShort.MAX_VALUE, 65501)
        assertEquals(listOf<UShort>(UShort.MAX_VALUE, 65501, 80, 9, 5, 2, 1, 0), ushortArray.sortedDescending())

        val uintArray = uintArrayOf(5, 2, 1, 9, 80, 0, UInt.MAX_VALUE, 4294967200)
        assertEquals(listOf<UInt>(UInt.MAX_VALUE, 4294967200, 80, 9, 5, 2, 1, 0), uintArray.sortedDescending())

        val ulongArray = ulongArrayOf(5, 2, 1, 9, 80, 0, ULong.MAX_VALUE, ULong.MAX_VALUE - 123)
        assertEquals(listOf<ULong>(ULong.MAX_VALUE, ULong.MAX_VALUE - 123, 80, 9, 5, 2, 1, 0), ulongArray.sortedDescending())
    }

    @Test
    fun sortedBy() {
        assertTrue(uintArrayOf().sortedBy { it.toString() }.none())
        assertEquals(listOf(1u), uintArrayOf(1).sortedBy { it.toString() })

        val ubyteArray = ubyteArrayOf(5, 2, 1, 9, 80, 0, UByte.MAX_VALUE, 250)
        assertEquals(listOf<UByte>(250, UByte.MAX_VALUE, 0, 1, 2, 5, 9, 80), ubyteArray.sortedBy { it.toByte() })

        val ushortArray = ushortArrayOf(5, 2, 1, 9, 80, 0, UShort.MAX_VALUE, 65501)
        assertEquals(listOf<UShort>(65501, UShort.MAX_VALUE, 0, 1, 2, 5, 9, 80), ushortArray.sortedBy { it.toShort() })

        val uintArray = uintArrayOf(5, 2, 1, 9, 80, 0, UInt.MAX_VALUE, 4294967200)
        assertEquals(listOf<UInt>(4294967200, UInt.MAX_VALUE, 0, 1, 2, 5, 9, 80), uintArray.sortedBy { it.toInt() })

        val ulongArray = ulongArrayOf(5, 2, 1, 9, 80, 0, ULong.MAX_VALUE, ULong.MAX_VALUE - 123)
        assertEquals(listOf<ULong>(ULong.MAX_VALUE - 123, ULong.MAX_VALUE, 0, 1, 2, 5, 9, 80), ulongArray.sortedBy { it.toLong() })
    }

    @Test
    fun sortedArray() {
        val ubyteArray = ubyteArrayOf(5, 2, 1, 9, 80, 0, UByte.MAX_VALUE, 250)
        assertArrayContentEquals(ubyteArrayOf(0, 1, 2, 5, 9, 80, 250, UByte.MAX_VALUE), ubyteArray.sortedArray())

        val ushortArray = ushortArrayOf(5, 2, 1, 9, 80, 0, UShort.MAX_VALUE, 65501)
        assertArrayContentEquals(ushortArrayOf(0, 1, 2, 5, 9, 80, 65501, UShort.MAX_VALUE), ushortArray.sortedArray())

        val uintArray = uintArrayOf(5, 2, 1, 9, 80, 0, UInt.MAX_VALUE, 4294967200)
        assertArrayContentEquals(uintArrayOf(0, 1, 2, 5, 9, 80, 4294967200, UInt.MAX_VALUE), uintArray.sortedArray())

        val ulongArray = ulongArrayOf(5, 2, 1, 9, 80, 0, ULong.MAX_VALUE, ULong.MAX_VALUE - 123)
        assertArrayContentEquals(ulongArrayOf(0, 1, 2, 5, 9, 80, ULong.MAX_VALUE - 123, ULong.MAX_VALUE), ulongArray.sortedArray())
    }

    @Test
    fun sortedArrayDescending() {
        val ubyteArray = ubyteArrayOf(5, 2, 1, 9, 80, 0, UByte.MAX_VALUE, 250)
        assertArrayContentEquals(ubyteArrayOf(UByte.MAX_VALUE, 250, 80, 9, 5, 2, 1, 0), ubyteArray.sortedArrayDescending())

        val ushortArray = ushortArrayOf(5, 2, 1, 9, 80, 0, UShort.MAX_VALUE, 65501)
        assertArrayContentEquals(ushortArrayOf(UShort.MAX_VALUE, 65501, 80, 9, 5, 2, 1, 0), ushortArray.sortedArrayDescending())

        val uintArray = uintArrayOf(5, 2, 1, 9, 80, 0, UInt.MAX_VALUE, 4294967200)
        assertArrayContentEquals(uintArrayOf(UInt.MAX_VALUE, 4294967200, 80, 9, 5, 2, 1, 0), uintArray.sortedArrayDescending())

        val ulongArray = ulongArrayOf(5, 2, 1, 9, 80, 0, ULong.MAX_VALUE, ULong.MAX_VALUE - 123)
        assertArrayContentEquals(ulongArrayOf(ULong.MAX_VALUE, ULong.MAX_VALUE - 123, 80, 9, 5, 2, 1, 0), ulongArray.sortedArrayDescending())
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
