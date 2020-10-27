/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.array5

import kotlin.test.*
import kotlinx.cinterop.*

@Test fun arrayGet() {
    val arr = Array(10) { 0 }
    assertEquals(0, arr[0])
    assertEquals(0, arr[9])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE]
    }
}

@Test fun arraySet() {
    val arr = Array(10) { 0 }
    arr[0] = 1
    arr[9] = 1
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE] = 1
    }
}

@Test fun byteArrayGet() {
    val arr = ByteArray(10) { 0 }
    assertEquals(0, arr[0])
    assertEquals(0, arr[9])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE]
    }
}

@Test fun byteArraySet() {
    val arr = ByteArray(10) { 0 }
    arr[0] = 1
    arr[9] = 1
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE] = 1
    }
}

@Test fun uByteArrayGet() {
    val arr = UByteArray(10) { 0U }
    assertEquals(0U, arr[0])
    assertEquals(0U, arr[9])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE]
    }
}

@Test fun uByteArraySet() {
    val arr = UByteArray(10) { 0U }
    arr[0] = 1U
    arr[9] = 1U
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10] = 1U
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1] = 1U
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE] = 1U
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE] = 1U
    }
}

@Test fun shortArrayGet() {
    val arr = ShortArray(10) { 0 }
    assertEquals(0, arr[0])
    assertEquals(0, arr[9])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE]
    }
}

@Test fun shortArraySet() {
    val arr = ShortArray(10) { 0 }
    arr[0] = 1
    arr[9] = 1
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE] = 1
    }
}

@Test fun uShortArrayGet() {
    val arr = UShortArray(10) { 0U }
    assertEquals(0U, arr[0])
    assertEquals(0U, arr[9])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE]
    }
}

@Test fun uShortArraySet() {
    val arr = UShortArray(10) { 0U }
    arr[0] = 1U
    arr[9] = 1U
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10] = 1U
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1] = 1U
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE] = 1U
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE] = 1U
    }
}

@Test fun intArrayGet() {
    val arr = IntArray(10) { 0 }
    assertEquals(0, arr[0])
    assertEquals(0, arr[9])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE]
    }
}

@Test fun intArraySet() {
    val arr = IntArray(10) { 0 }
    arr[0] = 1
    arr[9] = 1
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE] = 1
    }
}

@Test fun uIntArrayGet() {
    val arr = UIntArray(10) { 0U }
    assertEquals(0U, arr[0])
    assertEquals(0U, arr[9])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE]
    }
}

@Test fun uIntArraySet() {
    val arr = UIntArray(10) { 0U }
    arr[0] = 1U
    arr[9] = 1U
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10] = 1U
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1] = 1U
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE] = 1U
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE] = 1U
    }
}

@Test fun longArrayGet() {
    val arr = LongArray(10) { 0 }
    assertEquals(0, arr[0])
    assertEquals(0, arr[9])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE]
    }
}

@Test fun longArraySet() {
    val arr = LongArray(10) { 0 }
    arr[0] = 1
    arr[9] = 1
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE] = 1
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE] = 1
    }
}

@Test fun uLongArrayGet() {
    val arr = ULongArray(10) { 0U }
    assertEquals(0U, arr[0])
    assertEquals(0U, arr[9])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE]
    }
}

@Test fun uLongArraySet() {
    val arr = ULongArray(10) { 0U }
    arr[0] = 1U
    arr[9] = 1U
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10] = 1U
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1] = 1U
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE] = 1U
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE] = 1U
    }
}

@Test fun floatArrayGet() {
    val arr = FloatArray(10) { 0.0f }
    assertEquals(0.0f, arr[0])
    assertEquals(0.0f, arr[9])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE]
    }
}

@Test fun floatArraySet() {
    val arr = FloatArray(10) { 0.0f }
    arr[0] = 1.0f
    arr[9] = 1.0f
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10] = 1.0f
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1] = 1.0f
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE] = 1.0f
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE] = 1.0f
    }
}

@Test fun doubleArrayGet() {
    val arr = DoubleArray(10) { 0.0 }
    assertEquals(0.0, arr[0])
    assertEquals(0.0, arr[9])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE]
    }
}

@Test fun doubleArraySet() {
    val arr = DoubleArray(10) { 0.0 }
    arr[0] = 1.0
    arr[9] = 1.0
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10] = 1.0
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1] = 1.0
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE] = 1.0
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE] = 1.0
    }
}

@Test fun booleanArrayGet() {
    val arr = BooleanArray(10) { false }
    assertEquals(false, arr[0])
    assertEquals(false, arr[9])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE]
    }
}

@Test fun booleanArraySet() {
    val arr = BooleanArray(10) { false }
    arr[0] = true
    arr[9] = true
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10] = true
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1] = true
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE] = true
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE] = true
    }
}

@Test fun charArrayGet() {
    val arr = CharArray(10) { '0' }
    assertEquals('0', arr[0])
    assertEquals('0', arr[9])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE]
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE]
    }
}

@Test fun charArraySet() {
    val arr = CharArray(10) { '0' }
    arr[0] = '1'
    arr[9] = '1'
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[10] = '1'
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[-1] = '1'
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MAX_VALUE] = '1'
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr[Int.MIN_VALUE] = '1'
    }
}

@Test fun byteArrayGetUByte() {
    val arr = ByteArray(10) { 0 }
    assertEquals(0U, arr.getUByteAt(0))
    assertEquals(0U, arr.getUByteAt(9))
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getUByteAt(10)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getUByteAt(-1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getUByteAt(Int.MAX_VALUE)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getUByteAt(Int.MIN_VALUE)
    }
}

@Test fun byteArraySetUByte() {
    val arr = ByteArray(10) { 0 }
    arr.setUByteAt(0, 1U)
    arr.setUByteAt(9, 1U)
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setUByteAt(10, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setUByteAt(-1, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setUByteAt(Int.MAX_VALUE, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setUByteAt(Int.MIN_VALUE, 1U)
    }
}

@Test fun byteArrayGetChar() {
    val arr = ByteArray(10) { 0 }
    assertEquals(0.toChar(), arr.getCharAt(0))
    assertEquals(0.toChar(), arr.getCharAt(8))
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getCharAt(9)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getCharAt(10)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getCharAt(-1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getCharAt(Int.MAX_VALUE)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getCharAt(Int.MIN_VALUE)
    }
}

@Test fun byteArraySetChar() {
    val arr = ByteArray(10) { 0 }
    arr.setCharAt(0, '1')
    arr.setCharAt(8, '1')
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setCharAt(9, '1')
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setCharAt(10, '1')
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setCharAt(-1, '1')
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setCharAt(Int.MAX_VALUE, '1')
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setCharAt(Int.MIN_VALUE, '1')
    }
}

@Test fun byteArrayGetShort() {
    val arr = ByteArray(10) { 0 }
    assertEquals(0, arr.getShortAt(0))
    assertEquals(0, arr.getShortAt(8))
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getShortAt(9)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getShortAt(10)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getShortAt(-1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getShortAt(Int.MAX_VALUE)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getShortAt(Int.MIN_VALUE)
    }
}

@Test fun byteArraySetShort() {
    val arr = ByteArray(10) { 0 }
    arr.setShortAt(0, 0)
    arr.setShortAt(8, 0)
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setShortAt(9, 1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setShortAt(10, 1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setShortAt(-1, 1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setShortAt(Int.MAX_VALUE, 1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setShortAt(Int.MIN_VALUE, 1)
    }
}

@Test fun byteArrayGetUShort() {
    val arr = ByteArray(10) { 0 }
    assertEquals(0U, arr.getUShortAt(0))
    assertEquals(0U, arr.getUShortAt(8))
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getUShortAt(9)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getUShortAt(10)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getUShortAt(-1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getUShortAt(Int.MAX_VALUE)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getUShortAt(Int.MIN_VALUE)
    }
}

@Test fun byteArraySetUShort() {
    val arr = ByteArray(10) { 0 }
    arr.setUShortAt(0, 0U)
    arr.setUShortAt(8, 0U)
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setUShortAt(9, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setUShortAt(10, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setUShortAt(-1, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setUShortAt(Int.MAX_VALUE, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setUShortAt(Int.MIN_VALUE, 1U)
    }
}

@Test fun byteArrayGetInt() {
    val arr = ByteArray(10) { 0 }
    assertEquals(0, arr.getIntAt(0))
    assertEquals(0, arr.getIntAt(6))
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getIntAt(7)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getIntAt(10)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getIntAt(-1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getIntAt(Int.MAX_VALUE)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getIntAt(Int.MIN_VALUE)
    }
}

@Test fun byteArraySetInt() {
    val arr = ByteArray(10) { 0 }
    arr.setIntAt(0, 1)
    arr.setIntAt(6, 1)
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setIntAt(7, 1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setIntAt(10, 1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setIntAt(-1, 1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setIntAt(Int.MAX_VALUE, 1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setIntAt(Int.MIN_VALUE, 1)
    }
}

@Test fun byteArrayGetUInt() {
    val arr = ByteArray(10) { 0 }
    assertEquals(0U, arr.getUIntAt(0))
    assertEquals(0U, arr.getUIntAt(6))
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getUIntAt(7)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getUIntAt(10)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getUIntAt(-1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getUIntAt(Int.MAX_VALUE)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getUIntAt(Int.MIN_VALUE)
    }
}

@Test fun byteArraySetUInt() {
    val arr = ByteArray(10) { 0 }
    arr.setUIntAt(0, 1U)
    arr.setUIntAt(6, 1U)
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setUIntAt(7, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setUIntAt(10, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setUIntAt(-1, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setUIntAt(Int.MAX_VALUE, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setUIntAt(Int.MIN_VALUE, 1U)
    }
}

@Test fun byteArrayGetLong() {
    val arr = ByteArray(10) { 0 }
    assertEquals(0, arr.getLongAt(0))
    assertEquals(0, arr.getLongAt(2))
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getLongAt(3)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getLongAt(10)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getLongAt(-1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getLongAt(Int.MAX_VALUE)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getLongAt(Int.MIN_VALUE)
    }
}

@Test fun byteArraySetLong() {
    val arr = ByteArray(10) { 0 }
    arr.setLongAt(0, 1)
    arr.setLongAt(2, 1)
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setLongAt(3, 1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setLongAt(10, 1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setLongAt(-1, 1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setLongAt(Int.MAX_VALUE, 1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setLongAt(Int.MIN_VALUE, 1)
    }
}

@Test fun byteArrayGetULong() {
    val arr = ByteArray(10) { 0 }
    assertEquals(0U, arr.getULongAt(0))
    assertEquals(0U, arr.getULongAt(2))
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getULongAt(3)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getULongAt(10)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getULongAt(-1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getULongAt(Int.MAX_VALUE)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getULongAt(Int.MIN_VALUE)
    }
}

@Test fun byteArraySetULong() {
    val arr = ByteArray(10) { 0 }
    arr.setULongAt(0, 1U)
    arr.setULongAt(2, 1U)
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setULongAt(3, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setULongAt(10, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setULongAt(-1, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setULongAt(Int.MAX_VALUE, 1U)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setULongAt(Int.MIN_VALUE, 1U)
    }
}

@Test fun byteArrayGetFloat() {
    val arr = ByteArray(10) { 0 }
    assertEquals(0.0f, arr.getFloatAt(0))
    assertEquals(0.0f, arr.getFloatAt(6))
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getFloatAt(7)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getFloatAt(10)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getFloatAt(-1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getFloatAt(Int.MAX_VALUE)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getFloatAt(Int.MIN_VALUE)
    }
}

@Test fun byteArraySetFloat() {
    val arr = ByteArray(10) { 0 }
    arr.setFloatAt(0, 1.0f)
    arr.setFloatAt(6, 1.0f)
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setFloatAt(7, 1.0f)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setFloatAt(10, 1.0f)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setFloatAt(-1, 1.0f)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setFloatAt(Int.MAX_VALUE, 1.0f)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setFloatAt(Int.MIN_VALUE, 1.0f)
    }
}

@Test fun byteArrayGetDouble() {
    val arr = ByteArray(10) { 0 }
    assertEquals(0.0, arr.getDoubleAt(0))
    assertEquals(0.0, arr.getDoubleAt(2))
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getDoubleAt(3)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getDoubleAt(10)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getDoubleAt(-1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getDoubleAt(Int.MAX_VALUE)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.getDoubleAt(Int.MIN_VALUE)
    }
}

@Test fun byteArraySetDouble() {
    val arr = ByteArray(10) { 0 }
    arr.setDoubleAt(0, 1.0)
    arr.setDoubleAt(2, 1.0)
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setDoubleAt(3, 1.0)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setDoubleAt(10, 1.0)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setDoubleAt(-1, 1.0)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setDoubleAt(Int.MAX_VALUE, 1.0)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        arr.setDoubleAt(Int.MIN_VALUE, 1.0)
    }
}

@Test fun immutableBlobToByteArray() {
    val blob = immutableBlobOf(0, 0)
    val arr = blob.toByteArray(0, 1)
    assertEquals(1, arr.size)
    assertEquals(0, arr[0])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.toByteArray(-1, 1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.toByteArray(0, -1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.toByteArray(0, 10)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.toByteArray(10, 11)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.toByteArray(10, 1)
    }
}

@Test fun immutableBlobToUByteArray() {
    val blob = immutableBlobOf(0, 0)
    val arr = blob.toUByteArray(0, 1)
    assertEquals(1, arr.size)
    assertEquals(0U, arr[0])
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.toUByteArray(-1, 1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.toUByteArray(0, -1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.toUByteArray(0, 10)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.toUByteArray(10, 11)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.toUByteArray(10, 1)
    }
}

@Test fun immutableBlobAsCPointer() {
    val blob = immutableBlobOf(0, 0)
    assertEquals(0, blob.asCPointer(0).pointed.value)
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.asCPointer(10)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.asCPointer(-1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.asCPointer(Int.MAX_VALUE)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.asCPointer(Int.MIN_VALUE)
    }
}

@Test fun immutableBlobAsUCPointer() {
    val blob = immutableBlobOf(0, 0)
    assertEquals(0U, blob.asUCPointer(0).pointed.value)
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.asUCPointer(10)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.asUCPointer(-1)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.asUCPointer(Int.MAX_VALUE)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        blob.asUCPointer(Int.MIN_VALUE)
    }
}
