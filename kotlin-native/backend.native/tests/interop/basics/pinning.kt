/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*
import kotlinx.cinterop.*

@Test fun pinnedByteArrayAddressOf() {
    val arr = ByteArray(10) { 0 }
    arr.usePinned {
        assertEquals(0, it.addressOf(0).pointed.value)
        assertEquals(0, it.addressOf(9).pointed.value)
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(10)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(-1)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MAX_VALUE)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MIN_VALUE)
        }
    }
}

@Test fun pinnedStringAddressOf() {
    val str = "0000000000"
    str.usePinned {
        it.addressOf(0)
        it.addressOf(9)
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(10)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(-1)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MAX_VALUE)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MIN_VALUE)
        }
    }
}

@Test fun pinnedCharArrayAddressOf() {
    val arr = CharArray(10) { '0' }
    arr.usePinned {
        it.addressOf(0)
        it.addressOf(9)
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(10)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(-1)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MAX_VALUE)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MIN_VALUE)
        }
    }
}

@Test fun pinnedShortArrayAddressOf() {
    val arr = ShortArray(10) { 0 }
    arr.usePinned {
        assertEquals(0, it.addressOf(0).pointed.value)
        assertEquals(0, it.addressOf(9).pointed.value)
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(10)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(-1)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MAX_VALUE)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MIN_VALUE)
        }
    }
}

@Test fun pinnedIntArrayAddressOf() {
    val arr = IntArray(10) { 0 }
    arr.usePinned {
        assertEquals(0, it.addressOf(0).pointed.value)
        assertEquals(0, it.addressOf(9).pointed.value)
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(10)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(-1)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MAX_VALUE)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MIN_VALUE)
        }
    }
}

@Test fun pinnedLongArrayAddressOf() {
    val arr = LongArray(10) { 0 }
    arr.usePinned {
        assertEquals(0, it.addressOf(0).pointed.value)
        assertEquals(0, it.addressOf(9).pointed.value)
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(10)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(-1)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MAX_VALUE)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MIN_VALUE)
        }
    }
}

@Test fun pinnedUByteArrayAddressOf() {
    val arr = UByteArray(10) { 0U }
    arr.usePinned {
        assertEquals(0U, it.addressOf(0).pointed.value)
        assertEquals(0U, it.addressOf(9).pointed.value)
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(10)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(-1)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MAX_VALUE)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MIN_VALUE)
        }
    }
}

@Test fun pinnedUShortArrayAddressOf() {
    val arr = UShortArray(10) { 0U }
    arr.usePinned {
        assertEquals(0U, it.addressOf(0).pointed.value)
        assertEquals(0U, it.addressOf(9).pointed.value)
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(10)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(-1)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MAX_VALUE)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MIN_VALUE)
        }
    }
}

@Test fun pinnedUIntArrayAddressOf() {
    val arr = UIntArray(10) { 0U }
    arr.usePinned {
        assertEquals(0U, it.addressOf(0).pointed.value)
        assertEquals(0U, it.addressOf(9).pointed.value)
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(10)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(-1)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MAX_VALUE)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MIN_VALUE)
        }
    }
}

@Test fun pinnedULongArrayAddressOf() {
    val arr = ULongArray(10) { 0U }
    arr.usePinned {
        assertEquals(0U, it.addressOf(0).pointed.value)
        assertEquals(0U, it.addressOf(9).pointed.value)
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(10)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(-1)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MAX_VALUE)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MIN_VALUE)
        }
    }
}

@Test fun pinnedFloatArrayAddressOf() {
    val arr = FloatArray(10) { 0.0f }
    arr.usePinned {
        assertEquals(0.0f, it.addressOf(0).pointed.value)
        assertEquals(0.0f, it.addressOf(9).pointed.value)
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(10)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(-1)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MAX_VALUE)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MIN_VALUE)
        }
    }
}

@Test fun pinnedDoubleArrayAddressOf() {
    val arr = DoubleArray(10) { 0.0 }
    arr.usePinned {
        assertEquals(0.0, it.addressOf(0).pointed.value)
        assertEquals(0.0, it.addressOf(9).pointed.value)
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(10)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(-1)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MAX_VALUE)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            it.addressOf(Int.MIN_VALUE)
        }
    }
}
