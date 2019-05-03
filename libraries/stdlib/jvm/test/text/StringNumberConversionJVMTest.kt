/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import test.*
import kotlin.test.*

class StringNumberConversionJVMTest {

    @Test fun toIntArabicDigits() {
        compareConversion({ it.toInt() }, { it.toIntOrNull() }) {
            assertProduces("٢٣١٩٦٠", 231960)
        }
    }

    @Test fun toLongArabicDigits() {
        compareConversion({ it.toLong() }, { it.toLongOrNull() }) {
            assertProduces("٢٣١٩٦٠٧٧٨٤٥٩", 231960778459)
        }
    }

    @Test fun toFloat() {
        compareConversion(String::toFloat, String::toFloatOrNull) {
            assertProduces("77.0", 77.0f)
            assertProduces("-1e39", Float.NEGATIVE_INFINITY)
            assertProduces("1000000000000000000000000000000000000000", Float.POSITIVE_INFINITY)
            assertFailsOrNull("dark side")
            assertFailsOrNull("")
            assertFailsOrNull("   ")
        }
    }


    @Test fun toHexDouble() {
        compareConversion(String::toDouble, String::toDoubleOrNull, ::doubleTotalOrderEquals) {
            assertProduces("0x77p1", (0x77 shl 1).toDouble())
            assertProduces("0x.77P8", 0x77.toDouble())

            assertFailsOrNull("0x77e1")
        }
    }

    @Test fun toBigInteger() {
        compareConversion(String::toBigInteger, String::toBigIntegerOrNull) {
            assertProduces("0", java.math.BigInteger.ZERO)
            assertProduces("1", java.math.BigInteger.ONE)
            assertProduces("-1", java.math.BigInteger.ONE.negate())
            assertProduces("100000000000000000000", java.math.BigInteger("100000000000000000000"))
            assertFailsOrNull("")
            assertFailsOrNull("-")
            assertFailsOrNull("a")
            assertFailsOrNull("-x")
            assertFailsOrNull("1000 000")
        }

        compareConversionWithRadix(String::toBigInteger, String::toBigIntegerOrNull) {
            assertProduces(16, "ABCDEF90ABCDEF9012345678", java.math.BigInteger("ABCDEF90ABCDEF9012345678", 16))
            assertProduces(36, "HazelnutHazelnut", java.math.BigInteger.valueOf(1356099454469L).let { it.multiply(java.math.BigInteger.valueOf(36).pow(8)).add(it) })

            assertFailsOrNull(16, "EFG")
            assertFailsOrNull(10, "-1A")
            assertFailsOrNull(2, "-")
            assertFailsOrNull(3, "")

            assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { "37".toBigInteger(radix = 37) }
            assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { "1".toBigIntegerOrNull(radix = 1) }
        }
    }

    @Test fun toBigDecimal() {
        fun bd(value: String) = java.math.BigDecimal(value)
        compareConversion(String::toBigDecimal, String::toBigDecimalOrNull) {

            assertProduces("-77", bd("-77"))
            assertProduces("-77.0", bd("-77.0"))
            assertProduces("77.", bd("77"))
            assertProduces("123456789012345678901234567890.123456789", bd("123456789012345678901234567890.123456789"))
            assertProduces("-1.77", bd("-1.77"))
            assertProduces("+.77", bd("0.77"))
            assertProduces("7.7e1", bd("77"))
            assertProduces("+770e-1", bd("77.0"))

            assertFailsOrNull("7..7")
            assertFailsOrNull("\t-77 \n")
            assertFailsOrNull("007 not a number")
            assertFailsOrNull("")
            assertFailsOrNull("   ")
        }

        var mc = java.math.MathContext(3, java.math.RoundingMode.UP)
        compareConversion( { it.toBigDecimal(mc) }, { it.toBigDecimalOrNull(mc) }) {
            assertProduces("1.991", bd("2.00"))

            mc = java.math.MathContext(1, java.math.RoundingMode.UNNECESSARY)

            assertFailsWith<ArithmeticException> { "2.991".toBigDecimal(mc) }
            assertFailsWith<ArithmeticException> { "2.991".toBigDecimalOrNull(mc) }
        }
    }

}