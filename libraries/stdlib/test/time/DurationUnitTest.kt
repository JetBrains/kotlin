/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.time

import kotlin.math.sign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.DurationUnit.*
import kotlin.time.ExperimentalTime
import kotlin.time.convert

class DurationUnitTest {
    @Test
    @OptIn(ExperimentalTime::class)
    fun conversion() = doubleConversionBase(Duration::convert)

    @Test
    @OptIn(ExperimentalTime::class)
    fun durationUnitConvertDouble() = doubleConversionBase { value, sourceUnit, targetUnit -> sourceUnit.convert(value, targetUnit) }

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
                targetValue, sourceUnit.convert(sourceValue, targetUnit),
                "Expected $sourceValue $sourceUnit to be $targetValue $targetUnit"
            )
            assertEquals(
                -targetValue, sourceUnit.convert(-sourceValue, targetUnit),
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

        assertEquals(Long.MAX_VALUE, DAYS.convert(110_000L, NANOSECONDS))
        assertEquals(Long.MIN_VALUE, DAYS.convert(-110_000L, NANOSECONDS))
        assertEquals(106_751L, NANOSECONDS.convert(Long.MAX_VALUE, DAYS))

        for (unit in DurationUnit.entries) {
            test(0L, unit, 0L, unit)
            test(1L, unit, 1L, unit)
            test(Long.MAX_VALUE, unit, Long.MAX_VALUE, unit)
            test(Long.MIN_VALUE, unit, Long.MIN_VALUE, unit)

            for (otherUnit in DurationUnit.entries) {
                test(1L, unit, unitConversionTable.getValue(unit).getValue(otherUnit).first, otherUnit)
            }
        }
    }

    @Test
    fun intValueConversion() {
        fun test(sourceValue: Int, sourceUnit: DurationUnit, targetValue: Int, targetUnit: DurationUnit) {
            assertEquals(
                targetValue, sourceUnit.convert(sourceValue, targetUnit),
                "Expected $sourceValue $sourceUnit to be $targetValue $targetUnit"
            )
            // These values are coming only from the unit conversion table, so let's use this hack.
            // All other MIN/MAX value conversion cases calls assertEquals directly.
            val minusTargetValue = if (targetValue == Int.MAX_VALUE) Int.MIN_VALUE else -targetValue
            val minusSourceValue = if (sourceValue == Int.MAX_VALUE) Int.MIN_VALUE else -sourceValue
            assertEquals(
                minusTargetValue, sourceUnit.convert(minusSourceValue, targetUnit),
                "Expected $minusSourceValue $sourceUnit to be $minusTargetValue $targetUnit"
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

        assertEquals(Int.MAX_VALUE, DAYS.convert(1, NANOSECONDS))
        assertEquals(Int.MIN_VALUE, DAYS.convert(-1, NANOSECONDS))
        assertEquals(0, NANOSECONDS.convert(Int.MAX_VALUE, DAYS))
        assertEquals(0, NANOSECONDS.convert(Int.MIN_VALUE, DAYS))
        assertEquals(2, NANOSECONDS.convert(Int.MAX_VALUE, SECONDS))
        assertEquals(-2, NANOSECONDS.convert(Int.MIN_VALUE, SECONDS))

        for (unit in DurationUnit.entries) {
            test(0, unit, 0, unit)
            test(1, unit, 1, unit)
            test(Int.MAX_VALUE, unit, Int.MAX_VALUE, unit)
            test(Int.MIN_VALUE, unit, Int.MIN_VALUE, unit)

            for (otherUnit in DurationUnit.entries) {
                test(1, unit, unitConversionTable.getValue(unit).getValue(otherUnit).second, otherUnit)
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
private val unitConversionTable: Map<DurationUnit, Map<DurationUnit, Pair<Long, Int>>> = mapOf(
    NANOSECONDS to mapOf(
        NANOSECONDS to (1L to 1),
    ).withDefault { _ -> 0L to 0 },
    MICROSECONDS to mapOf(
        NANOSECONDS to (1_000L to 1_000),
        MICROSECONDS to (1L to 1),
    ).withDefault { _ -> 0L to 0 },
    MILLISECONDS to mapOf(
        NANOSECONDS to (1_000_000L to 1_000_000),
        MICROSECONDS to (1_000L to 1_000),
        MILLISECONDS to (1L to 1),
    ).withDefault { _ -> 0L to 0 },
    SECONDS to mapOf(
        NANOSECONDS to (1_000_000_000L to 1_000_000_000),
        MICROSECONDS to (1_000_000L to 1_000_000),
        MILLISECONDS to (1_000L to 1_000),
        SECONDS to (1L to 1),
    ).withDefault { _ -> 0L to 0 },
    MINUTES to mapOf(
        NANOSECONDS to (60_000_000_000L to Int.MAX_VALUE),
        MICROSECONDS to (60_000_000L to 60_000_000),
        MILLISECONDS to (60_000L to 60_000),
        SECONDS to (60L to 60),
        MINUTES to (1L to 1),
    ).withDefault { _ -> 0L to 0 },
    HOURS to mapOf(
        NANOSECONDS to (3_600_000_000_000L to Int.MAX_VALUE),
        MICROSECONDS to (3_600_000_000L to Int.MAX_VALUE),
        MILLISECONDS to (3_600_000L to 3_600_000),
        SECONDS to (3_600L to 3_600),
        MINUTES to (60L to 60),
        HOURS to (1L to 1),
    ).withDefault { _ -> 0L to 0 },
    DAYS to mapOf(
        NANOSECONDS to (86_400_000_000_000L to Int.MAX_VALUE),
        MICROSECONDS to (86_400_000_000L to Int.MAX_VALUE),
        MILLISECONDS to (86_400_000L to 86_400_000),
        SECONDS to (86_400L to 86_400),
        MINUTES to (1_440L to 1_440),
        HOURS to (24L to 24),
        DAYS to (1L to 1),
    )
)
