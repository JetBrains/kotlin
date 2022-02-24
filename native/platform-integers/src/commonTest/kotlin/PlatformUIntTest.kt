/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.Test

class PlatformUIntTest {

    @Test
    fun testPlus() {
        assertPrints(plui(6u) + plui(4u), "10")
    }

    @Test
    fun testMinus() {
        assertPrints(plui(6u) - plui(4u), "2")
    }

    @Test
    fun testTimes() {
        assertPrints(plui(6u) * plui(4u), "24")
    }

    @Test
    fun testDiv() {
        assertPrints(plui(6u) / plui(4u), "1")
    }

    @Test
    fun testRem() {
        assertPrints(plui(6u) % plui(4u), "2")
    }

    @Test
    fun testRangeTo() {
        assertPrints(plui(1u)..plui(5u), "1..5")
    }

    @Test
    fun testEquals() {
        assertPrints(plui(3u) == plui(3u), "true")
    }

    @Test
    fun testInc() {
        var incMe = plui(5u)
        incMe++
        assertPrints(incMe, "6")
    }

    @Test
    fun testDec() {
        var decMe = plui(5u)
        decMe--
        assertPrints(decMe, "4")
    }

    @Test
    fun testCompareTo() {
        assertPrints(plui(15u) < plui(13u), "false")
    }

    @Test
    fun testShl() {
        assertPrints(plui(2u) shl 2, "8")
    }

    @Test
    fun testShr() {
        assertPrints(plui(15u) shr 2, "3")
    }

    @Test
    fun testXor() {
        assertPrints(plui(5u) xor plui(3u), "6")
    }

    @Test
    fun testAnd() {
        assertPrints(plui(5u) and plui(3u), "1")
    }

    @Test
    fun testOr() {
        assertPrints(plui(5u) or plui(3u), "7")
    }

    @Test
    fun testToByte() {
        assertPrints(PlatformUInt.MIN_VALUE.toByte(), "0")
    }

    @Test
    fun testToShort() {
        assertPrints(PlatformUInt.MIN_VALUE.toShort(), "0")
    }

    @Test
    fun testToInt() {
        assertPrints(plui(UInt.MAX_VALUE).toInt(), "-1")
    }

    @Test
    fun testToLong() {
        assertPrints(plui(UShort.MAX_VALUE.toUInt()).toLong(), "${UShort.MAX_VALUE}")
    }

    @Test
    fun testToUByte() {
        assertPrints(plui(UInt.MAX_VALUE).toUByte(), "${UByte.MAX_VALUE}")
    }

    @Test
    fun testToUShort() {
        assertPrints(plui(UInt.MAX_VALUE).toUShort(), "${UShort.MAX_VALUE}")
    }

    @Test
    fun testToUInt() {
        assertPrints(plui(UInt.MAX_VALUE).toUInt(), "${UInt.MAX_VALUE}")
    }

    @Test
    fun testToULong() {
        assertPrints(plui(UInt.MAX_VALUE).toULong(), "${UInt.MAX_VALUE}")
    }

    @Test
    fun testToFloat() {
        assertPrints(plui(0u).toFloat(), "0.0")
    }

    @Test
    fun testToDouble() {
        assertPrints(plui(0u).toDouble(), "0.0")
    }

    @Test
    fun testCountLeadingZeroBits() {
        assertPrints(plui(1u).countLeadingZeroBits(), "${PlatformUInt.SIZE_BITS - 1}")
    }

    @Test
    fun testCountOneBits() {
        assertPrints(plui(3u).countOneBits(), "2")
    }

    @Test
    fun testCountTrailingZeroBits() {
        assertPrints(plui(4u).countTrailingZeroBits(), "2")
    }

    @Test
    @OptIn(ExperimentalStdlibApi::class)
    fun testRotateLeft() {
        assertPrints(plui(42u).rotateLeft(PlatformUInt.SIZE_BITS), "42")
    }

    @Test
    @OptIn(ExperimentalStdlibApi::class)
    fun testRotateRight() {
        assertPrints(plui(42u).rotateRight(PlatformUInt.SIZE_BITS), "42")
    }

    @Test
    fun testTakeHighestOneBit() {
        assertPrints(plui(3u).takeHighestOneBit(), "2")
    }

    @Test
    fun testTakeLowestOneBit() {
        assertPrints(plui(3u).takeLowestOneBit(), "1")
    }
}
