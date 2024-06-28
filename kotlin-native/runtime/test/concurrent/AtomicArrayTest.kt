/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.concurrent

import kotlin.concurrent.*
import kotlin.test.*

class AtomicIntArrayTest {
    @Test fun ctor() {
        val arr1 = AtomicIntArray(6)
        assertEquals(arr1[4], 0)
        assertEquals(arr1.length, 6)

        val arr2 = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(arr2[4], 40)
        assertEquals(arr2.length, 10)

        val emptyArr = AtomicIntArray(0)
        assertEquals(emptyArr.length, 0)

        assertFailsWith<IllegalArgumentException> {
            val arrNegativeSize = AtomicIntArray(-5)
        }
    }

    @Test fun getter() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        for (i in 0 until atomicIntArr.length) {
            assertEquals(i * 10, atomicIntArr[i], "getter: FAIL $i")
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr[22]
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr[-1]
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
    }

    @Test fun setter() {
        val atomicIntArr = AtomicIntArray(10)
        for (i in 0 until atomicIntArr.length) {
            atomicIntArr[i] = i * 10
        }
        for (i in 0 until atomicIntArr.length) {
            assertEquals(i * 10, atomicIntArr[i], "setter: FAIL $i")
        }
        assertFailsWith<IndexOutOfBoundsException> {
            atomicIntArr[22] = 100
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            atomicIntArr[-1] = 100
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
    }

    @Test fun addAndGet() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(110, atomicIntArr.addAndGet(1, 100), "addAndGet: FAIL 1")
        assertEquals(110, atomicIntArr[1], "addAndGet: FAIL 2")
        assertEquals(10, atomicIntArr.addAndGet(1, -100), "addAndGet: FAIL 3")
        assertEquals(10, atomicIntArr[1], "addAndGet: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.addAndGet(22, 33535)
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.addAndGet(-1, 33535)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
    }

    @Test fun compareAndExchange() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        val res1 = atomicIntArr.compareAndExchange(2, 20, 222) // success
        assertTrue(res1 == 20 && atomicIntArr[2] == 222, "compareAndExchange: FAIL 1")
        val res2 = atomicIntArr.compareAndExchange(2, 222, 2222) // success
        assertTrue(res2 == 222 && atomicIntArr[2] == 2222, "compareAndExchange: FAIL 2")
        val res3 = atomicIntArr.compareAndExchange(2, 223, 22222) // should fail
        assertTrue(res3 == 2222 && atomicIntArr[2] == 2222, "compareAndExchange: FAIL 3")
        val res4 = atomicIntArr.compareAndExchange(9, 10, 999) // should fail
        assertTrue(res4 == 90 && atomicIntArr[9] == 90, "compareAndExchange: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.compareAndExchange(10, 33535, 39530)
        }.let { ex ->
            assertEquals("The index 10 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.compareAndExchange(-1, 33535, 39530)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
    }

    @Test fun compareAndSet() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        val res1 = atomicIntArr.compareAndSet(2, 20, 222) // success
        assertTrue(res1 && atomicIntArr[2] == 222, "compareAndSet: FAIL 1")
        val res2 = atomicIntArr.compareAndSet(2, 222, 2222) // success
        assertTrue(res2 && atomicIntArr[2] == 2222, "compareAndSet: FAIL 2")
        val res3 = atomicIntArr.compareAndSet(2, 223, 22222) // should fail
        assertTrue(!res3 && atomicIntArr[2] == 2222, "compareAndSet: FAIL 3")
        val res4 = atomicIntArr.compareAndSet(9, 10, 999) // should fail
        assertTrue(!res4 && atomicIntArr[9] == 90, "compareAndSet: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.compareAndSet(10, 33535, 39530)
        }.let { ex ->
            assertEquals("The index 10 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.compareAndSet(-1, 33535, 39530)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
    }

    @Test fun getAndSet() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(20, atomicIntArr.getAndSet(2, 200), "getAndSet: FAIL 1")
        assertEquals(200, atomicIntArr.getAndSet(2, 2000), "getAndSet: FAIL 2")
        assertEquals(2000, atomicIntArr[2], "getAndSet: FAIL 3")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.getAndSet(22, 33535)
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.getAndSet(-1, 33535)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
    }

    @Test fun getAndAdd() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(10, atomicIntArr.getAndAdd(1, 100), "getAndAdd: FAIL 1")
        assertEquals(110, atomicIntArr[1], "getAndAdd: FAIL 2")
        assertEquals(110, atomicIntArr.getAndAdd(1, -100), "getAndAdd: FAIL 3")
        assertEquals(10, atomicIntArr[1], "getAndAdd: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.getAndAdd(22, 33535)
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.getAndAdd(-1, 33535)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
    }

    @Test fun getAndIncrement() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(10, atomicIntArr.getAndIncrement(1), "getAndIncrement: FAIL 1")
        assertEquals(11, atomicIntArr[1], "getAndIncrement: FAIL 2")
        assertEquals(11, atomicIntArr.getAndIncrement(1), "getAndIncrement: FAIL 3")
        assertEquals(12, atomicIntArr[1], "getAndIncrement: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.getAndIncrement(22)
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.getAndIncrement(-1)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
    }

    @Test fun incrementAndGet() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(11, atomicIntArr.incrementAndGet(1), "incrementAndGet: FAIL 1")
        assertEquals(11, atomicIntArr[1], "incrementAndGet: FAIL 2")
        assertEquals(12, atomicIntArr.incrementAndGet(1), "incrementAndGet: FAIL 3")
        assertEquals(12, atomicIntArr[1], "incrementAndGet: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.incrementAndGet(22)
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.incrementAndGet(-1)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
    }

    @Test fun getAndDecrement() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(10, atomicIntArr.getAndDecrement(1), "getAndDecrement: FAIL 1")
        assertEquals(9, atomicIntArr[1], "getAndDecrement: FAIL 2")
        assertEquals(9, atomicIntArr.getAndDecrement(1), "getAndDecrement: FAIL 3")
        assertEquals(8, atomicIntArr[1], "getAndDecrement: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.getAndDecrement(22)
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.getAndDecrement(-1)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
    }

    @Test fun decrementAndGet() {
        val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
        assertEquals(9, atomicIntArr.decrementAndGet(1), "decrementAndGet: FAIL 1")
        assertEquals(9, atomicIntArr[1], "decrementAndGet: FAIL 2")
        assertEquals(8, atomicIntArr.decrementAndGet(1), "decrementAndGet: FAIL 3")
        assertEquals(8, atomicIntArr[1], "decrementAndGet: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.decrementAndGet(22)
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicIntArr.decrementAndGet(-1)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicIntArray with size 10.", ex.message)
        }
    }
}

class AtomicLongArrayTest {
    @Test fun ctor() {
        val arr1 = AtomicLongArray(6)
        assertEquals(arr1[4], 0L)
        assertEquals(arr1.length, 6)
        val arr2 = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(arr2[4], 40L)
        assertEquals(arr2.length, 10)

        val emptyArr = AtomicLongArray(0)
        assertEquals(emptyArr.length, 0)

        assertFailsWith<IllegalArgumentException> {
            val arrNegativeSize = AtomicLongArray(-5)
        }
    }

    @Test fun getter() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        for (i in 0 until atomicLongArr.length) {
            assertEquals(i * 10L, atomicLongArr[i], "getter: FAIL $i")
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr[22]
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr[-1]
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
    }

    @Test fun setter() {
        val atomicLongArr = AtomicLongArray(10)
        for (i in 0 until atomicLongArr.length) {
            atomicLongArr[i] = i * 10L
        }
        for (i in 0 until atomicLongArr.length) {
            assertEquals(i * 10L, atomicLongArr[i], "setter: FAIL $i")
        }
        assertFailsWith<IndexOutOfBoundsException> {
            atomicLongArr[10] = 3998009
        }.let { ex ->
            assertEquals("The index 10 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            atomicLongArr[-1] = 3998009
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
    }

    @Test fun addAndGet() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(110L, atomicLongArr.addAndGet(1, 100L), "addAndGet: FAIL 1")
        assertEquals(110L, atomicLongArr[1], "addAndGet: FAIL 2")
        assertEquals(10L, atomicLongArr.addAndGet(1, -100L), "addAndGet: FAIL 3")
        assertEquals(10L, atomicLongArr[1], "addAndGet: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.addAndGet(22, 33535)
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.addAndGet(-1, 33535)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
    }

    @Test fun compareAndExchange() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        val res1 = atomicLongArr.compareAndExchange(2, 20L, 222L) // success
        assertTrue(res1 == 20L && atomicLongArr[2] == 222L, "compareAndExchange: FAIL 1")
        val res2 = atomicLongArr.compareAndExchange(2, 222L, 2222L) // success
        assertTrue(res2 == 222L && atomicLongArr[2] == 2222L, "compareAndExchange: FAIL 2")
        val res3 = atomicLongArr.compareAndExchange(2, 223L, 22222L) // should fail
        assertTrue(res3 == 2222L && atomicLongArr[2] == 2222L, "compareAndExchange: FAIL 3")
        val res4 = atomicLongArr.compareAndExchange(9, 10L, 999L) // should fail
        assertTrue(res4 == 90L && atomicLongArr[9] == 90L, "compareAndExchange: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            atomicLongArr.compareAndExchange(10, 9353, 39058308)
        }.let { ex ->
            assertEquals("The index 10 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            atomicLongArr.compareAndExchange(-1, 9353, 39058308)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
    }

    @Test fun compareAndSet() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        val res1 = atomicLongArr.compareAndSet(2, 20L, 222L) // success
        assertTrue(res1 && atomicLongArr[2] == 222L, "compareAndSet: FAIL 1")
        val res2 = atomicLongArr.compareAndSet(2, 222L, 2222L) // success
        assertTrue(res2 && atomicLongArr[2] == 2222L, "compareAndSet: FAIL 2")
        val res3 = atomicLongArr.compareAndSet(2, 223L, 22222L) // should fail
        assertTrue(!res3 && atomicLongArr[2] == 2222L, "compareAndSet: FAIL 3")
        val res4 = atomicLongArr.compareAndSet(9, 10L, 999L) // should fail
        assertTrue(!res4 && atomicLongArr[9] == 90L, "compareAndSet: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            atomicLongArr.compareAndSet(10, 9353, 39058308)
        }.let { ex ->
            assertEquals("The index 10 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            atomicLongArr.compareAndSet(-1, 9353, 39058308)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
    }

    @Test fun getAndSet() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(20L, atomicLongArr.getAndSet(2, 200L), "getAndSet: FAIL 1")
        assertEquals(200L, atomicLongArr.getAndSet(2, 2000L), "getAndSet: FAIL 2")
        assertEquals(2000L, atomicLongArr[2], "getAndSet: FAIL 3")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.getAndSet(22, 9353)
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.getAndSet(-1, 9353)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
    }

    @Test fun getAndAdd() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(10L, atomicLongArr.getAndAdd(1, 100L), "getAndAdd: FAIL 1")
        assertEquals(110L, atomicLongArr[1], "getAndAdd: FAIL 2")
        assertEquals(110L, atomicLongArr.getAndAdd(1, -100L), "getAndAdd: FAIL 3")
        assertEquals(10L, atomicLongArr[1], "getAndAdd: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.getAndAdd(100, 9353)
        }.let { ex ->
            assertEquals("The index 100 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.getAndAdd(-1, 9353)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
    }

    @Test fun getAndIncrement() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(10L, atomicLongArr.getAndIncrement(1), "getAndIncrement: FAIL 1")
        assertEquals(11L, atomicLongArr[1], "getAndIncrement: FAIL 2")
        assertEquals(11L, atomicLongArr.getAndIncrement(1), "getAndIncrement: FAIL 3")
        assertEquals(12L, atomicLongArr[1], "getAndIncrement: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.getAndIncrement(22)
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.addAndGet(-1, 33535)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
    }

    @Test fun incrementAndGet() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(11L, atomicLongArr.incrementAndGet(1), "incrementAndGet: FAIL 1")
        assertEquals(11L, atomicLongArr[1], "incrementAndGet: FAIL 2")
        assertEquals(12L, atomicLongArr.incrementAndGet(1), "incrementAndGet: FAIL 3")
        assertEquals(12L, atomicLongArr[1], "incrementAndGet: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.incrementAndGet(22)
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.incrementAndGet(-1)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
    }

    @Test fun getAndDecrement() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(10L, atomicLongArr.getAndDecrement(1), "getAndDecrement: FAIL 1")
        assertEquals(9L, atomicLongArr[1], "getAndDecrement: FAIL 2")
        assertEquals(9L, atomicLongArr.getAndDecrement(1), "getAndDecrement: FAIL 3")
        assertEquals(8L, atomicLongArr[1], "getAndDecrement: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.getAndDecrement(22)
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.getAndDecrement(-1)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
    }

    @Test fun decrementAndGet() {
        val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
        assertEquals(9L, atomicLongArr.decrementAndGet(1), "decrementAndGet: FAIL 1")
        assertEquals(9L, atomicLongArr[1], "decrementAndGet: FAIL 2")
        assertEquals(8L, atomicLongArr.decrementAndGet(1), "decrementAndGet: FAIL 3")
        assertEquals(8L, atomicLongArr[1], "decrementAndGet: FAIL 4")
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.decrementAndGet(22)
        }.let { ex ->
            assertEquals("The index 22 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = atomicLongArr.decrementAndGet(-1)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicLongArray with size 10.", ex.message)
        }
    }
}

class AtomicArrayTest {
    @Test fun ctor() {
        val arr2 = AtomicArray<Data?>(10) { null }
        assertEquals(arr2[4], null)
        assertEquals(arr2.length, 10)

        val emptyArr = AtomicArray<Data?>(0) { Data(1) }
        assertEquals(emptyArr.length, 0)

        assertFailsWith<IllegalArgumentException> {
            val arrNegativeSize = AtomicArray<Data?>(-5) { Data(1) }
        }
    }

    @Test fun getter() {
        val refArr = AtomicArray<Data?>(10) { i -> Data(i) }
        for (i in 0 until refArr.length) {
            assertEquals(Data(i), refArr[i], "getter: FAIL $i")
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = refArr[100]
        }.let { ex ->
            assertEquals("The index 100 is out of the bounds of the AtomicArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            val res = refArr[-1]
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicArray with size 10.", ex.message)
        }
    }

    @Test fun setter() {
        val refArr = AtomicArray<Data?>(10) { null }
        for (i in 0 until refArr.length) {
            refArr[i] = Data(i)
        }
        for (i in 0 until refArr.length) {
            assertEquals(Data(i), refArr[i], "setter: FAIL $i")
        }
        assertFailsWith<IndexOutOfBoundsException> {
            refArr[100] = Data(100)
        }.let { ex ->
            assertEquals("The index 100 is out of the bounds of the AtomicArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            refArr[-1] = Data(-1)
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicArray with size 10.", ex.message)
        }
    }

    @Test fun compareAndExchange() {
        val refArr = AtomicArray<Data?>(10) { null }
        val newValue = Data(1)
        val res1 = refArr.compareAndExchange(3, null, newValue)
        assertTrue(res1 == null && refArr[3] == newValue, "compareAndExchange: FAIL 1")
        val res2 = refArr.compareAndExchange(3, newValue, Data(2))
        assertTrue(res2 == newValue && refArr[3] == Data(2), "compareAndExchange: FAIL 2")
        val res3 = refArr.compareAndExchange(3, newValue, Data(3))
        assertTrue(res3 == Data(2) && refArr[3] == Data(2), "compareAndExchange: FAIL 3")
        assertFailsWith<IndexOutOfBoundsException> {
            refArr.compareAndExchange(10, newValue, Data(4))
        }.let { ex ->
            assertEquals("The index 10 is out of the bounds of the AtomicArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            refArr.compareAndExchange(-1, newValue, Data(4))
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicArray with size 10.", ex.message)
        }
    }

    @Test fun compareAndSet() {
        val refArr = AtomicArray<Data?>(10) { null }
        val newValue = Data(1)
        val res1 = refArr.compareAndSet(3, null, newValue)
        assertTrue(res1 && refArr[3] == newValue, "testAtomicArrayCompareAndSet: FAIL 1")
        val res2 = refArr.compareAndSet(3, newValue, Data(2))
        assertTrue(res2 && refArr[3] == Data(2), "testAtomicArrayCompareAndSet: FAIL 2")
        val res3 = refArr.compareAndSet(3, newValue, Data(3))
        assertTrue(!res3 && refArr[3] == Data(2), "testAtomicArrayCompareAndSet: FAIL 3")
        assertFailsWith<IndexOutOfBoundsException> {
            refArr.compareAndSet(10, newValue, Data(4))
        }.let { ex ->
            assertEquals("The index 10 is out of the bounds of the AtomicArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            refArr.compareAndSet(-1, newValue, Data(4))
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicArray with size 10.", ex.message)
        }
    }

    @Test fun getAndSet() {
        val refArr = AtomicArray<Data?>(10) { null }
        val res4 = refArr.getAndSet(4, Data(1))
        assertEquals(null, res4, "getAndSet: FAIL 1")
        val res5 = refArr.getAndSet(4, Data(2))
        assertEquals(Data(1), res5, "getAndSet: FAIL 2")
        assertFailsWith<IndexOutOfBoundsException> {
            refArr.getAndSet(10, Data(1))
        }.let { ex ->
            assertEquals("The index 10 is out of the bounds of the AtomicArray with size 10.", ex.message)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            refArr.getAndSet(-1, Data(1))
        }.let { ex ->
            assertEquals("The index -1 is out of the bounds of the AtomicArray with size 10.", ex.message)
        }
    }
}