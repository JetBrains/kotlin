/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

package runtime.atomics.atomic_array_smoke

import kotlin.test.*
import kotlin.concurrent.*

@Test
fun testAtomicIntArrayConstructor() {
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

@Test
fun testAtomicIntArrayGetElement() {
    val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
    for (i in 0 until atomicIntArr.length) {
        assertEquals(i * 10, atomicIntArr[i], "testAtomicIntArrayGetElement: FAIL $i")
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

@Test
fun testAtomicIntArraySetElement() {
    val atomicIntArr = AtomicIntArray(10)
    for (i in 0 until atomicIntArr.length) {
        atomicIntArr[i] = i * 10
    }
    for (i in 0 until atomicIntArr.length) {
        assertEquals(i * 10, atomicIntArr[i], "testAtomicIntArraySetElement: FAIL $i")
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

@Test
fun testAtomicIntArrayCompareAndExchange() {
    val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
    val res1 = atomicIntArr.compareAndExchange(2, 20, 222) // success
    assertTrue(res1 == 20 && atomicIntArr[2] == 222, "testAtomicIntArrayCompareAndExchange: FAIL 1")
    val res2 = atomicIntArr.compareAndExchange(2, 222, 2222) // success
    assertTrue(res2 == 222 && atomicIntArr[2] == 2222, "testAtomicIntArrayCompareAndExchange: FAIL 2")
    val res3 = atomicIntArr.compareAndExchange(2, 223, 22222) // should fail
    assertTrue(res3 == 2222 && atomicIntArr[2] == 2222, "testAtomicIntArrayCompareAndExchange: FAIL 3")
    val res4 = atomicIntArr.compareAndExchange(9, 10, 999) // should fail
    assertTrue(res4 == 90 && atomicIntArr[9] == 90, "testAtomicIntArrayCompareAndExchange: FAIL 4")
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

@Test
fun testAtomicIntArrayCompareAndSet() {
    val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
    val res1 = atomicIntArr.compareAndSet(2, 20, 222) // success
    assertTrue(res1 && atomicIntArr[2] == 222, "testAtomicIntArrayCompareAndSet: FAIL 1")
    val res2 = atomicIntArr.compareAndSet(2, 222, 2222) // success
    assertTrue(res2 && atomicIntArr[2] == 2222, "testAtomicIntArrayCompareAndSet: FAIL 2")
    val res3 = atomicIntArr.compareAndSet(2, 223, 22222) // should fail
    assertTrue(!res3 && atomicIntArr[2] == 2222, "testAtomicIntArrayCompareAndSet: FAIL 3")
    val res4 = atomicIntArr.compareAndSet(9, 10, 999) // should fail
    assertTrue(!res4 && atomicIntArr[9] == 90, "testAtomicIntArrayCompareAndSet: FAIL 4")
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

@Test
fun testAtomicIntArrayGetAndSet() {
    val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
    assertEquals(20, atomicIntArr.getAndSet(2, 200), "testAtomicIntArrayGetAndSet: FAIL 1")
    assertEquals(200, atomicIntArr.getAndSet(2, 2000), "testAtomicIntArrayGetAndSet: FAIL 2")
    assertEquals(2000, atomicIntArr[2], "testAtomicIntArrayGetAndSet: FAIL 3")
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

@Test
fun testAtomicIntArrayGetAndAdd() {
    val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
    assertEquals(10, atomicIntArr.getAndAdd(1, 100), "testAtomicIntArrayGetAndAdd: FAIL 1")
    assertEquals(110, atomicIntArr[1], "testAtomicIntArrayGetAndAdd: FAIL 2")
    assertEquals(110, atomicIntArr.getAndAdd(1, -100), "testAtomicIntArrayGetAndAdd: FAIL 3")
    assertEquals(10, atomicIntArr[1], "testAtomicIntArrayGetAndAdd: FAIL 4")
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

@Test
fun testAtomicIntArrayAddAndGet() {
    val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
    assertEquals(110, atomicIntArr.addAndGet(1, 100), "testAtomicIntArrayAddAndGet: FAIL 1")
    assertEquals(110, atomicIntArr[1], "testAtomicIntArrayAddAndGet: FAIL 2")
    assertEquals(10, atomicIntArr.addAndGet(1, -100), "testAtomicIntArrayAddAndGet: FAIL 3")
    assertEquals(10, atomicIntArr[1], "testAtomicIntArrayAddAndGet: FAIL 4")
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

@Test
fun testAtomicIntArrayGetAndIncrement() {
    val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
    assertEquals(10, atomicIntArr.getAndIncrement(1), "testAtomicIntArrayGetAndIncrement: FAIL 1")
    assertEquals(11, atomicIntArr[1], "testAtomicIntArrayGetAndIncrement: FAIL 2")
    assertEquals(11, atomicIntArr.getAndIncrement(1), "testAtomicIntArrayGetAndIncrement: FAIL 3")
    assertEquals(12, atomicIntArr[1], "testAtomicIntArrayGetAndIncrement: FAIL 4")
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

@Test
fun testAtomicIntArrayIncrementAndGet() {
    val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
    assertEquals(11, atomicIntArr.incrementAndGet(1), "testAtomicIntArrayIncrementAndGet: FAIL 1")
    assertEquals(11, atomicIntArr[1], "testAtomicIntArrayIncrementAndGet: FAIL 2")
    assertEquals(12, atomicIntArr.incrementAndGet(1), "testAtomicIntArrayIncrementAndGet: FAIL 3")
    assertEquals(12, atomicIntArr[1], "testAtomicIntArrayIncrementAndGet: FAIL 4")
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

@Test
fun testAtomicIntArrayGetAndDecrement() {
    val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
    assertEquals(10, atomicIntArr.getAndDecrement(1), "testAtomicIntArrayGetAndDecrement: FAIL 1")
    assertEquals(9, atomicIntArr[1], "testAtomicIntArrayGetAndDecrement: FAIL 2")
    assertEquals(9, atomicIntArr.getAndDecrement(1), "testAtomicIntArrayGetAndDecrement: FAIL 3")
    assertEquals(8, atomicIntArr[1], "testAtomicIntArrayGetAndDecrement: FAIL 4")
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

@Test
fun testAtomicIntArrayDecrementAndGet() {
    val atomicIntArr = AtomicIntArray(10) { i: Int -> i * 10 }
    assertEquals(9, atomicIntArr.decrementAndGet(1), "testAtomicIntArrayDecrementAndGet: FAIL 1")
    assertEquals(9, atomicIntArr[1], "testAtomicIntArrayDecrementAndGet: FAIL 2")
    assertEquals(8, atomicIntArr.decrementAndGet(1), "testAtomicIntArrayDecrementAndGet: FAIL 3")
    assertEquals(8, atomicIntArr[1], "testAtomicIntArrayDecrementAndGet: FAIL 4")
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

@Test
fun testAtomicLongArrayConstructor() {
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

@Test
fun testAtomicLongArrayGetElement() {
    val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
    for (i in 0 until atomicLongArr.length) {
        assertEquals(i * 10L, atomicLongArr[i], "testAtomicLongArrayGetElement: FAIL $i")
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

@Test
fun testAtomicLongArraySetElement() {
    val atomicLongArr = AtomicLongArray(10)
    for (i in 0 until atomicLongArr.length) {
        atomicLongArr[i] = i * 10L
    }
    for (i in 0 until atomicLongArr.length) {
        assertEquals(i * 10L, atomicLongArr[i], "testAtomicLongArraySetElement: FAIL $i")
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

@Test
fun testAtomicLongArrayCompareAndExchange() {
    val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
    val res1 = atomicLongArr.compareAndExchange(2, 20L, 222L) // success
    assertTrue(res1 == 20L && atomicLongArr[2] == 222L, "testAtomicLongArrayCompareAndExchange: FAIL 1")
    val res2 = atomicLongArr.compareAndExchange(2, 222L, 2222L) // success
    assertTrue(res2 == 222L && atomicLongArr[2] == 2222L, "testAtomicLongArrayCompareAndExchange: FAIL 2")
    val res3 = atomicLongArr.compareAndExchange(2, 223L, 22222L) // should fail
    assertTrue(res3 == 2222L && atomicLongArr[2] == 2222L, "testAtomicLongArrayCompareAndExchange: FAIL 3")
    val res4 = atomicLongArr.compareAndExchange(9, 10L, 999L) // should fail
    assertTrue(res4 == 90L && atomicLongArr[9] == 90L, "testAtomicLongArrayCompareAndExchange: FAIL 4")
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

@Test
fun testAtomicLongArrayCompareAndSet() {
    val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
    val res1 = atomicLongArr.compareAndSet(2, 20L, 222L) // success
    assertTrue(res1 && atomicLongArr[2] == 222L, "testAtomicLongArrayCompareAndSet: FAIL 1")
    val res2 = atomicLongArr.compareAndSet(2, 222L, 2222L) // success
    assertTrue(res2 && atomicLongArr[2] == 2222L, "testAtomicLongArrayCompareAndSet: FAIL 2")
    val res3 = atomicLongArr.compareAndSet(2, 223L, 22222L) // should fail
    assertTrue(!res3 && atomicLongArr[2] == 2222L, "testAtomicLongArrayCompareAndSet: FAIL 3")
    val res4 = atomicLongArr.compareAndSet(9, 10L, 999L) // should fail
    assertTrue(!res4 && atomicLongArr[9] == 90L, "testAtomicLongArrayCompareAndSet: FAIL 4")
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

@Test
fun testAtomicLongArrayGetAndSet() {
    val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
    assertEquals(20L, atomicLongArr.getAndSet(2, 200L), "testAtomicLongArrayGetAndSet: FAIL 1")
    assertEquals(200L, atomicLongArr.getAndSet(2, 2000L), "testAtomicLongArrayGetAndSet: FAIL 2")
    assertEquals(2000L, atomicLongArr[2], "testAtomicLongArrayGetAndSet: FAIL 3")
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

@Test
fun testAtomicLongArrayGetAndAdd() {
    val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
    assertEquals(10L, atomicLongArr.getAndAdd(1, 100L), "testAtomicLongArrayGetAndAdd: FAIL 1")
    assertEquals(110L, atomicLongArr[1], "testAtomicLongArrayGetAndAdd: FAIL 2")
    assertEquals(110L, atomicLongArr.getAndAdd(1, -100L), "testAtomicLongArrayGetAndAdd: FAIL 3")
    assertEquals(10L, atomicLongArr[1], "testAtomicLongArrayGetAndAdd: FAIL 4")
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

@Test
fun testAtomicLongArrayAddAndGet() {
    val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
    assertEquals(110L, atomicLongArr.addAndGet(1, 100L), "testAtomicLongArrayAddAndGet: FAIL 1")
    assertEquals(110L, atomicLongArr[1], "testAtomicLongArrayAddAndGet: FAIL 2")
    assertEquals(10L, atomicLongArr.addAndGet(1, -100L), "testAtomicLongArrayAddAndGet: FAIL 3")
    assertEquals(10L, atomicLongArr[1], "testAtomicLongArrayAddAndGet: FAIL 4")
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

@Test
fun testAtomicLongArrayGetAndIncrement() {
    val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
    assertEquals(10L, atomicLongArr.getAndIncrement(1), "testAtomicLongArrayGetAndIncrement: FAIL 1")
    assertEquals(11L, atomicLongArr[1], "testAtomicLongArrayGetAndIncrement: FAIL 2")
    assertEquals(11L, atomicLongArr.getAndIncrement(1), "testAtomicLongArrayGetAndIncrement: FAIL 3")
    assertEquals(12L, atomicLongArr[1], "testAtomicLongArrayGetAndIncrement: FAIL 4")
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

@Test
fun testAtomicLongArrayIncrementAndGet() {
    val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
    assertEquals(11L, atomicLongArr.incrementAndGet(1), "testAtomicLongArrayIncrementAndGet: FAIL 1")
    assertEquals(11L, atomicLongArr[1], "testAtomicLongArrayIncrementAndGet: FAIL 2")
    assertEquals(12L, atomicLongArr.incrementAndGet(1), "testAtomicLongArrayIncrementAndGet: FAIL 3")
    assertEquals(12L, atomicLongArr[1], "testAtomicLongArrayIncrementAndGet: FAIL 4")
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

@Test
fun testAtomicLongArrayGetAndDecrement() {
    val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
    assertEquals(10L, atomicLongArr.getAndDecrement(1), "testAtomicLongArrayGetAndDecrement: FAIL 1")
    assertEquals(9L, atomicLongArr[1], "testAtomicLongArrayGetAndDecrement: FAIL 2")
    assertEquals(9L, atomicLongArr.getAndDecrement(1), "testAtomicLongArrayGetAndDecrement: FAIL 3")
    assertEquals(8L, atomicLongArr[1], "testAtomicLongArrayGetAndDecrement: FAIL 4")
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

@Test
fun testAtomicLongArrayDecrementAndGet() {
    val atomicLongArr = AtomicLongArray(10) { i: Int -> i * 10L }
    assertEquals(9L, atomicLongArr.decrementAndGet(1), "testAtomicLongArrayDecrementAndGet: FAIL 1")
    assertEquals(9L, atomicLongArr[1], "testAtomicLongArrayDecrementAndGet: FAIL 2")
    assertEquals(8L, atomicLongArr.decrementAndGet(1), "testAtomicLongArrayDecrementAndGet: FAIL 3")
    assertEquals(8L, atomicLongArr[1], "testAtomicLongArrayDecrementAndGet: FAIL 4")
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

@Test
fun testAtomicArrayConstructor() {
    val arr2 = AtomicArray<String?>(10) { null }
    assertEquals(arr2[4], null)
    assertEquals(arr2.length, 10)

    val emptyArr = AtomicArray<String?>(0) { "aa" }
    assertEquals(emptyArr.length, 0)

    assertFailsWith<IllegalArgumentException> {
        val arrNegativeSize = AtomicArray<String?>(-5) { "aa" }
    }
}

@Test
fun testAtomicArrayGetElement() {
    val refArr = AtomicArray<String?>(10) { i -> "a$i" }
    for (i in 0 until refArr.length) {
        assertEquals("a$i", refArr[i], "testAtomicArrayGetElement: FAIL $i")
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

@Test
fun testAtomicArraySetElement() {
    val refArr = AtomicArray<String?>(10) { null }
    for (i in 0 until refArr.length) {
        refArr[i] = "a$i"
    }
    for (i in 0 until refArr.length) {
        assertEquals("a$i", refArr[i], "testAtomicArraySetElement: FAIL $i")
    }
    assertFailsWith<IndexOutOfBoundsException> {
        refArr[100] = "aaa"
    }.let { ex ->
        assertEquals("The index 100 is out of the bounds of the AtomicArray with size 10.", ex.message)
    }
    assertFailsWith<IndexOutOfBoundsException> {
        refArr[-1] = "aaa"
    }.let { ex ->
        assertEquals("The index -1 is out of the bounds of the AtomicArray with size 10.", ex.message)
    }
}

@Test
fun testAtomicArrayCompareAndExchange() {
    val refArr = AtomicArray<String?>(10) { null }
    val newValue = "aaa"
    val res1 = refArr.compareAndExchange(3, null, newValue)
    assertTrue(res1 == null && refArr[3] == newValue, "testAtomicArrayCompareAndExchange: FAIL 1")
    val res2 = refArr.compareAndExchange(3, newValue, "bbb")
    assertTrue(res2 == newValue && refArr[3] == "bbb", "testAtomicArrayCompareAndExchange: FAIL 2")
    val res3 = refArr.compareAndExchange(3, newValue, "ccc")
    assertTrue(res3 == "bbb" && refArr[3] == "bbb", "testAtomicArrayCompareAndExchange: FAIL 3")
    assertFailsWith<IndexOutOfBoundsException> {
        refArr.compareAndExchange(10, "aaa", "jdndkj")
    }.let { ex ->
        assertEquals("The index 10 is out of the bounds of the AtomicArray with size 10.", ex.message)
    }
    assertFailsWith<IndexOutOfBoundsException> {
        refArr.compareAndExchange(-1, "aaa", "jdndkj")
    }.let { ex ->
        assertEquals("The index -1 is out of the bounds of the AtomicArray with size 10.", ex.message)
    }
}

@Test
fun testAtomicArrayCompareAndSet() {
    val refArr = AtomicArray<String?>(10) { null }
    val newValue = "aaa"
    val res1 = refArr.compareAndSet(3, null, newValue)
    assertTrue(res1 && refArr[3] == newValue, "testAtomicArrayCompareAndSet: FAIL 1")
    val res2 = refArr.compareAndSet(3, newValue, "bbb")
    assertTrue(res2 && refArr[3] == "bbb", "testAtomicArrayCompareAndSet: FAIL 2")
    val res3 = refArr.compareAndSet(3, newValue, "ccc")
    assertTrue(!res3 && refArr[3] == "bbb", "testAtomicArrayCompareAndSet: FAIL 3")
    assertFailsWith<IndexOutOfBoundsException> {
        refArr.compareAndSet(10, "aaa", "jdndkj")
    }.let { ex ->
        assertEquals("The index 10 is out of the bounds of the AtomicArray with size 10.", ex.message)
    }
    assertFailsWith<IndexOutOfBoundsException> {
        refArr.compareAndSet(-1, "aaa", "jdndkj")
    }.let { ex ->
        assertEquals("The index -1 is out of the bounds of the AtomicArray with size 10.", ex.message)
    }
}

@Test
fun testAtomicArrayGetAndSet() {
    val refArr = AtomicArray<String?>(10) { null }
    val res4 = refArr.getAndSet(4, "aaa")
    assertEquals(null, res4, "testAtomicArrayGetAndSet: FAIL 1")
    val res5 = refArr.getAndSet(4, "bbb")
    assertEquals("aaa", res5, "testAtomicArrayGetAndSet: FAIL 2")
    assertFailsWith<IndexOutOfBoundsException> {
        refArr.getAndSet(10, "aaa")
    }.let { ex ->
        assertEquals("The index 10 is out of the bounds of the AtomicArray with size 10.", ex.message)
    }
    assertFailsWith<IndexOutOfBoundsException> {
        refArr.getAndSet(-1, "aaa")
    }.let { ex ->
        assertEquals("The index -1 is out of the bounds of the AtomicArray with size 10.", ex.message)
    }
}