/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics

import org.jetbrains.kotlin.statistics.fileloggers.MetricsContainer
import org.jetbrains.kotlin.statistics.metrics.ConcatMetricContainer
import org.jetbrains.kotlin.statistics.metrics.NumberAnonymizationPolicy.RANDOM_10_PERCENT
import org.jetbrains.kotlin.statistics.metrics.StringAnonymizationPolicy
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


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

    @Test
    fun versionAnonymization() {
        assertEquals("0.0.0", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("some.invalid.string"))
        assertEquals("1.0.0", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("1"))
        assertEquals("1.2.0", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("1.2"))

        assertEquals("1.2.3", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("1.2.3"))
        assertEquals("1.2.3", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("1.2.3"))
        assertEquals("1.2.3", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("1.2.3.4"))
        assertEquals("1.2.3-m", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("1.2.3-M"))
        assertEquals("1.2.3-m1", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("1.2.3-M1"))
        assertEquals("1.2.3-m2", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("1.2.3.M2"))
        assertEquals("1.2.3-rc", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("1.2.3-RC"))
        assertEquals("1.2.3-rc5", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("1.2.3-RC5"))

        assertEquals("1.2.3", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("1.2.3.unknown suffix"))
        assertEquals("1.2.3", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("1.2.3-unknown suffix"))

        assertEquals("123.234.345-dev", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("123.234.345-dev-12345"))
        assertEquals("1.9.255-snapshot", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("1.9.255-SNAPSHOT"))

        assertEquals("1.9.255-beta", StringAnonymizationPolicy.ComponentVersionAnonymizer().anonymize("1.9.255-beta"))
    }

    @Test
    fun versionStringValidation() {
        val separator = ConcatMetricContainer.SEPARATOR
        val container = MetricsContainer()
        fun whenAdded(newValue: String, expected: String) {
            container.report(StringMetrics.MPP_PLATFORMS, newValue)
            val currentValue = container.getMetric(StringMetrics.MPP_PLATFORMS)!!.toStringRepresentation()
            assertEquals(expected, currentValue)
            val regex = StringMetrics.MPP_PLATFORMS.anonymization.validationRegexp()
            assertTrue(currentValue.matches(Regex(regex)), "'${currentValue}' should match '${regex}'")
        }
        whenAdded("js", "js")
        whenAdded("common", "common${separator}js")
        whenAdded("jvm", "common${separator}js${separator}jvm")
        whenAdded("AAA", "UNEXPECTED-VALUE${separator}common${separator}js${separator}jvm")
        whenAdded("BBB", "UNEXPECTED-VALUE${separator}common${separator}js${separator}jvm")
    }
}
