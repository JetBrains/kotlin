/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.numbers

import java.math.BigInteger
import java.math.BigDecimal

import kotlin.test.*
import java.math.MathContext
import java.math.RoundingMode

class BigNumbersTest {
    @Test fun testBigInteger() {
        val a = BigInteger("2")
        val b = BigInteger("3")

        assertEquals(BigInteger("5"), a + b)
        assertEquals(BigInteger("-1"), a - b)
        assertEquals(BigInteger("6"), a * b)
        assertEquals(BigInteger("0"), a / b)
        assertEquals(BigInteger("-2"), -a)
        assertEquals(BigInteger("-2"), -a % b)
        assertEquals(BigInteger("1"), (-a).mod(b))
        assertEquals(BigInteger("-2"), (-a).remainder(b))

        assertEquals(BigInteger("3"), a.inc())
        assertEquals(BigInteger("1"), a.dec())
        assertEquals(BigInteger("-3"), a.inv())
        assertEquals(BigInteger("2"), a and b)
        assertEquals(BigInteger("3"), a or b)
        assertEquals(BigInteger("1"), a xor b)
        assertEquals(BigInteger("4"), a shl 1)
        assertEquals(BigInteger("1"), a shr 1)
        assertEquals(BigInteger("0"), a shr 2)
        assertEquals(BigInteger("-4"), -a shl 1)
        assertEquals(BigInteger("-1"), -a shr 1)
        assertEquals(BigInteger("-1"), -a shr 2)

        assertEquals(BigInteger("2"), 2.toBigInteger())
        assertEquals(BigInteger("-3"), -3L.toBigInteger())

        assertEquals(BigDecimal("2"), a.toBigDecimal())
        assertEquals(BigDecimal("0.02"), a.toBigDecimal(2))
        assertEquals(BigDecimal("2E+2"), a.toBigDecimal(-2))
        assertEquals(BigDecimal("2.6E+3"), BigInteger("253").toBigDecimal(-1, MathContext(2, RoundingMode.UP)))
        assertEquals(BigDecimal("2.6E+2"), BigInteger("253").toBigDecimal(mathContext = MathContext(2, RoundingMode.UP)))
        assertEquals(BigDecimal("3"), BigInteger("253").toBigDecimal(2, MathContext(1, RoundingMode.UP)))


        var c = 2.toBigInteger()
        assertEquals(BigInteger("2"), c++)
        assertEquals(BigInteger("3"), c)
        assertEquals(BigInteger("4"), ++c)
        assertEquals(BigInteger("4"), c)

        assertEquals(BigInteger("4"), c--)
        assertEquals(BigInteger("3"), c)
        assertEquals(BigInteger("2"), --c)
        assertEquals(BigInteger("2"), c)
    }

    @Test fun testBigDecimal() {
        val a = BigDecimal("2")
        val b = BigDecimal("3")

        assertEquals(BigDecimal("5"), a + b)
        assertEquals(BigDecimal("-1"), a - b)
        assertEquals(BigDecimal("6"), a * b)
        assertEquals(BigDecimal("2"), BigDecimal("4") / a)
        assertEquals(BigDecimal("-2"), -a)
        assertEquals(BigDecimal("-2"), -a % b)
        assertEquals(BigDecimal("-2"), @Suppress("DEPRECATION_ERROR") (-a).mod(b))
        assertEquals(BigDecimal("-2"), (-a).rem(b))

        assertEquals(BigDecimal("3"), a.inc())
        assertEquals(BigDecimal("1"), a.dec())

        assertEquals(BigDecimal("2"), 2.toBigDecimal())
        assertEquals(BigDecimal("-3"), -3L.toBigDecimal())
        assertEquals(BigDecimal("2.0"), 2f.toBigDecimal())
        assertEquals(BigDecimal("0.5"), 0.5.toBigDecimal())

        var c = "1.5".toBigDecimal()
        assertEquals(BigDecimal("1.5"), c++)
        assertEquals(BigDecimal("2.5"), c)
        assertEquals(BigDecimal("3.5"), ++c)
        assertEquals(BigDecimal("3.5"), c)
        assertEquals(BigDecimal("3.5"), c--)
        assertEquals(BigDecimal("2.5"), c)
        assertEquals(BigDecimal("1.5"), --c)
        assertEquals(BigDecimal("1.5"), c)
    }

    @Test fun bigDecimalDivRounding() {
        val (d1, d2, d3, d4, d5) = (1..5).map { BigDecimal(it.toString()) }
        val d7 = BigDecimal("7")

        assertEquals(d1, d2 / d3)
        assertEquals(d2, d3 / d2)
        assertEquals(d2, d5 / d2)
        assertEquals(d4, d7 / d2)
        assertEquals(d1, d7 / d5)
    }
}

