/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import org.jetbrains.kotlin.build.report.metrics.BACKEND
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporterImpl
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.build.report.metrics.CODE_ANALYSIS
import org.jetbrains.kotlin.build.report.metrics.COMPILER_INITIALIZATION
import org.jetbrains.kotlin.build.report.metrics.IR_LOWERING
import org.jetbrains.kotlin.build.report.metrics.KlibSizeMetric
import org.jetbrains.kotlin.util.CompilerType
import org.jetbrains.kotlin.util.DynamicStats
import org.jetbrains.kotlin.util.KlibElementStats
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.PlatformType
import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.UnitStats
import org.jetbrains.kotlin.util.UnitStatsJsonDumper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KotlinNativeCompilerMetricsTest {

    /** What the Kotlin/Native compiler writes for `-Xdump-perf`, with the phases a klib compilation skips left out. */
    private val compilerReport = UnitStatsJsonDumper.dump(
        UnitStats(
            name = "lib",
            outputKind = "LIBRARY",
            timeStampMs = 1_700_000_000_000,
            platform = PlatformType.Native,
            compilerType = CompilerType.K2,
            hasErrors = false,
            filesCount = 3,
            linesCount = 40,
            measuredCpuAndUserTime = true,
            initStats = Time(1_000_000, 1_000_001, 1_000_002),
            analysisStats = Time(2_000_000, 2_000_001, 2_000_002),
            translationToIrStats = null,
            irPreLoweringStats = null,
            irSerializationStats = null,
            klibWritingStats = null,
            klibMetadataWritingStats = null,
            irLinkingStats = null,
            irLoweringStats = Time(3_000_000, 3_000_001, 3_000_002),
            backendStats = null,
            dynamicStats = listOf(DynamicStats(PhaseType.IrLowering, "Inline", Time(500_000, 500_001, 500_002))),
            klibElementStats = listOf(KlibElementStats("KLIB directory cumulative size", 4_096)),
        )
    )

    @Test
    fun `the phases the compiler ran are reported as wall time`() {
        val reporter = BuildMetricsReporterImpl<BuildTimeMetric, BuildPerformanceMetric>()

        reporter.addCompilerMetrics(CompilerMetricsJson.parse(compilerReport))

        val times = reporter.getMetrics().buildTimes.buildTimesMapMs()
        assertEquals(1, times[COMPILER_INITIALIZATION])
        assertEquals(2, times[CODE_ANALYSIS])
        assertNull(times[BACKEND], "a phase the compiler didn't run has no measurement")
    }

    @Test
    fun `dynamic phases nest under their parent phase`() {
        val reporter = BuildMetricsReporterImpl<BuildTimeMetric, BuildPerformanceMetric>()

        reporter.addCompilerMetrics(CompilerMetricsJson.parse(compilerReport))

        val inline = reporter.getMetrics().buildTimes.buildTimesMapMs().keys.single { it.name == "Inline" }
        assertEquals(IR_LOWERING, inline.parent)
    }

    @Test
    fun `klib sizes are reported in bytes`() {
        val reporter = BuildMetricsReporterImpl<BuildTimeMetric, BuildPerformanceMetric>()

        reporter.addCompilerMetrics(CompilerMetricsJson.parse(compilerReport))

        val sizes = reporter.getMetrics().buildPerformanceMetrics.asMap()
        assertEquals(4_096, sizes[KlibSizeMetric.createIfDoesNotExistAndReturn("KLIB directory cumulative size")])
    }

    @Test
    fun `every phase key matches what the compiler writes`() {
        // the DTO names its keys by hand, so a UnitStats rename would otherwise read as "phase not run"
        val allPhases = UnitStatsJsonDumper.dump(
            UnitStats(
                name = "lib",
                outputKind = "LIBRARY",
                filesCount = 1,
                linesCount = 1,
                measuredCpuAndUserTime = false,
                initStats = Time(1, 0, 0),
                analysisStats = Time(2, 0, 0),
                translationToIrStats = Time(3, 0, 0),
                irPreLoweringStats = Time(4, 0, 0),
                irSerializationStats = Time(5, 0, 0),
                klibWritingStats = Time(6, 0, 0),
                klibMetadataWritingStats = Time(7, 0, 0),
                irLinkingStats = Time(8, 0, 0),
                irLoweringStats = Time(9, 0, 0),
                backendStats = Time(10, 0, 0),
            )
        )

        val metrics = CompilerMetricsJson.parse(allPhases)

        assertEquals(PhaseType.entries, metrics.phases.map { it.first })
        assertEquals((1L..10L).toList(), metrics.phases.map { it.second?.nanos })
    }

    @Test
    fun `keys the build report doesn't use are skipped`() {
        val metrics = CompilerMetricsJson.parse(compilerReport)

        assertEquals(2_000_000, metrics.analysisStats?.nanos)
        assertNull(metrics.backendStats)
    }
}
