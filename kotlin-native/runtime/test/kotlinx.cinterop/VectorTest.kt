/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.kotlinx.cinterop

import kotlin.test.*
import kotlinx.cinterop.Vector128
import kotlinx.cinterop.vectorOf

class VectorTest {
    @Test
    fun box() {
        class Box(val value: Vector128)

        val v = vectorOf(1f, 3.162f, 10f, 31f)
        val box = Box(v)
        assertEquals(v, box.value)
    }

    @Test
    fun boxWithExtraFields() {
        class Box(v: Vector128) {
            val extraField: Int = 1
            var value: Vector128 = v
        }

        val box = Box(vectorOf(0, 1, 2, 3))
        assertEquals(vectorOf(0, 1, 2, 3), box.value)
        box.value = vectorOf(0.1f, 1.1f, 2.1f, 3.1f)
        assertEquals(vectorOf(0.1f, 1.1f, 2.1f, 3.1f), box.value)
    }

    @Test
    fun getIntAt() {
        val a = arrayOf(0, 1, 2, 3)
        val v = vectorOf(a[0], a[1], a[2], a[3])
        (0 until 4).forEach { assertEquals(a[it], v.getIntAt(it)) }
        assertFailsWith<IndexOutOfBoundsException> { v.getIntAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { v.getIntAt(4) }
    }

    @Test
    fun getFloatAt() {
        val a = arrayOf(1f, 3.162f, 10f, 31f)
        val v = vectorOf(a[0], a[1], a[2], a[3])
        (0 until 4).forEach { assertEquals(a[it], v.getFloatAt(it)) }
        assertFailsWith<IndexOutOfBoundsException> { v.getFloatAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { v.getFloatAt(4) }
    }

    @Test
    fun getByteAt() {
        val a = arrayOf(0, 1, 2, 3)
        val v = vectorOf(a[0], a[1], a[2], a[3])
        (0 until 4).forEach {
            assertEquals(if (Platform.isLittleEndian) a[it].toByte() else 0, v.getByteAt(it * 4))
            assertEquals(0, v.getByteAt(it * 4 + 1))
            assertEquals(0, v.getByteAt(it * 4 + 2))
            assertEquals(if (!Platform.isLittleEndian) a[it].toByte() else 0, v.getByteAt(it * 4 + 3))
        }
        assertFailsWith<IndexOutOfBoundsException> { v.getByteAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { v.getByteAt(16) }
    }

    @Test
    fun updateVector() {
        var v = vectorOf(0, 1, 2, 3)
        val a = arrayOf(1f, 3.162f, 10f, 31f)
        // Used to be vector of ints, now a vector of floats.
        v = vectorOf(a[0], a[1], a[2], a[3])
        (0 until 4).forEach { assertEquals(a[it], v.getFloatAt(it)) }
    }

    @Test
    fun testToString() {
        val v = vectorOf(100, 1024, Int.MAX_VALUE, Int.MIN_VALUE)
        assertEquals("(0x64, 0x400, 0x7fffffff, 0x80000000)", v.toString())
    }

    @Test
    fun testEquals() {
        assertNotEquals(vectorOf(-1f, 0f, 0f, -7f), vectorOf(1f, 4f, 3f, 7f))
        assertNotEquals(vectorOf(-1f, 0f, 0f, -7f), Any())
        assertEquals(vectorOf(-1f, 0f, 0f, -7f), vectorOf(-1f, 0f, 0f, -7f))
    }

    @Test
    fun testHashCode() {
        assertNotEquals(vectorOf(1f, 4f, 3f, 7f).hashCode(), vectorOf(3f, 7f, 1f, 4f).hashCode())
        assertEquals(vectorOf(1f, 4f, 3f, 7f).hashCode(), vectorOf(1f, 4f, 3f, 7f).hashCode())
    }
}