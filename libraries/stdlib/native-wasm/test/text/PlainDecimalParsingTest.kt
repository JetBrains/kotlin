/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.internal.dtoa.parsePlainDoubleOrNaN
import kotlin.internal.dtoa.parsePlainFloatOrNaN
import kotlin.math.pow
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.text.FloatingPointParser

/**
 * Checks that the fast path for plain decimals in [FloatingPointParser] gives bit-identical results
 * to the general parser, and that it leaves everything else to the general parser.
 */
class PlainDecimalParsingTest {

    private fun assertSameDouble(s: String) {
        val plain = parsePlainDoubleOrNaN(s)
        if (plain.isNaN()) return
        val general = FloatingPointParser.parseDoubleGeneral(s)
        assertEquals(general.toRawBits(), plain.toRawBits(), "Fast path result $plain differs from general $general for \"$s\"")
    }

    private fun assertSameFloat(s: String) {
        val plain = parsePlainFloatOrNaN(s)
        if (plain.isNaN()) return
        val general = FloatingPointParser.parseFloatGeneral(s)
        assertEquals(general.toRawBits(), plain.toRawBits(), "Fast path result $plain differs from general $general for \"$s\"")
    }

    private fun assertSame(s: String) {
        assertSameDouble(s)
        assertSameFloat(s)
    }

    @Test
    fun commonInputsTakeFastPath() {
        for (s in listOf("0", "-0", "+0", "0.0", "-0.0", "1", "-77", "0.1234567890", "15.5", "-1.77", "+770e-1", "7.7E1", "1e22", "1e-22")) {
            assertFalse(parsePlainDoubleOrNaN(s).isNaN(), "Expected Double fast path for \"$s\"")
            assertSameDouble(s)
        }
        for (s in listOf("0", "-0", "+0", "0.0", "-0.0", "1", "-77", "0.1234567", "15.5", "-1.77", "+770e-1", "7.7E1", "1e10", "1e-10")) {
            assertFalse(parsePlainFloatOrNaN(s).isNaN(), "Expected Float fast path for \"$s\"")
            assertSameFloat(s)
        }
        assertEquals((-0.0).toRawBits(), parsePlainDoubleOrNaN("-0.0").toRawBits())
        assertEquals((-0.0f).toRawBits(), parsePlainFloatOrNaN("-0").toRawBits())
    }

    @Test
    fun otherInputsAreLeftToGeneralParser() {
        val inputs = listOf(
            "", "-", "+", ".", "-.", ".5", "+.5", "1.", "-1.", "1.e5", "1e", "1e+", "1e-", "1e+-1", "--1", "+-1", "1.2.3", "1x",
            " 1", "1 ", "\t1.5\n", "1.5f", "1.5F", "1.5d", "1.5D", "0x1p3", "-0x1.8p1",
            "NaN", "-NaN", "Infinity", "+Infinity", "-Infinity",
            "1e2147483648", "1e-2147483649", "0e-400", "1e23", "1e-23", "123456789012345678901234567890",
            "9007199254740993", "1.0000000000000000000000001",
        )
        for (s in inputs) {
            assertTrue(parsePlainDoubleOrNaN(s).isNaN(), "Expected no Double fast path for \"$s\"")
            assertTrue(parsePlainFloatOrNaN(s).isNaN(), "Expected no Float fast path for \"$s\"")
        }
        for (s in listOf("16777217", "1e11", "1e-11", "0.000000000001")) {
            assertTrue(parsePlainFloatOrNaN(s).isNaN(), "Expected no Float fast path for \"$s\"")
        }
    }

    @Test
    fun boundaries() {
        val inputs = listOf(
            // Mantissa boundaries: 2^53 and 2^24, and their neighbours.
            "9007199254740991", "9007199254740992", "9007199254740993", "-9007199254740992",
            "900719925474099.2", "9.007199254740992e15", "9.007199254740992e-7", "0.9007199254740992e22",
            "16777215", "16777216", "16777217", "1.6777216e7", "1.6777216e-3", "-16777.216",
            // Power boundaries: 10^22 and 10^10.
            "1e22", "-1e22", "1e-22", "9007199254740992e22", "9007199254740992e-22", "1e23", "1e-23",
            "1e10", "1e-10", "16777216e10", "16777216e-10", "1e11", "1e-11",
            "0.0000000000000000000001", "100000000000000000000.0", "1000000000000000000000",
            // Leading and trailing zeros.
            "0000000000000000000000000001", "1.0000000000000000000", "00000.00000", "0.0000000000000000000000",
            "0e0", "0e22", "-0e-22", "0E+5", "000123.456000",
            // Max and min values neighbourhood within the fast path.
            "1.7976931348623157", "4.9e-3", "3.4028235", "1.4e-5",
            // Exponents written with many digits.
            "1e0000000000000000000001", "1e-0000000000000000000022",
            // Values from KT-89017 and typical CSV data.
            "15.5", "0.1234567890", "-122.4194155", "37.7749295", "0.1", "0.2", "0.3", "2.5", "1.005",
        )
        for (s in inputs) assertSame(s)
    }

    @Test
    fun randomPlainDecimals() {
        val random = Random(42)
        repeat(20_000) {
            val s = buildString {
                when (random.nextInt(3)) {
                    0 -> append('-')
                    1 -> append('+')
                }
                repeat(random.nextInt(1, 12)) { append('0' + random.nextInt(10)) }
                if (random.nextBoolean()) {
                    append('.')
                    repeat(random.nextInt(1, 18)) { append('0' + random.nextInt(10)) }
                }
                if (random.nextInt(4) == 0) {
                    append(if (random.nextBoolean()) 'e' else 'E')
                    when (random.nextInt(3)) {
                        0 -> append('-')
                        1 -> append('+')
                    }
                    append(random.nextInt(0, 30))
                }
            }
            assertSame(s)
        }
    }

    @Test
    fun randomShortMantissas() {
        // Short mantissas are the ones the Float fast path accepts, so test them more densely.
        val random = Random(43)
        repeat(10_000) {
            val mantissa = random.nextLong(0, 1L shl 25)
            val digits = mantissa.toString()
            val point = random.nextInt(0, digits.length + 1)
            val s = buildString {
                if (random.nextBoolean()) append('-')
                append(digits, 0, point.coerceAtLeast(1))
                if (point in 1 until digits.length) {
                    append('.')
                    append(digits, point, digits.length)
                }
                if (random.nextBoolean()) {
                    append('e')
                    append(random.nextInt(-25, 26))
                }
            }
            assertSame(s)
        }
    }

    @Test
    fun randomFixedPointValues() {
        val random = Random(44)
        repeat(10_000) {
            val decimals = random.nextInt(1, 17)
            val integerPart = random.nextLong(0, 1_000_000)
            val fraction = buildString { repeat(decimals) { append('0' + random.nextInt(10)) } }
            assertSame("$integerPart.$fraction")
        }
    }

    @Test
    fun randomToStringOutputs() {
        val random = Random(45)
        repeat(3_000) {
            val d = random.nextDouble() * 10.0.pow(random.nextInt(-25, 25))
            assertSame(d.toString())
            assertSame((-d).toString())
            val f = d.toFloat()
            assertSame(f.toString())
        }
    }
}
