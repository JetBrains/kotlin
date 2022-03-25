/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.jetbrains.kotlin.statistics.metrics.NumberAnonymizationPolicy.RANDOM_10_PERCENT
import kotlin.test.Test
import kotlin.test.assertEquals


class MetricPolicyTest {

    @Test
    fun numberAnonimization() {
        assertEquals(-200, RANDOM_10_PERCENT.anonymize(-234))
        assertEquals(-120, RANDOM_10_PERCENT.anonymize(-123))
        assertEquals(-9, RANDOM_10_PERCENT.anonymize(-9))
        assertEquals(0, RANDOM_10_PERCENT.anonymize(0))
        assertEquals(1, RANDOM_10_PERCENT.anonymize(1))
        assertEquals(5, RANDOM_10_PERCENT.anonymize(5))
        assertEquals(9, RANDOM_10_PERCENT.anonymize(9))
        assertEquals(10, RANDOM_10_PERCENT.anonymize(10))
        assertEquals(19, RANDOM_10_PERCENT.anonymize(19))
        assertEquals(20, RANDOM_10_PERCENT.anonymize(29))
        assertEquals(120, RANDOM_10_PERCENT.anonymize(123))
        assertEquals(200, RANDOM_10_PERCENT.anonymize(234))
    }
}