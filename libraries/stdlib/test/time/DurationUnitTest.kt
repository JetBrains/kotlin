/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.time

import kotlin.math.sign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.DurationUnit.*
import kotlin.time.ExperimentalTime
import kotlin.time.convert
import kotlin.time.convertToWhole

class DurationUnitTest {
    @Test
    @OptIn(ExperimentalTime::class)
    @Suppress("DEPRECATION")
    fun conversion() = doubleConversionBase(Duration::convert)

    @Test
    @OptIn(ExperimentalTime::class)
    fun durationUnitConvertDouble() = doubleConversionBase { value, sourceUnit, targetUnit ->
        DurationUnit.convert(value, sourceUnit, targetUnit)
    }

    fun checkDoubleConversion(
        sourceValue: Double, sourceUnit: DurationUnit, targetValue: Double, targetUnit: DurationUnit,
        conversionFunction: (value: Double, sourceUnit: DurationUnit, targetUnit: DurationUnit) -> Double,
    ) {
        assertEquals(
            targetValue, conversionFunction(sourceValue, sourceUnit, targetUnit),
            "Expected $sourceValue $sourceUnit to be $targetValue $targetUnit"
        )
        assertEquals(
            sourceValue, conversionFunction(targetValue, targetUnit, sourceUnit),
            "Expected $targetValue $targetUnit to be $sourceValue $sourceUnit"
        )
    }

    private fun doubleConversionBase(
        conversionFunction: (value: Double, sourceUnit: DurationUnit, targetUnit: DurationUnit) -> Double,
    ) {
        fun test(sourceValue: Double, sourceUnit: DurationUnit, targetValue: Double, targetUnit: DurationUnit) {
            checkDoubleConversion(sourceValue, sourceUnit, targetValue, targetUnit, conversionFunction)
        }

        test(1.0, MINUTES, 60.0, SECONDS)
        test(30.0, MINUTES, 0.5, HOURS)
        test(12.0, HOURS, 0.5, DAYS)
        test(720.0, MINUTES, 0.5, DAYS)
        test(1.0, DAYS, 86400.0, SECONDS)
        test(1.0, DAYS, 86400e9, NANOSECONDS)
        test(50.0, NANOSECONDS, 0.05, MICROSECONDS)
        test(50.0, NANOSECONDS, 50e-9, SECONDS)
        test(16.0, MILLISECONDS, 0.016, SECONDS)
        test(-1.0, DAYS, -86400.0, SECONDS)
        test(-12.0, HOURS, -0.5, DAYS)

        for (unit in DurationUnit.entries) {
            test(1.0, unit, 1.0, unit)
            test(Double.NaN, unit, Double.NaN, unit)
            test(Double.POSITIVE_INFINITY, unit, Double.POSITIVE_INFINITY, unit)
            test(Double.NEGATIVE_INFINITY, unit, Double.NEGATIVE_INFINITY, unit)
        }

        // One-sided conversions
        assertEquals(Double.POSITIVE_INFINITY, conversionFunction(1e300, DAYS, NANOSECONDS))
        assertEquals(Double.NEGATIVE_INFINITY, conversionFunction(-1e300, DAYS, NANOSECONDS))
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun longValueConversion() {
        fun test(sourceValue: Long, sourceUnit: DurationUnit, targetValue: Long, targetUnit: DurationUnit) {
            assertEquals(
                targetValue, DurationUnit.convertToWhole(sourceValue, sourceUnit, targetUnit),
                "Expected $sourceValue $sourceUnit to be $targetValue $targetUnit"
            )
            assertEquals(
                -targetValue, DurationUnit.convertToWhole(-sourceValue, sourceUnit, targetUnit),
                "Expected ${-sourceValue} $sourceUnit to be ${-targetValue} $targetUnit"
            )
        }

        test(1L, MINUTES, 60L, SECONDS)
        test(30L, MINUTES, 0L, HOURS)
        test(60L, MINUTES, 1L, HOURS)
        test(1L, HOURS, 60L, MINUTES)
        test(12L, HOURS, 0L, DAYS)
        test(1L, DAYS, 24L, HOURS)
        test(25L, HOURS, 1L, DAYS)
        test(720L, MINUTES, 0L, DAYS)
        test(1L, DAYS, 86_400L, SECONDS)
        test(86_400L, SECONDS, 1L, DAYS)
        test(1L, DAYS, 86_400_000_000_000L, NANOSECONDS)
        test(86_400_000_000_000L, NANOSECONDS, 1L, DAYS)
        test(50L, NANOSECONDS, 0L, MICROSECONDS)
        test(1_500_000L, MICROSECONDS, 1L, SECONDS)

        assertEquals(Long.MAX_VALUE, DurationUnit.convertToWhole(110_000L, DAYS, NANOSECONDS))
        assertEquals(Long.MIN_VALUE, DurationUnit.convertToWhole(-110_000L, DAYS, NANOSECONDS))
        assertEquals(106_751L, DurationUnit.convertToWhole(Long.MAX_VALUE, NANOSECONDS, DAYS))

        for (unit in DurationUnit.entries) {
            test(0L, unit, 0L, unit)
            test(1L, unit, 1L, unit)
            test(Long.MAX_VALUE, unit, Long.MAX_VALUE, unit)
            test(Long.MIN_VALUE, unit, Long.MIN_VALUE, unit)

            for (otherUnit in DurationUnit.entries) {
                test(1L, unit, unitConversionTable.getValue(unit).getValue(otherUnit), otherUnit)
            }
        }
    }

    @Test
    fun intValueConversion() {
        fun test(sourceValue: Int, sourceUnit: DurationUnit, targetValue: Long, targetUnit: DurationUnit) {
            assertEquals(
                targetValue, DurationUnit.convertToWhole(sourceValue, sourceUnit, targetUnit),
                "Expected $sourceValue $sourceUnit to be $targetValue $targetUnit"
            )
            assertNotEquals(
                Int.MIN_VALUE,
                sourceValue,
                "The test function can't handle Int.MIN_VALUE, implement checks on your own"
            )
            assertEquals(
                -targetValue, DurationUnit.convertToWhole(-sourceValue, sourceUnit, targetUnit),
                "Expected ${-sourceValue} $sourceUnit to be ${-targetValue} $targetUnit"
            )
        }

        test(1, MINUTES, 60, SECONDS)
        test(30, MINUTES, 0, HOURS)
        test(60, MINUTES, 1, HOURS)
        test(1, HOURS, 60, MINUTES)
        test(12, HOURS, 0, DAYS)
        test(1, DAYS, 24, HOURS)
        test(25, HOURS, 1, DAYS)
        test(720, MINUTES, 0, DAYS)
        test(1, DAYS, 86_400, SECONDS)
        test(86_400, SECONDS, 1, DAYS)
        test(50, NANOSECONDS, 0, MICROSECONDS)
        test(1_500_000, MICROSECONDS, 1, SECONDS)

        assertEquals(86400000000000L, DurationUnit.convertToWhole(1, DAYS, NANOSECONDS))
        assertEquals(-86400000000000L, DurationUnit.convertToWhole(-1, DAYS, NANOSECONDS))
        assertEquals(Long.MAX_VALUE, DurationUnit.convertToWhole(1000000, DAYS, NANOSECONDS))
        assertEquals(Long.MIN_VALUE, DurationUnit.convertToWhole(-1000000, DAYS, NANOSECONDS))
        assertEquals(0, DurationUnit.convertToWhole(Int.MAX_VALUE, NANOSECONDS, DAYS))
        assertEquals(0, DurationUnit.convertToWhole(Int.MIN_VALUE, NANOSECONDS, DAYS))
        assertEquals(2, DurationUnit.convertToWhole(Int.MAX_VALUE, NANOSECONDS, SECONDS))
        assertEquals(-2, DurationUnit.convertToWhole(Int.MIN_VALUE, NANOSECONDS, SECONDS))

        for (unit in DurationUnit.entries) {
            test(0, unit, 0, unit)
            test(1, unit, 1, unit)
            test(Int.MAX_VALUE, unit, Int.MAX_VALUE.toLong(), unit)
            assertEquals(Int.MIN_VALUE.toLong(), DurationUnit.convertToWhole(Int.MIN_VALUE, unit, unit))

            for (otherUnit in DurationUnit.entries) {
                test(1, unit, unitConversionTable.getValue(unit).getValue(otherUnit), otherUnit)
            }
        }
    }

    @Test
    fun unitOrdering() {
        val units = listOf(NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS)
        for (i in units.indices) {
            for (j in units.indices) {
                assertEquals(
                    (i compareTo j).sign,
                    (units[i] compareTo units[j]).sign,
                    "Units ${units[i]} and ${units[j]} are not in the expected order"
                )
            }
        }
    }
}

// maps representing how a unit value (1) in one unit is represented in other units
private val unitConversionTable: Map<DurationUnit, Map<DurationUnit, Long>> = mapOf(
    NANOSECONDS to mapOf(
        NANOSECONDS to 1L,
    ).withDefault { _ -> 0L },
    MICROSECONDS to mapOf(
        NANOSECONDS to 1_000L,
        MICROSECONDS to 1L,
    ).withDefault { _ -> 0L },
    MILLISECONDS to mapOf(
        NANOSECONDS to 1_000_000L,
        MICROSECONDS to 1_000L,
        MILLISECONDS to 1L,
    ).withDefault { _ -> 0L },
    SECONDS to mapOf(
        NANOSECONDS to 1_000_000_000L,
        MICROSECONDS to 1_000_000L,
        MILLISECONDS to 1_000L,
        SECONDS to 1L,
    ).withDefault { _ -> 0L },
    MINUTES to mapOf(
        NANOSECONDS to 60_000_000_000L,
        MICROSECONDS to 60_000_000L,
        MILLISECONDS to 60_000L,
        SECONDS to 60L,
        MINUTES to 1L,
    ).withDefault { _ -> 0L },
    HOURS to mapOf(
        NANOSECONDS to 3_600_000_000_000L,
        MICROSECONDS to 3_600_000_000L,
        MILLISECONDS to 3_600_000L,
        SECONDS to 3_600L,
        MINUTES to 60L,
        HOURS to 1L,
    ).withDefault { _ -> 0L },
    DAYS to mapOf(
        NANOSECONDS to 86_400_000_000_000L,
        MICROSECONDS to 86_400_000_000L,
        MILLISECONDS to 86_400_000L,
        SECONDS to 86_400L,
        MINUTES to 1_440L,
        HOURS to 24L,
        DAYS to 1L,
    )
)
