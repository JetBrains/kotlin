/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(kotlin.native.ObsoleteNativeApi::class)

package runtime.collections.BitSet

import kotlin.test.*

fun assertContainsOnly(bitSet: BitSet, trueBits: Set<Int>, size: Int) {
    for (i in 0 until size) {
        val expectedBit = i in trueBits
        if (bitSet[i] != expectedBit) {
            throw AssertionError()
        }
    }
}

fun assertNotContainsOnly(bitSet: BitSet, falseBits: Set<Int>, size: Int) {
    for (i in 0 until size) {
        val expectedBit = i !in falseBits
        if (bitSet[i] != expectedBit) {
            throw AssertionError()
        }
    }
}

fun assertTrue(str: String, cond: Boolean) { if (!cond) throw AssertionError(str) }
fun assertFalse(str: String, cond: Boolean) { if (cond) throw AssertionError(str) }
fun <T> assertEquals(str: String, a: T, b: T) { if (a != b) throw AssertionError(str) }

fun assertTrue(cond: Boolean) = assertTrue("", cond)
fun assertFalse(cond: Boolean) = assertFalse("", cond)
fun <T> assertEquals(a: T, b: T) = assertEquals("", a, b)

fun fail(): Unit = throw AssertionError()

fun testConstructor() {
    var b = BitSet(12)
    assertContainsOnly(b, setOf(), 12)

    b = BitSet(12) { it == 0 || it in 5..6 || it == 11 }
    assertContainsOnly(b, setOf(0, 5, 6, 11), 12)
}

fun testSet() {
    var b = BitSet(0)
    assertEquals(b.lastTrueIndex, -1)
    b = BitSet(2)
    // Test set and clear operation for single index.
    assertTrue(b.isEmpty)
    b.set(0, true)
    b.set(3, true)
    assertEquals(b.lastTrueIndex, 3)
    assertFalse(b.isEmpty)
    assertContainsOnly(b, setOf(0, 3), 4)
    b.clear()
    assertContainsOnly(b, setOf(), 4)
    b.set(1)
    b.set(5)
    assertEquals(b.lastTrueIndex, 5)
    assertContainsOnly(b, setOf(1, 5), 6)
    b.set(1, false)
    b.set(7, false)
    assertEquals(b.lastTrueIndex, 5)
    assertContainsOnly(b, setOf(5), 8)
    b.clear(5)
    assertEquals(b.lastTrueIndex, -1)
    assertContainsOnly(b, setOf(), 8)
    b.set(70)
    assertEquals(b.lastTrueIndex, 70)
    assertContainsOnly(b, setOf(70), 71)
    b.clear(70)
    assertEquals(b.lastTrueIndex, -1)
    assertContainsOnly(b, setOf(), 71)

    // Test set and clear operations for ranges.
    // Set false and clear.
    b = BitSet(2)
    assertContainsOnly(b, setOf(), 2)
    b.set(0..70, true)
    assertNotContainsOnly(b, setOf(), 71)
    b.set(0..2, false)
    assertNotContainsOnly(b, setOf(0, 1, 2), 71)
    b.set(63..65, false)
    assertNotContainsOnly(b, setOf(0, 1, 2, 63, 64, 65), 71)
    b.set(68..70, false)
    assertNotContainsOnly(b, setOf(0, 1, 2, 63, 64, 65, 68, 69, 70), 71)
    b.set(68..72, false)
    assertNotContainsOnly(b, setOf(0, 1, 2, 63, 64, 65, 68, 69, 70, 71, 72), 73)
    b.set(0..72, false)
    assertContainsOnly(b, setOf(), 73)

    b.set(0..72)
    assertNotContainsOnly(b, setOf(), 73)
    b.clear(0..2)
    assertNotContainsOnly(b, setOf(0, 1, 2), 73)
    b.clear(63..65)
    assertNotContainsOnly(b, setOf(0, 1, 2, 63, 64, 65), 73)
    b.clear(68..70)
    assertNotContainsOnly(b, setOf(0, 1, 2, 63, 64, 65, 68, 69, 70), 73)
    b.clear(68..72)
    assertNotContainsOnly(b, setOf(0, 1, 2, 63, 64, 65, 68, 69, 70, 71, 72), 73)
    b.clear(0..72)
    assertContainsOnly(b, setOf(), 73)

    // Set true.
    b.set(0..2, true)
    assertContainsOnly(b, setOf(0, 1, 2), 73)
    b.set(63..65, true)
    assertContainsOnly(b, setOf(0, 1, 2, 63, 64, 65), 73)
    b.set(70..72, true)
    assertContainsOnly(b, setOf(0, 1, 2, 63, 64, 65, 70, 71, 72), 73)
    b.set(73..74, true)
    assertContainsOnly(b, setOf(0, 1, 2, 63, 64, 65, 70, 71, 72, 73, 74), 75)
    b.set(0..74, true)
    assertNotContainsOnly(b, setOf(), 75)

    // Test set and clear for pair of indices.
    b = BitSet(2)
    assertContainsOnly(b, setOf(), 71)
    b.set(0, 71, true)
    assertNotContainsOnly(b, setOf(), 71)
    b.set(0, 3, false)
    assertNotContainsOnly(b, setOf(0, 1, 2), 71)
    b.set(63, 66, false)
    assertNotContainsOnly(b, setOf(0, 1, 2, 63, 64, 65), 71)
    b.set(68, 71, false)
    assertNotContainsOnly(b, setOf(0, 1, 2, 63, 64, 65, 68, 69, 70), 71)
    b.set(68, 73, false)
    assertNotContainsOnly(b, setOf(0, 1, 2, 63, 64, 65, 68, 69, 70, 71, 72), 73)
    b.set(0, 73, false)
    assertContainsOnly(b, setOf(), 73)

    b.set(0, 73)
    assertNotContainsOnly(b, setOf(), 73)
    b.clear(0, 3)
    assertNotContainsOnly(b, setOf(0, 1, 2), 73)
    b.clear(63, 66)
    assertNotContainsOnly(b, setOf(0, 1, 2, 63, 64, 65), 73)
    b.clear(68, 71)
    assertNotContainsOnly(b, setOf(0, 1, 2, 63, 64, 65, 68, 69, 70), 73)
    b.clear(68, 73)
    assertNotContainsOnly(b, setOf(0, 1, 2, 63, 64, 65, 68, 69, 70, 71, 72), 73)
    b.clear(0, 73)
    assertContainsOnly(b, setOf(), 73)

    // Set true.
    b.set(0, 3, true)
    assertContainsOnly(b, setOf(0, 1, 2), 73)
    b.set(63, 66, true)
    assertContainsOnly(b, setOf(0, 1, 2, 63, 64, 65), 73)
    b.set(70, 73, true)
    assertContainsOnly(b, setOf(0, 1, 2, 63, 64, 65, 70, 71, 72), 73)
    b.set(73, 75, true)
    assertContainsOnly(b, setOf(0, 1, 2, 63, 64, 65, 70, 71, 72, 73, 74), 75)
    b.set(0, 75, true)
    assertNotContainsOnly(b, setOf(), 75)

    // Access to negative elements must cause an exception
    try {
        b.set(-1)
        fail()
    } catch(e: IndexOutOfBoundsException) {}
    try {
        b.clear(-1)
        fail()
    } catch(e: IndexOutOfBoundsException) {}
    try {
        b.clear(-1..0)
        fail()
    } catch(e: IndexOutOfBoundsException) {}
    try {
        b.set(-1..0)
        fail()
    } catch(e: IndexOutOfBoundsException) {}
    try {
        b[-1]
        fail()
    } catch(e: IndexOutOfBoundsException) {}
}

fun testFlip() {
    val b = BitSet(2)
    b.set(0, true)
    b.set(70, true)
    b.set(63..65, true)
    assertEquals(b.lastTrueIndex, 70)
    // 0 element
    assertContainsOnly(b, setOf(0, 63, 64, 65, 70), 71)
    b.flip(0)
    assertContainsOnly(b, setOf(63, 64, 65, 70), 71)
    b.flip(1)
    assertContainsOnly(b, setOf(1, 63, 64, 65, 70), 71)
    b.flip(0)
    assertContainsOnly(b, setOf(0, 1, 63, 64, 65, 70), 71)

    // last element
    b.flip(70)
    assertContainsOnly(b, setOf(0, 1, 63, 64, 65), 71)
    b.flip(69)
    assertContainsOnly(b, setOf(0, 1, 63, 64, 65, 69), 71)
    b.flip(70)
    assertContainsOnly(b, setOf(0, 1, 63, 64, 65, 69, 70), 71)

    // element in the middle
    b.flip(64)
    assertContainsOnly(b, setOf(0, 1, 63, 65, 69, 70), 71)
    b.flip(65)
    assertContainsOnly(b, setOf(0, 1, 63, 69, 70), 71)
    b.flip(65)
    b.flip(64)
    assertContainsOnly(b, setOf(0, 1, 63, 64, 65, 69, 70), 71)

    // range in the beginning
    b.flip(0..2)
    assertContainsOnly(b, setOf(2, 63, 64, 65, 69, 70), 71)
    b.flip(0, 3)
    assertContainsOnly(b, setOf(0, 1, 63, 64, 65, 69, 70), 71)

    // In the end
    b.flip(68..70)
    assertContainsOnly(b, setOf(0, 1, 63, 64, 65, 68), 71)
    b.flip(68, 71)
    assertContainsOnly(b, setOf(0, 1, 63, 64, 65, 69, 70), 71)

    // In the middle
    b.flip(64..66)
    assertContainsOnly(b, setOf(0, 1, 63, 66, 69, 70), 71)
    b.flip(64, 67)
    assertContainsOnly(b, setOf(0, 1, 63, 64, 65, 69, 70), 71)

    // Access to a negative element must cause an exception.
    try {
        b.flip(-1)
        fail()
    } catch(e: IndexOutOfBoundsException) {}
    try {
        b.flip(-1..0)
        fail()
    } catch(e: IndexOutOfBoundsException) {}
}

fun testNextBit() {
    val b = BitSet(71)
    b.set(0)
    b.set(65)
    b.set(70)
    assertEquals(b.nextSetBit(), 0)
    assertEquals(b.nextSetBit(0), 0)
    assertEquals(b.nextSetBit(1), 65)
    assertEquals(b.nextSetBit(65), 65)
    assertEquals(b.nextSetBit(66), 70)
    assertEquals(b.nextSetBit(70), 70)
    assertEquals(b.nextSetBit(71), -1)

    assertEquals(b.previousSetBit(0), 0)
    assertEquals(b.previousSetBit(64), 0)
    assertEquals(b.previousSetBit(65), 65)
    assertEquals(b.previousSetBit(69), 65)
    assertEquals(b.previousSetBit(70), 70)
    assertEquals(b.previousSetBit(71), 70)

    b.clear()
    assertEquals(b.nextSetBit(), -1)
    assertEquals(b.previousSetBit(70), -1)

    b.set(0..70)
    assertEquals(b.nextClearBit(), 71)
    assertEquals(b.previousClearBit(70), -1)

    b.clear(0)
    b.clear(65)
    b.clear(70)
    assertEquals(b.nextClearBit(), 0)
    assertEquals(b.nextClearBit(0), 0)
    assertEquals(b.nextClearBit(1), 65)
    assertEquals(b.nextClearBit(65), 65)
    assertEquals(b.nextClearBit(66), 70)
    assertEquals(b.nextClearBit(70), 70)
    assertEquals(b.nextClearBit(71), 71) // assume that the bitset is extended here (virtually).

    assertEquals(b.previousClearBit(0), 0)
    assertEquals(b.previousClearBit(64), 0)
    assertEquals(b.previousClearBit(65), 65)
    assertEquals(b.previousClearBit(69), 65)
    assertEquals(b.previousClearBit(70), 70)
    assertEquals(b.previousClearBit(71), 71)

    // See http://docs.oracle.com/javase/8/docs/api/java/util/BitSet.html#previousClearBit-int-
    assertEquals(b.previousClearBit(-1), -1)
    assertEquals(b.previousSetBit(-1), -1)

    // Test behaviour on the right border of the bit vector.
    // We assume that the vector is infinite and have zeros after (size - 1)th bit.
    var a = BitSet(64)
    assertEquals(a.nextClearBit(63), 63)
    assertEquals(a.nextClearBit(64), 64)
    assertEquals(a.nextSetBit(63), -1)
    assertEquals(a.nextSetBit(64), -1)
    a.set(0, 64)
    assertEquals(a.nextClearBit(63), 64)
    assertEquals(a.nextClearBit(64), 64)
    assertEquals(a.nextSetBit(63), 63)
    assertEquals(a.nextSetBit(64), -1)

    a.clear()
    assertEquals(a.previousClearBit(63), 63)
    assertEquals(a.previousClearBit(64), 64)
    assertEquals(a.previousSetBit(63), -1)
    assertEquals(a.previousSetBit(64), -1)
    a.set(0, 64)
    assertEquals(a.previousClearBit(63), -1)
    assertEquals(a.previousClearBit(64), 64)
    assertEquals(a.previousSetBit(63), 63)
    assertEquals(a.previousSetBit(64), 63)

    a = BitSet(0)
    assertEquals(a.nextSetBit(0), -1)
    assertEquals(a.nextClearBit(0), 0)
    assertEquals(a.previousSetBit(0), -1)
    assertEquals(a.previousClearBit(0), 0)

    // Access to a negative element must cause an exception.
    try {
        b.previousSetBit(-2)
        fail()
    } catch(e: IndexOutOfBoundsException) {}
    try {
        b.previousClearBit(-2)
        fail()
    } catch(e: IndexOutOfBoundsException) {}
    try {
        b.nextSetBit(-1)
        fail()
    } catch(e: IndexOutOfBoundsException) {}
    try {
        b.nextClearBit(-1)
        fail()
    } catch(e: IndexOutOfBoundsException) {}
}

fun BitSet.setBits(vararg indices: Int, value: Boolean = true) {
    indices.forEach {
        set(it, value)
    }
}

fun testLogic() {
    var b2 = BitSet(76)
    b2.setBits(1, 3,
            61, 63,
            65, 67,
            70, 72)

    // and
    var b1 = BitSet(73)
    b1.set(2..3); b1.set(62..63); b1.set(66..67); b1.set(71..72)
    b1.and(b2)
    assertContainsOnly(b1, setOf(3, 63, 67, 72), 76)
    b1 = BitSet(73)
    b1.set(2..3); b1.set(62..63); b1.set(66..67); b1.set(71..72)
    b1.set(128)
    b1.and(b2)
    assertContainsOnly(b1, setOf(3, 63, 67, 72), 129)

    // or
    b1 = BitSet(73)
    b1.set(2..3); b1.set(62..63); b1.set(66..67); b1.set(71..72)
    b1.or(b2)
    assertContainsOnly(b1, setOf(1, 2, 3, 61, 62, 63, 65, 66, 67, 70, 71, 72), 76)

    b1 = BitSet(73)
    b1.set(2..3); b1.set(62..63); b1.set(66..67); b1.set(71..72)
    b1.set(128)
    b1.or(b2)
    assertContainsOnly(b1, setOf(1, 2, 3, 61, 62, 63, 65, 66, 67, 70, 71, 72, 128), 129)

    // xor
    b1 = BitSet(73)
    b1.set(2..3); b1.set(62..63); b1.set(66..67); b1.set(71..72)
    b1.xor(b2)
    assertContainsOnly(b1, setOf(1, 2, 61, 62, 65, 66, 70, 71), 76)

    b1 = BitSet(73)
    b1.set(2..3); b1.set(62..63); b1.set(66..67); b1.set(71..72)
    b1.set(128)
    b1.xor(b2)
    assertContainsOnly(b1, setOf(1, 2, 61, 62, 65, 66, 70, 71, 128), 129)

    // andNot
    b1 = BitSet(73)
    b1.set(2..3); b1.set(62..63); b1.set(66..67); b1.set(71..72)
    b1.andNot(b2)
    assertContainsOnly(b1, setOf(2, 62, 66, 71), 76)

    b1 = BitSet(73)
    b1.set(2..3); b1.set(62..63); b1.set(66..67); b1.set(71..72)
    b1.set(128)
    b1.andNot(b2)
    assertContainsOnly(b1, setOf(2, 62, 66, 71, 128), 129)

    // intersects
    b1 = BitSet(73)
    b1.set(0..1); b1.set(62..63); b1.set(64..65); b1.set(71..72)
    b2.clear(); b2.set(0)
    assertTrue(b1.intersects(b2))
    b2.clear(); b2.set(62..65)
    assertTrue(b1.intersects(b2))
    b2.clear(); b2.set(72)
    assertTrue(b1.intersects(b2))
    b2.clear()
    assertFalse(b1.intersects(b2))
    b2.set(128)
    assertFalse(b1.intersects(b2))
}

// Based on Harmony tests.
fun testEqualsHashCode() {
    // HashCode.
    val b = BitSet()
    b.set(0..7)
    b.clear(2)
    b.clear(6)
    assertEquals("BitSet returns wrong hash value", 1129, b.hashCode())
    b.set(10)
    b.clear(3)
    assertEquals("BitSet returns wrong hash value", 97, b.hashCode())

    // Equals.
    val b1 = BitSet()
    val b2 = BitSet()
    b1.set(0..7)
    b2.set(0..7)

    assertTrue("Same BitSet returned false", b1 == b1)
    assertTrue("Identical BitSet returned false", b1 == b2)
    b2.clear(6)
    assertFalse("Different BitSets returned true", b1 == b2)

    val b3 = BitSet()
    b3.set(0..7)
    b3.set(128)
    assertFalse("Different sized BitSet with higher bit set returned true", b1 == b3)
    b3.clear(128)
    assertTrue("Different sized BitSet with higher bits not set returned false", b1 == b3)
}

@Test fun runTest() {
    testConstructor()
    testSet()
    testFlip()
    testNextBit()
    testLogic()
    testEqualsHashCode()
}