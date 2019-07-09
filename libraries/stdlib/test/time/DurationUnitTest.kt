/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:UseExperimental(ExperimentalTime::class)
package test.time

import kotlin.test.*
import kotlin.time.*
// TODO: Use star import after KT-30983 is fixed
import kotlin.time.DurationUnit.DAYS
import kotlin.time.DurationUnit.HOURS
import kotlin.time.DurationUnit.MINUTES
import kotlin.time.DurationUnit.SECONDS
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.DurationUnit.MICROSECONDS
import kotlin.time.DurationUnit.NANOSECONDS

class DurationUnitTest {

    @Test
    fun conversion() {
        fun test(sourceValue: Double, sourceUnit: DurationUnit, targetValue: Double, targetUnit: DurationUnit) {
            assertEquals(
                targetValue, Duration.convert(sourceValue, sourceUnit, targetUnit),
                "Expected $sourceValue $sourceUnit to be $targetValue $targetUnit"
            )
            assertEquals(
                sourceValue, Duration.convert(targetValue, targetUnit, sourceUnit),
                "Expected $targetValue $targetUnit to be $sourceValue $sourceUnit"
            )
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
    }



}