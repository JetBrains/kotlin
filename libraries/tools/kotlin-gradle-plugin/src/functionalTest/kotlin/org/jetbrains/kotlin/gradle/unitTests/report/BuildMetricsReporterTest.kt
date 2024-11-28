/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.report

import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporterImpl
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.internal.compilerRunner.native.parseCompilerMetricsFromFile
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains

class BuildMetricsReporterTest {
    @Test
    fun parseNativePerformanceLog() {
        val file = File("src/functionalTest/resources/nativePerformance.log")
        println(file.absolutePath)
        val metricsReporter = BuildMetricsReporterImpl<GradleBuildTime, GradleBuildPerformanceMetric>()
        metricsReporter.parseCompilerMetricsFromFile(file)
        val buildTimeKeys = metricsReporter.getMetrics().buildTimes.asMapMs().keys
        assertContains(buildTimeKeys, GradleBuildTime.COMPILER_INITIALIZATION)
        assertContains(buildTimeKeys, GradleBuildTime.CODE_ANALYSIS)
        assertContains(buildTimeKeys, GradleBuildTime.BACKEND)
        assertContains(buildTimeKeys, GradleBuildTime.IR_LOWERING)
        assertContains(buildTimeKeys, GradleBuildTime.TRANSLATION_TO_IR)

        val buildPerformanceKeys = metricsReporter.getMetrics().buildPerformanceMetrics.asMap().keys
        assertContains(buildPerformanceKeys, GradleBuildPerformanceMetric.ANALYSIS_LPS)
        assertContains(buildPerformanceKeys, GradleBuildPerformanceMetric.TRANSLATION_TO_IR_LPS)
        assertContains(buildPerformanceKeys, GradleBuildPerformanceMetric.IR_LOWERING_LPS)
        assertContains(buildPerformanceKeys, GradleBuildPerformanceMetric.BACKEND_LPS)
    }
}
