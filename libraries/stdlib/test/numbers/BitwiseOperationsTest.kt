/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.numbers

import kotlin.test.*

// TODO: Run these tests during compiler test only (JVM & JS)
class BitwiseOperationsTest {
    @Test fun orForInt() {
        assertEquals(3, 2 or 1)
    }

    @Test fun andForInt() {
        assertEquals(0, 1 and 0)
    }

    @Test fun xorForInt() {
        assertEquals(1, 2 xor 3)
    }

    @Test fun shlForInt() {
        assertEquals(4, 1 shl 2)
        assertEquals(4, 1 shl 34)
    }

    @Test fun shrForInt() {
        assertEquals(1, 2 shr 1)
        assertEquals(1, 2 shr 33)
    }

    @Test fun ushrForInt() {
        assertEquals(2147483647, -1 ushr 1)
        assertEquals(2147483647, -1 ushr 33)
    }

    @Test fun shlForUInt() {
        assertEquals(4u, 1u shl 2)
        assertEquals(4u, 1u shl 34)
    }

    @Test fun shrForUInt() {
        assertEquals(1u, 2u shr 1)
        assertEquals(1u, 2u shr 33)

        assertEquals(Int.MAX_VALUE.toUInt(), UInt.MAX_VALUE shr 1)
        assertEquals(Int.MAX_VALUE.toUInt(), UInt.MAX_VALUE shr 33)
    }

    @Test fun shlForLong() {
        assertEquals(4L, 1L shl 2)
        assertEquals(4L, 1L shl 66)
    }

    @Test fun shrForLong() {
        assertEquals(1L, 2L shr 1)
        assertEquals(1L, 2L shr 65)
    }

    @Test fun ushrForLong() {
        assertEquals(Long.MAX_VALUE, -1L ushr 1)
        assertEquals(Long.MAX_VALUE, -1L ushr 65)
    }

    @Test fun shlForULong() {
        assertEquals(4UL, 1UL shl 2)
        assertEquals(4UL, 1UL shl 66)
    }

    @Test fun shrForULong() {
        assertEquals(1UL, 2UL shr 1)
        assertEquals(1UL, 2UL shr 65)

        assertEquals(Long.MAX_VALUE.toULong(), ULong.MAX_VALUE shr 1)
        assertEquals(Long.MAX_VALUE.toULong(), ULong.MAX_VALUE shr 65)
    }

    @Test fun invForInt() {
        assertEquals(0, (-1).inv())
    }
}