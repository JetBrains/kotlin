/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalAtomicApi::class)
package test.concurrent.atomics

import test.properties.delegation.references.Data
import kotlin.concurrent.atomics.*
import kotlin.test.*

class AtomicIntArrayTest {
    @Test
    fun ctor() {
        val arr1 = AtomicIntArray(6)
        assertEquals(0, arr1.loadAt(4))
        assertEquals(6, arr1.size)

        val arr2 = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(40, arr2.loadAt(4))
        assertEquals(10, arr2.size)

        val emptyArr = AtomicIntArray(0)
        assertEquals(0, emptyArr.size)

        assertFails {
            val arrNegativeSize = AtomicIntArray(-5)
        }
    }

    @Test
    fun getter() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        for (i in 0 until atomicIntArr.size) {
            assertEquals(i * 10, atomicIntArr.loadAt(i), "getter: FAIL $i")
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.loadAt(22)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.loadAt(-1)
        }
    }

    @Test
    fun setter() {
        val atomicIntArr = AtomicIntArray(10)
        for (i in 0 until atomicIntArr.size) {
            atomicIntArr.storeAt(i, i * 10)
        }
        for (i in 0 until atomicIntArr.size) {
            assertEquals(i * 10, atomicIntArr.loadAt(i), "setter: FAIL $i")
        }
        assertFailsWith<IndexOutOfBoundsException> {
            atomicIntArr.storeAt(22, 100)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            atomicIntArr.storeAt(-1, 100)
        }
    }

    @Test
    fun addAndFetchAt() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(110, atomicIntArr.addAndFetchAt(1, 100), "addAndFetchAt: FAIL 1")
        assertEquals(110, atomicIntArr.loadAt(1), "addAndFetchAt: FAIL 2")
        assertEquals(10, atomicIntArr.addAndFetchAt(1, -100), "addAndFetchAt: FAIL 3")
        assertEquals(10, atomicIntArr.loadAt(1), "addAndFetchAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.addAndFetchAt(22, 33535)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.addAndFetchAt(-1, 33535)
        }
    }

    @Test
    fun compareAndExchange() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        val res1 = atomicIntArr.compareAndExchangeAt(2, 20, 222) // success
        assertTrue(res1 == 20 && atomicIntArr.loadAt(2) == 222, "compareAndExchange: FAIL 1")
        val res2 = atomicIntArr.compareAndExchangeAt(2, 222, 2222) // success
        assertTrue(res2 == 222 && atomicIntArr.loadAt(2) == 2222, "compareAndExchangeAt: FAIL 2")
        val res3 = atomicIntArr.compareAndExchangeAt(2, 223, 22222) // should fail
        assertTrue(res3 == 2222 && atomicIntArr.loadAt(2) == 2222, "compareAndExchangeAt: FAIL 3")
        val res4 = atomicIntArr.compareAndExchangeAt(9, 10, 999) // should fail
        assertTrue(res4 == 90 && atomicIntArr.loadAt(9) == 90, "compareAndExchangeAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.compareAndExchangeAt(10, 33535, 39530)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.compareAndExchangeAt(-1, 33535, 39530)
        }
    }

    @Test
    fun compareAndSetAt() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        val res1 = atomicIntArr.compareAndSetAt(2, 20, 222) // success
        assertTrue(res1 && atomicIntArr.loadAt(2) == 222, "compareAndSetAt: FAIL 1")
        val res2 = atomicIntArr.compareAndSetAt(2, 222, 2222) // success
        assertTrue(res2 && atomicIntArr.loadAt(2) == 2222, "compareAndSetAt: FAIL 2")
        val res3 = atomicIntArr.compareAndSetAt(2, 223, 22222) // should fail
        assertTrue(!res3 && atomicIntArr.loadAt(2) == 2222, "compareAndSetAt: FAIL 3")
        val res4 = atomicIntArr.compareAndSetAt(9, 10, 999) // should fail
        assertTrue(!res4 && atomicIntArr.loadAt(9) == 90, "compareAndSetAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.compareAndSetAt(10, 33535, 39530)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.compareAndSetAt(-1, 33535, 39530)
        }
    }

    @Test
    fun exchangeAt() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(20, atomicIntArr.exchangeAt(2, 200), "exchangeAt: FAIL 1")
        assertEquals(200, atomicIntArr.exchangeAt(2, 2000), "exchangeAt: FAIL 2")
        assertEquals(2000, atomicIntArr.loadAt(2), "exchangeAt: FAIL 3")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.exchangeAt(22, 33535)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.exchangeAt(-1, 33535)
        }
    }

    @Test
    fun fetchAndAddAt() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(10, atomicIntArr.fetchAndAddAt(1, 100), "fetchAndAddAt: FAIL 1")
        assertEquals(110, atomicIntArr.loadAt(1), "fetchAndAddAt: FAIL 2")
        assertEquals(110, atomicIntArr.fetchAndAddAt(1, -100), "fetchAndAddAt: FAIL 3")
        assertEquals(10, atomicIntArr.loadAt(1), "fetchAndAddAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.fetchAndAddAt(22, 33535)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.fetchAndAddAt(-1, 33535)
        }
    }

    @Test
    fun fetchAndIncrementAt() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(10, atomicIntArr.fetchAndIncrementAt(1), "fetchAndIncrementAt: FAIL 1")
        assertEquals(11, atomicIntArr.loadAt(1), "fetchAndIncrementAt: FAIL 2")
        assertEquals(11, atomicIntArr.fetchAndIncrementAt(1), "fetchAndIncrementAt: FAIL 3")
        assertEquals(12, atomicIntArr.loadAt(1), "fetchAndIncrementAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.fetchAndIncrementAt(22)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.fetchAndIncrementAt(-1)
        }
    }

    @Test
    fun incrementAndFetchAt() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(11, atomicIntArr.incrementAndFetchAt(1), "incrementAndFetchAt: FAIL 1")
        assertEquals(11, atomicIntArr.loadAt(1), "incrementAndFetchAt: FAIL 2")
        assertEquals(12, atomicIntArr.incrementAndFetchAt(1), "incrementAndFetchAt: FAIL 3")
        assertEquals(12, atomicIntArr.loadAt(1), "incrementAndFetchAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.incrementAndFetchAt(22)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.incrementAndFetchAt(-1)
        }
    }

    @Test
    fun fetchAndDecrementAt() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(10, atomicIntArr.fetchAndDecrementAt(1), "fetchAndDecrementAt: FAIL 1")
        assertEquals(9, atomicIntArr.loadAt(1), "fetchAndDecrementAt: FAIL 2")
        assertEquals(9, atomicIntArr.fetchAndDecrementAt(1), "fetchAndDecrementAt: FAIL 3")
        assertEquals(8, atomicIntArr.loadAt(1), "fetchAndDecrementAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.fetchAndDecrementAt(22)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.fetchAndDecrementAt(-1)
        }
    }

    @Test
    fun decrementAndFetchAt() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(9, atomicIntArr.decrementAndFetchAt(1), "decrementAndFetchAt: FAIL 1")
        assertEquals(9, atomicIntArr.loadAt(1), "decrementAndFetchAt: FAIL 2")
        assertEquals(8, atomicIntArr.decrementAndFetchAt(1), "decrementAndFetchAt: FAIL 3")
        assertEquals(8, atomicIntArr.loadAt(1), "decrementAndFetchAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.decrementAndFetchAt(22)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.decrementAndFetchAt(-1)
        }
    }

    @Test
    fun toStringTest() {
        val array = AtomicIntArray(3) { it }
        assertEquals("[0, 1, 2]", array.toString())
    }
}

class AtomicLongArrayTest {
    @Test
    fun ctor() {
        val arr1 = AtomicLongArray(6)
        assertEquals(arr1.loadAt(4), 0L)
        assertEquals(arr1.size, 6)
        val arr2 = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(arr2.loadAt(4), 40L)
        assertEquals(arr2.size, 10)

        val emptyArr = AtomicLongArray(0)
        assertEquals(emptyArr.size, 0)

        assertFails {
            val arrNegativeSize = AtomicLongArray(-5)
        }
    }

    @Test
    fun getter() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        for (i in 0 until atomicLongArr.size) {
            assertEquals(i * 10L, atomicLongArr.loadAt(i), "getter: FAIL $i")
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.loadAt(22)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.loadAt(-1)
        }
    }

    @Test
    fun setter() {
        val atomicLongArr = AtomicLongArray(10)
        for (i in 0 until atomicLongArr.size) {
            atomicLongArr.storeAt(i, i * 10L)
        }
        for (i in 0 until atomicLongArr.size) {
            assertEquals(i * 10L, atomicLongArr.loadAt(i), "setter: FAIL $i")
        }
        assertFailsWith<IndexOutOfBoundsException> {
            atomicLongArr.storeAt(10, 3998009)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            atomicLongArr.storeAt(-1, 3998009)
        }
    }

    @Test
    fun addAndFetchAt() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(110L, atomicLongArr.addAndFetchAt(1, 100L), "addAndFetchAt: FAIL 1")
        assertEquals(110L, atomicLongArr.loadAt(1), "addAndFetchAt: FAIL 2")
        assertEquals(10L, atomicLongArr.addAndFetchAt(1, -100L), "addAndFetchAt: FAIL 3")
        assertEquals(10L, atomicLongArr.loadAt(1), "addAndFetchAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.addAndFetchAt(22, 33535)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.addAndFetchAt(-1, 33535)
        }
    }

    @Test
    fun compareAndExchangeAt() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        val res1 = atomicLongArr.compareAndExchangeAt(2, 20L, 222L) // success
        assertTrue(res1 == 20L && atomicLongArr.loadAt(2) == 222L, "compareAndExchangeAt: FAIL 1")
        val res2 = atomicLongArr.compareAndExchangeAt(2, 222L, 2222L) // success
        assertTrue(res2 == 222L && atomicLongArr.loadAt(2) == 2222L, "compareAndExchangeAt: FAIL 2")
        val res3 = atomicLongArr.compareAndExchangeAt(2, 223L, 22222L) // should fail
        assertTrue(res3 == 2222L && atomicLongArr.loadAt(2) == 2222L, "compareAndExchangeAt: FAIL 3")
        val res4 = atomicLongArr.compareAndExchangeAt(9, 10L, 999L) // should fail
        assertTrue(res4 == 90L && atomicLongArr.loadAt(9) == 90L, "compareAndExchangeAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            atomicLongArr.compareAndExchangeAt(10, 9353, 39058308)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            atomicLongArr.compareAndExchangeAt(-1, 9353, 39058308)
        }
    }

    @Test
    fun compareAndSetAt() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        val res1 = atomicLongArr.compareAndSetAt(2, 20L, 222L) // success
        assertTrue(res1 && atomicLongArr.loadAt(2) == 222L, "compareAndSetAt: FAIL 1")
        val res2 = atomicLongArr.compareAndSetAt(2, 222L, 2222L) // success
        assertTrue(res2 && atomicLongArr.loadAt(2) == 2222L, "compareAndSetAt: FAIL 2")
        val res3 = atomicLongArr.compareAndSetAt(2, 223L, 22222L) // should fail
        assertTrue(!res3 && atomicLongArr.loadAt(2) == 2222L, "compareAndSetAt: FAIL 3")
        val res4 = atomicLongArr.compareAndSetAt(9, 10L, 999L) // should fail
        assertTrue(!res4 && atomicLongArr.loadAt(9) == 90L, "compareAndSetAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            atomicLongArr.compareAndSetAt(10, 9353, 39058308)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            atomicLongArr.compareAndSetAt(-1, 9353, 39058308)
        }
    }

    @Test
    fun exchangeAt() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(20L, atomicLongArr.exchangeAt(2, 200L), "exchangeAt: FAIL 1")
        assertEquals(200L, atomicLongArr.exchangeAt(2, 2000L), "exchangeAt: FAIL 2")
        assertEquals(2000L, atomicLongArr.loadAt(2), "exchangeAt: FAIL 3")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.exchangeAt(22, 9353)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.exchangeAt(-1, 9353)
        }
    }

    @Test
    fun fetchAndAddAt() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(10L, atomicLongArr.fetchAndAddAt(1, 100L), "fetchAndAddAt: FAIL 1")
        assertEquals(110L, atomicLongArr.loadAt(1), "fetchAndAddAt: FAIL 2")
        assertEquals(110L, atomicLongArr.fetchAndAddAt(1, -100L), "fetchAndAddAt: FAIL 3")
        assertEquals(10L, atomicLongArr.loadAt(1), "fetchAndAddAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.fetchAndAddAt(100, 9353)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.fetchAndAddAt(-1, 9353)
        }
    }

    @Test
    fun fetchAndIncrementAt() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(10L, atomicLongArr.fetchAndIncrementAt(1), "fetchAndIncrementAt: FAIL 1")
        assertEquals(11L, atomicLongArr.loadAt(1), "fetchAndIncrementAt: FAIL 2")
        assertEquals(11L, atomicLongArr.fetchAndIncrementAt(1), "fetchAndIncrementAt: FAIL 3")
        assertEquals(12L, atomicLongArr.loadAt(1), "fetchAndIncrementAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.fetchAndIncrementAt(22)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.addAndFetchAt(-1, 33535)
        }
    }

    @Test
    fun incrementAndFetchAt() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(11L, atomicLongArr.incrementAndFetchAt(1), "incrementAndFetchAt: FAIL 1")
        assertEquals(11L, atomicLongArr.loadAt(1), "incrementAndFetchAt: FAIL 2")
        assertEquals(12L, atomicLongArr.incrementAndFetchAt(1), "incrementAndFetchAt: FAIL 3")
        assertEquals(12L, atomicLongArr.loadAt(1), "incrementAndFetchAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.incrementAndFetchAt(22)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.incrementAndFetchAt(-1)
        }
    }

    @Test
    fun fetchAndDecrementAt() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(10L, atomicLongArr.fetchAndDecrementAt(1), "fetchAndDecrementAt: FAIL 1")
        assertEquals(9L, atomicLongArr.loadAt(1), "fetchAndDecrementAt: FAIL 2")
        assertEquals(9L, atomicLongArr.fetchAndDecrementAt(1), "fetchAndDecrementAt: FAIL 3")
        assertEquals(8L, atomicLongArr.loadAt(1), "fetchAndDecrementAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.fetchAndDecrementAt(22)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.fetchAndDecrementAt(-1)
        }
    }

    @Test
    fun decrementAndFetchAt() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(9L, atomicLongArr.decrementAndFetchAt(1), "decrementAndFetchAt: FAIL 1")
        assertEquals(9L, atomicLongArr.loadAt(1), "decrementAndFetchAt: FAIL 2")
        assertEquals(8L, atomicLongArr.decrementAndFetchAt(1), "decrementAndFetchAt: FAIL 3")
        assertEquals(8L, atomicLongArr.loadAt(1), "decrementAndFetchAt: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.decrementAndFetchAt(22)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.decrementAndFetchAt(-1)
        }
    }

    @Test
    fun toStringTest() {
        val array = AtomicLongArray(3) { it.toLong() }
        assertEquals("[0, 1, 2]", array.toString())
    }
}

class AtomicArrayTest {
    private data class Data(val value: Int)

    @Test
    fun ctor() {
        val arr2 = AtomicArray<Data?>(10) { null }
        assertEquals(arr2.loadAt(4), null)
        assertEquals(arr2.size, 10)

        val emptyArr = AtomicArray<Data?>(0) { Data(1) }
        assertEquals(emptyArr.size, 0)

        assertFails {
            val arrNegativeSize = AtomicArray<Data?>(-5) { Data(1) }
        }
    }

    @Test
    fun getter() {
        val refArr = AtomicArray<Data?>(10) { i -> Data(i) }
        for (i in 0 until refArr.size) {
            assertEquals(Data(i), refArr.loadAt(i), "getter: FAIL $i")
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = refArr.loadAt(100)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = refArr.loadAt(-1)
        }
    }

    @Test
    fun setter() {
        val refArr = AtomicArray<Data?>(10) { null }
        for (i in 0 until refArr.size) {
            refArr.storeAt(i, Data(i))
        }
        for (i in 0 until refArr.size) {
            assertEquals(Data(i), refArr.loadAt(i), "setter: FAIL $i")
        }
        assertFailsWith<IndexOutOfBoundsException> {
            refArr.storeAt(100, Data(100))
        }
        assertFailsWith<IndexOutOfBoundsException> {
            refArr.storeAt(-1, Data(-1))
        }
    }

    @Test
    fun compareAndExchangeAt() {
        val refArr = AtomicArray<Data?>(10) { null }
        val newValue = Data(1)
        val res1 = refArr.compareAndExchangeAt(3, null, newValue)
        assertTrue(res1 == null && refArr.loadAt(3) == newValue, "compareAndExchangeAt: FAIL 1")
        val res2 = refArr.compareAndExchangeAt(3, newValue, Data(2))
        assertTrue(res2 == newValue && refArr.loadAt(3) == Data(2), "compareAndExchangeAt: FAIL 2")
        val res3 = refArr.compareAndExchangeAt(3, newValue, Data(3))
        assertTrue(res3 == Data(2) && refArr.loadAt(3) == Data(2), "compareAndExchangeAt: FAIL 3")
        assertFailsWith<IndexOutOfBoundsException> {
            refArr.compareAndExchangeAt(10, newValue, Data(4))
        }
        assertFailsWith<IndexOutOfBoundsException> {
            refArr.compareAndExchangeAt(-1, newValue, Data(4))
        }
    }

    @Test
    fun compareAndSetAt() {
        val refArr = AtomicArray<Data?>(10) { null }
        val newValue = Data(1)
        val res1 = refArr.compareAndSetAt(3, null, newValue)
        assertTrue(res1 && refArr.loadAt(3) == newValue, "testAtomicArraycompareAndSetAt: FAIL 1")
        val res2 = refArr.compareAndSetAt(3, newValue, Data(2))
        assertTrue(res2 && refArr.loadAt(3) == Data(2), "testAtomicArraycompareAndSetAt: FAIL 2")
        val res3 = refArr.compareAndSetAt(3, newValue, Data(3))
        assertTrue(!res3 && refArr.loadAt(3) == Data(2), "testAtomicArraycompareAndSetAt: FAIL 3")
        assertFailsWith<IndexOutOfBoundsException> {
            refArr.compareAndSetAt(10, newValue, Data(4))
        }
        assertFailsWith<IndexOutOfBoundsException> {
            refArr.compareAndSetAt(-1, newValue, Data(4))
        }
    }

    @Test
    fun exchangeAt() {
        val refArr = AtomicArray<Data?>(10) { null }
        val res4 = refArr.exchangeAt(4, Data(1))
        assertEquals(null, res4, "exchangeAt: FAIL 1")
        val res5 = refArr.exchangeAt(4, Data(2))
        assertEquals(Data(1), res5, "exchangeAt: FAIL 2")
        assertFailsWith<IndexOutOfBoundsException> {
            refArr.exchangeAt(10, Data(1))
        }
        assertFailsWith<IndexOutOfBoundsException> {
            refArr.exchangeAt(-1, Data(1))
        }
    }

    @Test
    fun compareAndSetComparingByReference() {
        val datum1 = Data(1)
        val datum2 = Data(1)

        val atomicArray = AtomicArray(arrayOf(datum1))

        assertEquals(datum1, datum2)
        assertNotSame(datum1, datum2)

        // datum1 is equal to itself, CAS should succeed
        assertTrue(atomicArray.compareAndSetAt(0, datum1, datum2))

        // datum2 is not equal to datum1, they are two distinct, so CAS should fail
        assertFalse(atomicArray.compareAndSetAt(0, datum1, Data(2)))
    }

    @Test
    fun toStringTest() {
        val array = AtomicArray(arrayOf(Data(0), Data(1), Data(2)))
        assertEquals("[Data(value=0), Data(value=1), Data(value=2)]", array.toString())
    }
}

class AtomicArrayFactoriesTest {
    @Test
    fun atomicIntArrayTest() {
        val emptyArray: AtomicIntArray = atomicIntArrayOf()
        assertEquals(0, emptyArray.size)

        val nonEmptyArray: AtomicIntArray = atomicIntArrayOf(1, 2, 3)
        assertEquals(3, nonEmptyArray.size)
        assertEquals("[1, 2, 3]", nonEmptyArray.toString())
    }

    @Test
    fun atomicLongArrayTest() {
        val emptyArray: AtomicLongArray = atomicLongArrayOf()
        assertEquals(0, emptyArray.size)

        val nonEmptyArray: AtomicLongArray = atomicLongArrayOf(1L, 2L, 3L)
        assertEquals(3, nonEmptyArray.size)
        assertEquals("[1, 2, 3]", nonEmptyArray.toString())
    }

    @Test
    fun atomicArrayTest() {
        val emptyArray: AtomicArray<Any> = atomicArrayOf()
        assertEquals(0, emptyArray.size)

        val nonEmptyArray: AtomicArray<String?> = atomicArrayOf("1", null, "3")
        assertEquals(3, nonEmptyArray.size)
        assertEquals("[1, null, 3]", nonEmptyArray.toString())
    }
}
