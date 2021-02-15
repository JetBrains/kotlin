/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.hash_map1

import kotlin.native.MemoryModel
import kotlin.native.Platform
import kotlin.native.internal.GC
import kotlin.test.*

fun assertTrue(cond: Boolean) {
    if (!cond)
        println("FAIL")
}

fun assertFalse(cond: Boolean) {
    if (cond)
        println("FAIL")
}

fun assertEquals(value1: Any?, value2: Any?) {
    if (value1 != value2)
        println("FAIL")
}

fun assertNotEquals(value1: Any?, value2: Any?) {
    if (value1 == value2)
        println("FAIL")
}

fun assertEquals(value1: Int, value2: Int) {
    if (value1 != value2)
        println("FAIL")
}

fun testRehashAndCompact() {
    val m = HashMap<String, String>()
    for (repeat in 1..10) {
        val n = when (repeat) {
            1 -> 1000
            2 -> 10000
            3 -> 10
            else -> 100000
        }
        for (i in 1..n) {
            assertFalse(m.containsKey(i.toString()))
            assertEquals(null, m.put(i.toString(), "val$i"))
            assertTrue(m.containsKey(i.toString()))
            assertEquals(i, m.size)
        }
        for (i in 1..n) {
            assertTrue(m.containsKey(i.toString()))
        }
        for (i in 1..n) {
            assertEquals("val$i", m.remove(i.toString()))
            assertFalse(m.containsKey(i.toString()))
            assertEquals(n - i, m.size)
        }
        assertTrue(m.isEmpty())
    }
}

fun testClear() {
    val m = HashMap<String, String>()
    for (repeat in 1..10) {
        val n = when (repeat) {
            1 -> 1000
            2 -> 10000
            3 -> 10
            else -> 100000
        }
        for (i in 1..n) {
            assertFalse(m.containsKey(i.toString()))
            assertEquals(null, m.put(i.toString(), "val$i"))
            assertTrue(m.containsKey(i.toString()))
            assertEquals(i, m.size)
        }
        for (i in 1..n) {
            assertTrue(m.containsKey(i.toString()))
        }
        m.clear()
        assertEquals(0, m.size)
        for (i in 1..n) {
            assertFalse(m.containsKey(i.toString()))
        }
    }
}

@Test fun runTest() {
    // TODO: Do not manually control this.
    if (Platform.memoryModel == MemoryModel.EXPERIMENTAL) {
        GC.threshold = 1000000
        GC.thresholdAllocations = 1000000
    }
    testRehashAndCompact()
    testClear()
    println("OK")
}
