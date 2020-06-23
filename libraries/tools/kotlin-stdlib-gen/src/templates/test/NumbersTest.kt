/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates.test

import templates.*
import templates.Family.*

object NumbersTest : TestTemplateGroupBase() {
    val f_rotate = test("rotate()") {
        include(Primitives, setOf(PrimitiveType.Byte, PrimitiveType.Short, PrimitiveType.Int, PrimitiveType.Long))
        include(Unsigned)
    } builder {
        body {
            """
            fun test(value: $P, n: Int, expected: $P) {
                assertEquals(expected, value.rotateLeft(n))
                assertEquals(expected, value.rotateRight(-n))
            }
    
            fun testCyclic(value: $P) {
                for (n in -2 * $P.SIZE_BITS..2 * $P.SIZE_BITS) {
                    val rl = value.rotateLeft(n)
                    val rr = value.rotateRight(-n)
                    assertEquals(rl, rr)
                    assertEquals(rl, value.rotateLeft(n % $P.SIZE_BITS))
                    assertEquals(rr, value.rotateRight((-n) % $P.SIZE_BITS))
                    assertEquals(value, value.rotateLeft(n).rotateLeft(-n))
                    assertEquals(value, value.rotateRight(n).rotateRight(-n))
                }
            }
            """
        }
        bodyAppend(PrimitiveType.Byte) {
            """
            test(0x73, 4, 0x37)
            test(0x73, -3, 0x6E)
            test(0x73, 1, 0xE6.toByte())
            test(0xE6.toByte(), 1, 0xCD.toByte())
            """
        }
        bodyAppend(PrimitiveType.Short) {
            """
            test(0x7361, 4, 0x3617)
            test(0x7361, -3, 0b001_0111_0011_0110_0)
            test(0x7361, 1,  0b111_0011_0110_0001_0.toShort())
            test(0xE6C2.toShort(), 1, 0b11_0011_0110_0001_01.toShort())
            """
        }
        bodyAppend(PrimitiveType.Int) {
            """
            test(0x7_3422345, 4, 0x3422345_7)
            test(0x7342234_5, -4, 0x5_7342234)
            test(0x73422345, 1, 0xE684468A.toInt())
            """
        }
        bodyAppend(PrimitiveType.Long) {
            """
            test(0x7372ABAC_DEEF0123, 4, 0x372ABAC_DEEF01237)
            test(0x88888888_44444444U.toLong(), -3, 0x91111111_08888888u.toLong())
            test(0x88888888_44444444U.toLong(),  1, 0x11111110_88888889)
            """
        }
        bodyAppend(PrimitiveType.UByte) {
            """
            test(0x73u, 4, 0x37u)
            test(0x73u, -3, 0x6Eu)
            test(0x73u, 1, 0xE6u)
            test(0xE6u, 1, 0xCDu)
            """
        }
        bodyAppend(PrimitiveType.UShort) {
            """
            test(0x7361u, 4, 0x3617u)
            test(0x7361u, -3, 0b001_0111_0011_0110_0u)
            test(0x7361u, 1,  0b111_0011_0110_0001_0u)
            test(0xE6C2u, 1,  0b11_0011_0110_0001_01u)
            """
        }
        bodyAppend(PrimitiveType.UInt) {
            """
            test(0x7_3422345u, 4, 0x3422345_7u)
            test(0x7342234_5u, -4, 0x5_7342234u)
            test(0x73422345u, 1, 0xE684468Au)
            """
        }
        bodyAppend(PrimitiveType.ULong) {
            """
            test(0x7372ABAC_DEEF0123uL, 4, 0x372ABAC_DEEF01237uL)
            test(0x88888888_44444444uL, -3, 0x91111111_08888888uL)
            test(0x88888888_44444444uL,  1, 0x11111110_88888889uL)
            """
        }
        bodyAppend {
            """
            repeat(100) {
                testCyclic(${primitive!!.randomNext()})
            }
            """
        }
    }

    val f_bits = test("bits()") {
        include(Primitives, setOf(PrimitiveType.Byte, PrimitiveType.Short, PrimitiveType.Int, PrimitiveType.Long))
        include(Unsigned)
    } builder {
        body {
            val sizeBits = primitive!!.SIZE_BITS
            """
            fun test(value: $P, oneBits: Int, leadingZeroes: Int, trailingZeroes: Int) {
                assertEquals(oneBits, value.countOneBits())
                assertEquals(leadingZeroes, value.countLeadingZeroBits())
                assertEquals(trailingZeroes, value.countTrailingZeroBits())
                val highestBit = if (leadingZeroes < $P.SIZE_BITS) ONE.shl($P.SIZE_BITS - leadingZeroes - 1).to$P() else ZERO
                val lowestBit = if (trailingZeroes < $P.SIZE_BITS) ONE.shl(trailingZeroes).to$P() else ZERO
                assertEquals(highestBit, value.takeHighestOneBit())
                assertEquals(lowestBit, value.takeLowestOneBit())
            }
            
            test(ZERO, 0, $sizeBits, $sizeBits)
            test(ONE, 1, ${sizeBits - 1}, 0)
            test(TWO, 1, ${sizeBits - 2}, 1)
            """
        }
        bodyAppend(PrimitiveType.Byte) {
            """
            test(0x44, 2, 1, 2)
            test(0x80.toByte(), 1, 0, 7)
            test(0xF0.toByte(), 4, 0, 4)
            """
        }
        bodyAppend(PrimitiveType.Short) {
            """
            test(0xF2, 5, 8, 1)
            test(0x8000.toShort(), 1, 0, 15)
            test(0xF200.toShort(), 5, 0, 9)
            """
        }
        bodyAppend(PrimitiveType.Int) {
            """
            test(0xF002, 5, 16, 1)
            test(0xF00F0000.toInt(), 8, 0, 16)
            """
        }
        bodyAppend(PrimitiveType.Long) {
            """
            test(0xF002, 5, 48, 1)
            test(0xF00F0000L, 8, 32, 16)
            test(0x1111_3333_EEEE_0000L, 4 + 8 + 12, 3, 17)
            """
        }
        bodyAppend(PrimitiveType.UByte) {
            """
            test(0x44u, 2, 1, 2)
            test(0x80u, 1, 0, 7)
            test(0xF0u, 4, 0, 4)
            """
        }
        bodyAppend(PrimitiveType.UShort) {
            """
            test(0xF2u, 5, 8, 1)
            test(0x8000u, 1, 0, 15)
            test(0xF200u, 5, 0, 9)
            """
        }
        bodyAppend(PrimitiveType.UInt) {
            """
            test(0xF002u, 5, 16, 1)
            test(0xF00F0000u, 8, 0, 16)
            """
        }
        bodyAppend(PrimitiveType.ULong) {
            """
            test(0xF002uL, 5, 48, 1)
            test(0xF00F0000uL, 8, 32, 16)
            test(0x1111_3333_EEEE_0000uL, 4 + 8 + 12, 3, 17)
            """
        }
    }
}