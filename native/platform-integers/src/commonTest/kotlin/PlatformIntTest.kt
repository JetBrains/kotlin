/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.Test

class PlatformIntTest {

    @Test
    fun testPlus() {
        assertPrints(pli(6) + pli(4), "10")
    }

    @Test
    fun testMinus() {
        assertPrints(pli(4) - pli(6), "-2")
    }

    @Test
    fun testTimes() {
        assertPrints(pli(6) * pli(-4), "-24")
    }

    @Test
    fun testDiv() {
        assertPrints(pli(6) / pli(4), "1")
    }

    @Test
    fun testRem() {
        assertPrints(pli(6) % pli(4), "2")
    }

    @Test
    fun testUnaryPlus() {
        assertPrints(+pli(5), "5")
    }

    @Test
    fun testUnaryMinus() {
        assertPrints(-pli(5), "-5")
    }

    @Test
    fun testRangeTo() {
        assertPrints(pli(1)..pli(5), "1..5")
    }

    @Test
    fun testEquals() {
        assertPrints(pli(-3) == pli(-3), "true")
    }

    @Test
    fun testInc() {
        var incMe = pli(5)
        incMe++
        assertPrints(incMe, "6")
    }

    @Test
    fun testDec() {
        var decMe = pli(5)
        decMe--
        assertPrints(decMe, "4")
    }

    @Test
    fun testCompareTo() {
        assertPrints(pli(15) < pli(13), "false")
    }

    @Test
    fun testShl() {
        assertPrints(pli(-2) shl 2, "-8")
    }

    @Test
    fun testShr() {
        assertPrints(pli(15) shr 2, "3")
    }

    @Test
    fun testUshr() {
        assertPrints(pli(-2) ushr 1, "${PlatformInt.MAX_VALUE}")
    }

    @Test
    fun testXor() {
        assertPrints(pli(-5) xor pli(-3), "6")
    }

    @Test
    fun testAnd() {
        assertPrints(pli(-5) and pli(-3), "-7")
    }

    @Test
    fun testOr() {
        assertPrints(pli(-5) or pli(-3), "-1")
    }

    @Test
    fun testToByte() {
        assertPrints(PlatformInt.MIN_VALUE.toByte(), "0")
    }

    @Test
    fun testToShort() {
        assertPrints(PlatformInt.MIN_VALUE.toByte(), "0")
    }

    @Test
    fun testToInt() {
        assertPrints(pli(Int.MAX_VALUE).toInt(), "${Int.MAX_VALUE}")
    }

    @Test
    fun testToLong() {
        assertPrints(pli(Int.MAX_VALUE).toLong(), "${Int.MAX_VALUE}")
    }

    @Test
    fun testToFloat() {
        assertPrints(pli(0).toFloat(), "0.0")
    }

    @Test
    fun testToDouble() {
        assertPrints(pli(0).toDouble(), "0.0")
    }

    @Test
    fun testToChar() {
        assertPrints(pli('a'.code).toChar(), "a")
    }

    @Test
    fun testToUByte() {
        assertPrints(pli(-1).toUByte(), "${UByte.MAX_VALUE}")
    }

    @Test
    fun testToUShort() {
        assertPrints(pli(-1).toUShort(), "${UShort.MAX_VALUE}")
    }

    @Test
    fun testToUInt() {
        assertPrints(pli(-1).toUInt(), "${UInt.MAX_VALUE}")
    }

    @Test
    fun testToULong() {
        assertPrints(pli(-1L).toULong(), "${PlatformUInt.MAX_VALUE}")
    }

    @Test
    fun testFloorDiv() {
        assertPrints(pli(-3).floorDiv(pli(2)), "-2")
    }

    @Test
    fun testCountLeadingZeroBits() {
        assertPrints(pli(-1).countLeadingZeroBits(), "0")
        assertPrints(pli(0).countLeadingZeroBits(), "${PlatformInt.SIZE_BITS}")
    }

    @Test
    fun testCountOneBits() {
        assertPrints(pli(-1).countOneBits(), "${PlatformInt.SIZE_BITS}")
        assertPrints(pli(0).countOneBits(), "0")
    }

    @Test
    fun testCountTrailingZeroBits() {
        assertPrints(pli(-4).countTrailingZeroBits(), "2")
        assertPrints(pli(4).countTrailingZeroBits(), "2")
    }

    @Test
    @OptIn(ExperimentalStdlibApi::class)
    fun testRotateLeft() {
        assertPrints(pli(42).rotateLeft(PlatformInt.SIZE_BITS), "42")
    }

    @Test
    @OptIn(ExperimentalStdlibApi::class)
    fun testRotateRight() {
        assertPrints(pli(42).rotateRight(PlatformInt.SIZE_BITS), "42")
    }

    @Test
    fun testTakeHighestOneBit() {
        assertPrints(pli(3).takeHighestOneBit(), "2")
        assertPrints(pli(-1).takeHighestOneBit(), "${PlatformInt.MIN_VALUE}")
    }

    @Test
    fun testTakeLowestOneBit() {
        assertPrints(pli(3).takeLowestOneBit(), "1")
    }
}
