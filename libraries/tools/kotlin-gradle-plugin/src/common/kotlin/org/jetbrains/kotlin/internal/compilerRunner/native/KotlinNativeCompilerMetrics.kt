/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.build.report.metrics.BACKEND
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.build.report.metrics.CODE_ANALYSIS
import org.jetbrains.kotlin.build.report.metrics.COMPILER_INITIALIZATION
import org.jetbrains.kotlin.build.report.metrics.CustomBuildTimeMetric
import org.jetbrains.kotlin.build.report.metrics.IR_LINKING
import org.jetbrains.kotlin.build.report.metrics.IR_LOWERING
import org.jetbrains.kotlin.build.report.metrics.IR_PRE_LOWERING
import org.jetbrains.kotlin.build.report.metrics.IR_SERIALIZATION
import org.jetbrains.kotlin.build.report.metrics.KLIB_METADATA_WRITING
import org.jetbrains.kotlin.build.report.metrics.KLIB_WRITING
import org.jetbrains.kotlin.build.report.metrics.KlibSizeMetric
import org.jetbrains.kotlin.build.report.metrics.TRANSLATION_TO_IR
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.util.PhaseType

/**
 * The part of the compiler's performance report that goes into the build report. The Kotlin/Native compiler writes
 * the file from [org.jetbrains.kotlin.util.UnitStats] with `UnitStatsJsonDumper`; keys this class doesn't declare
 * are skipped, and a phase the compiler didn't run is absent.
 */
@Serializable
internal class CompilerMetricsJson(
    val initStats: TimeJson? = null,
    val analysisStats: TimeJson? = null,
    val translationToIrStats: TimeJson? = null,
    val irPreLoweringStats: TimeJson? = null,
    val irSerializationStats: TimeJson? = null,
    val klibWritingStats: TimeJson? = null,
    val klibMetadataWritingStats: TimeJson? = null,
    val irLinkingStats: TimeJson? = null,
    val irLoweringStats: TimeJson? = null,
    val backendStats: TimeJson? = null,
    val dynamicStats: List<DynamicStatsJson>? = null,
    val klibElementStats: List<KlibElementStatsJson>? = null,
) {
    val phases: List<Pair<PhaseType, TimeJson?>>
        get() = listOf(
            PhaseType.Initialization to initStats,
            PhaseType.Analysis to analysisStats,
            PhaseType.TranslationToIr to translationToIrStats,
            PhaseType.IrPreLowering to irPreLoweringStats,
            PhaseType.IrSerialization to irSerializationStats,
            PhaseType.KlibWriting to klibWritingStats,
            PhaseType.KlibMetadataWriting to klibMetadataWritingStats,
            PhaseType.IrLinking to irLinkingStats,
            PhaseType.IrLowering to irLoweringStats,
            PhaseType.Backend to backendStats,
        )

    companion object {
        /** Lenient, because the dumper writes `parentPhaseType` as a bare word. */
        private val json = Json(KgpJson.default) { isLenient = true }

        fun parse(text: String): CompilerMetricsJson = json.decodeFromString(serializer(), text)
    }
}

/** `userNanos` and `cpuNanos` are also in the file; the build report only shows wall time. */
@Serializable
internal class TimeJson(val nanos: Long)

@Serializable
internal data class DynamicStatsJson(val parentPhaseType: PhaseType, val name: String, val time: TimeJson)

@Serializable
internal data class KlibElementStatsJson(val path: String, val size: Long)

internal fun BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric>.addCompilerMetrics(metrics: CompilerMetricsJson) {
    metrics.phases.forEach { (phaseType, time) ->
        if (time != null) addTimeMetricNs(phaseType.toGradleBuildTime(), time.nanos)
    }
    metrics.dynamicStats?.forEach { (parentPhaseType, name, time) ->
        addTimeMetricNs(CustomBuildTimeMetric.createIfDoesNotExistAndReturn(name, parentPhaseType.toGradleBuildTime()), time.nanos)
    }
    metrics.klibElementStats?.forEach { (path, size) ->
        addMetric(KlibSizeMetric.createIfDoesNotExistAndReturn(path), size)
    }
}

private fun PhaseType.toGradleBuildTime() = when (this) {
    PhaseType.Initialization -> COMPILER_INITIALIZATION
    PhaseType.Analysis -> CODE_ANALYSIS
    PhaseType.TranslationToIr -> TRANSLATION_TO_IR
    PhaseType.IrPreLowering -> IR_PRE_LOWERING
    PhaseType.IrSerialization -> IR_SERIALIZATION
    PhaseType.KlibWriting -> KLIB_WRITING
    PhaseType.KlibMetadataWriting -> KLIB_METADATA_WRITING
    PhaseType.IrLinking -> IR_LINKING
    PhaseType.IrLowering -> IR_LOWERING
    PhaseType.Backend -> BACKEND
}
