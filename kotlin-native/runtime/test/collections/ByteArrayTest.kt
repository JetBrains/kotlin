/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*

class ByteArrayTest {
    @Test fun getUByteOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        assertEquals(0U, arr.getUByteAt(0))
        assertEquals(0U, arr.getUByteAt(arr.size - 1))
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getUByteAt(arr.size)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getUByteAt(-1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getUByteAt(Int.MAX_VALUE)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getUByteAt(Int.MIN_VALUE)
        }
    }

    @Test fun setUByteOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        arr.setUByteAt(0, 1U)
        arr.setUByteAt(arr.size - 1, 1U)
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setUByteAt(arr.size, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setUByteAt(-1, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setUByteAt(Int.MAX_VALUE, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setUByteAt(Int.MIN_VALUE, 1U)
        }
    }

    @Test fun getCharOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        assertEquals(0.toChar(), arr.getCharAt(0))
        assertEquals(0.toChar(), arr.getCharAt(8))
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getCharAt(arr.size - 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getCharAt(arr.size)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getCharAt(-1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getCharAt(Int.MAX_VALUE)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getCharAt(Int.MIN_VALUE)
        }
    }

    @Test fun setCharOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        arr.setCharAt(0, '1')
        arr.setCharAt(8, '1')
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setCharAt(arr.size - 1, '1')
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setCharAt(arr.size, '1')
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setCharAt(-1, '1')
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setCharAt(Int.MAX_VALUE, '1')
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setCharAt(Int.MIN_VALUE, '1')
        }
    }

    @Test fun getShortOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        assertEquals(0, arr.getShortAt(0))
        assertEquals(0, arr.getShortAt(8))
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getShortAt(arr.size - 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getShortAt(arr.size)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getShortAt(-1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getShortAt(Int.MAX_VALUE)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getShortAt(Int.MIN_VALUE)
        }
    }

    @Test fun setShortOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        arr.setShortAt(0, 0)
        arr.setShortAt(8, 0)
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setShortAt(arr.size - 1, 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setShortAt(arr.size, 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setShortAt(-1, 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setShortAt(Int.MAX_VALUE, 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setShortAt(Int.MIN_VALUE, 1)
        }
    }

    @Test fun getUShortOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        assertEquals(0U, arr.getUShortAt(0))
        assertEquals(0U, arr.getUShortAt(8))
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getUShortAt(arr.size - 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getUShortAt(arr.size)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getUShortAt(-1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getUShortAt(Int.MAX_VALUE)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getUShortAt(Int.MIN_VALUE)
        }
    }

    @Test fun setUShortOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        arr.setUShortAt(0, 0U)
        arr.setUShortAt(8, 0U)
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setUShortAt(arr.size - 1, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setUShortAt(arr.size, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setUShortAt(-1, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setUShortAt(Int.MAX_VALUE, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setUShortAt(Int.MIN_VALUE, 1U)
        }
    }

    @Test fun getIntOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        assertEquals(0, arr.getIntAt(0))
        assertEquals(0, arr.getIntAt(6))
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getIntAt(7)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getIntAt(arr.size)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getIntAt(-1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getIntAt(Int.MAX_VALUE)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getIntAt(Int.MIN_VALUE)
        }
    }

    @Test fun setIntOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        arr.setIntAt(0, 1)
        arr.setIntAt(6, 1)
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setIntAt(7, 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setIntAt(arr.size, 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setIntAt(-1, 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setIntAt(Int.MAX_VALUE, 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setIntAt(Int.MIN_VALUE, 1)
        }
    }

    @Test fun getUIntOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        assertEquals(0U, arr.getUIntAt(0))
        assertEquals(0U, arr.getUIntAt(6))
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getUIntAt(7)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getUIntAt(arr.size)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getUIntAt(-1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getUIntAt(Int.MAX_VALUE)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getUIntAt(Int.MIN_VALUE)
        }
    }

    @Test fun setUIntOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        arr.setUIntAt(0, 1U)
        arr.setUIntAt(6, 1U)
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setUIntAt(7, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setUIntAt(arr.size, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setUIntAt(-1, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setUIntAt(Int.MAX_VALUE, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setUIntAt(Int.MIN_VALUE, 1U)
        }
    }

    @Test fun getLongOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        assertEquals(0, arr.getLongAt(0))
        assertEquals(0, arr.getLongAt(2))
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getLongAt(3)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getLongAt(arr.size)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getLongAt(-1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getLongAt(Int.MAX_VALUE)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getLongAt(Int.MIN_VALUE)
        }
    }

    @Test fun setLongOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        arr.setLongAt(0, 1)
        arr.setLongAt(2, 1)
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setLongAt(3, 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setLongAt(arr.size, 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setLongAt(-1, 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setLongAt(Int.MAX_VALUE, 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setLongAt(Int.MIN_VALUE, 1)
        }
    }

    @Test fun getULongOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        assertEquals(0U, arr.getULongAt(0))
        assertEquals(0U, arr.getULongAt(2))
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getULongAt(3)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getULongAt(arr.size)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getULongAt(-1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getULongAt(Int.MAX_VALUE)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getULongAt(Int.MIN_VALUE)
        }
    }

    @Test fun setULongOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        arr.setULongAt(0, 1U)
        arr.setULongAt(2, 1U)
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setULongAt(3, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setULongAt(arr.size, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setULongAt(-1, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setULongAt(Int.MAX_VALUE, 1U)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setULongAt(Int.MIN_VALUE, 1U)
        }
    }

    @Test fun getFloatOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        assertEquals(0.0f, arr.getFloatAt(0))
        assertEquals(0.0f, arr.getFloatAt(6))
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getFloatAt(7)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getFloatAt(arr.size)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getFloatAt(-1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getFloatAt(Int.MAX_VALUE)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getFloatAt(Int.MIN_VALUE)
        }
    }

    @Test fun setFloatOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        arr.setFloatAt(0, 1.0f)
        arr.setFloatAt(6, 1.0f)
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setFloatAt(7, 1.0f)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setFloatAt(arr.size, 1.0f)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setFloatAt(-1, 1.0f)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setFloatAt(Int.MAX_VALUE, 1.0f)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setFloatAt(Int.MIN_VALUE, 1.0f)
        }
    }

    @Test fun getDoubleOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        assertEquals(0.0, arr.getDoubleAt(0))
        assertEquals(0.0, arr.getDoubleAt(2))
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getDoubleAt(3)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getDoubleAt(arr.size)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getDoubleAt(-1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getDoubleAt(Int.MAX_VALUE)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.getDoubleAt(Int.MIN_VALUE)
        }
    }

    @Test fun setDoubleOutOfBounds() {
        val arr = ByteArray(10) { 0 }
        arr.setDoubleAt(0, 1.0)
        arr.setDoubleAt(2, 1.0)
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setDoubleAt(3, 1.0)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setDoubleAt(arr.size, 1.0)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setDoubleAt(-1, 1.0)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setDoubleAt(Int.MAX_VALUE, 1.0)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr.setDoubleAt(Int.MIN_VALUE, 1.0)
        }
    }

    @Test fun smoke() {
        // These tests assume little endian bit ordering.
        val array = ByteArray(42)
        array.setLongAt(5, 0x1234_5678_9abc_def0)

        expect(0x1234_5678_9abc_def0) { array.getLongAt(5) }
        expect(0xdef0.toInt()) { array.getCharAt(5).toInt() }
        expect(0x9abc.toShort()) { array.getShortAt(7) }
        expect(0x1234_5678) { array.getIntAt(9) }
        expect(0xdef0_0000u) { array.getUIntAt(3) }
        expect(0xf0_00u) { array.getUShortAt(4) }
        expect(0xf0u) { array.getUByteAt(5) }
        expect(0x1234_5678_9abcuL) { array.getULongAt(7) }

        array.setIntAt(2, 0x40100000)
        expect(2.25f) { array.getFloatAt(2) }
        array.setLongAt(11, 0x400c_0000_0000_0000)
        expect(3.5) { array.getDoubleAt(11) }
    }
}