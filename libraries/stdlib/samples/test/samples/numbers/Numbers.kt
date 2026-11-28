/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.numbers

import samples.*

class Numbers {
    @Sample
    fun intCountOneBits() {
        assertPrints(0b1011.countOneBits(), "3")
        assertPrints((-1).countOneBits(), "32")
        assertPrints(0.countOneBits(), "0")
    }

    @Sample
    fun intCountLeadingZeroBits() {
        assertPrints(0b1011.countLeadingZeroBits(), "28")
        assertPrints((-1).countLeadingZeroBits(), "0")
        assertPrints(0.countLeadingZeroBits(), "32")
    }

    @Sample
    fun intCountTrailingZeroBits() {
        assertPrints(0b1011000.countTrailingZeroBits(), "3")
        assertPrints((-8).countTrailingZeroBits(), "3")
        assertPrints(0.countTrailingZeroBits(), "32")
    }

    @Sample
    fun intTakeHighestOneBit() {
        assertPrints(0b1011000.takeHighestOneBit(), "64")
        assertPrints((-1).takeHighestOneBit(), "-2147483648")
        assertPrints(0.takeHighestOneBit(), "0")
    }

    @Sample
    fun intTakeLowestOneBit() {
        assertPrints(0b1011000.takeLowestOneBit(), "8")
        assertPrints((-8).takeLowestOneBit(), "8")
        assertPrints(0.takeLowestOneBit(), "0")
    }

    @Sample
    fun intRotateLeft() {
        assertPrints(0x12345678.rotateLeft(8), "878082066")
        assertPrints((-1).rotateLeft(8), "-1")
        assertPrints(0.rotateLeft(8), "0")
    }

    @Sample
    fun intRotateRight() {
        assertPrints(0x12345678.rotateRight(8), "2014458966")
        assertPrints((-1).rotateRight(8), "-1")
        assertPrints(0.rotateRight(8), "0")
    }

    @Sample
    fun longCountOneBits() {
        assertPrints(0b1011L.countOneBits(), "3")
        assertPrints((-1L).countOneBits(), "64")
        assertPrints(0L.countOneBits(), "0")
    }

    @Sample
    fun longCountLeadingZeroBits() {
        assertPrints(0b1011L.countLeadingZeroBits(), "60")
        assertPrints((-1L).countLeadingZeroBits(), "0")
        assertPrints(0L.countLeadingZeroBits(), "64")
    }

    @Sample
    fun longCountTrailingZeroBits() {
        assertPrints(0b1011000L.countTrailingZeroBits(), "3")
        assertPrints((-8L).countTrailingZeroBits(), "3")
        assertPrints(0L.countTrailingZeroBits(), "64")
    }

    @Sample
    fun longTakeHighestOneBit() {
        assertPrints(0b1011000L.takeHighestOneBit(), "64")
        assertPrints((-1L).takeHighestOneBit(), "-9223372036854775808")
        assertPrints(0L.takeHighestOneBit(), "0")
    }

    @Sample
    fun longTakeLowestOneBit() {
        assertPrints(0b1011000L.takeLowestOneBit(), "8")
        assertPrints((-8L).takeLowestOneBit(), "8")
        assertPrints(0L.takeLowestOneBit(), "0")
    }

    @Sample
    fun longRotateLeft() {
        assertPrints(0x123456789ABCDEF0L.rotateLeft(8), "2596069104")
        assertPrints((-1L).rotateLeft(8), "-1")
        assertPrints(0L.rotateLeft(8), "0")
    }

    @Sample
    fun longRotateRight() {
        assertPrints(0x123456789ABCDEF0L.rotateRight(8), "1737580809831919155")
        assertPrints((-1L).rotateRight(8), "-1")
        assertPrints(0L.rotateRight(8), "0")
    }
}
