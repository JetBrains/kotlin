/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.statistic

import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporterImpl
import org.jetbrains.kotlin.build.report.metrics.JpsBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.JpsBuildTime
import org.jetbrains.kotlin.build.report.statistics.BuildDataType
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.compilerRunner.JpsCompilationResult
import java.net.InetAddress
import java.util.*
import kotlin.collections.ArrayList

class JpsBuilderMetricReporterImpl(
    chunk: ModuleChunk,
    private val reporter: BuildMetricsReporterImpl<JpsBuildTime, JpsBuildPerformanceMetric>,
    private val label: String? = null,
    private val kotlinVersion: String = "kotlin_version"
) :
    JpsBuilderMetricReporter, BuildMetricsReporter<JpsBuildTime, JpsBuildPerformanceMetric> by reporter {

    companion object {
        private val hostName: String? = try {
            InetAddress.getLocalHost().hostName
        } catch (_: Exception) {
            //do nothing
            null
        }
        private val uuid = UUID.randomUUID()
    }


    private val startTime = System.currentTimeMillis()
    private var finishTime: Long = 0L
    private val tags = HashSet<StatTag>()
    private val changedFiles = ArrayList<String>()
    private val compilerArguments = ArrayList<String>()
    private val moduleString = chunk.name
    private var exitCode = "Unknown"
    private var kotlinLanguageVersion: String? = null

    override fun buildFinish(moduleChunk: ModuleChunk, context: CompileContext, exitCode: String) {
        finishTime = System.currentTimeMillis()
        this.exitCode = exitCode
    }

    override fun addChangedFiles(files: List<String>) {
        changedFiles.addAll(files)
    }

    override fun addCompilerArguments(arguments: List<String>) {
        compilerArguments.addAll(arguments)
    }

    override fun setKotlinLanguageVersion(languageVersion: String?) {
        kotlinLanguageVersion = languageVersion
    }

    override fun addTag(tag: StatTag) {
        tags.add(tag)
    }

    override fun addCompilerMetrics(jpsCompilationResult: JpsCompilationResult) {
        reporter.addMetrics(jpsCompilationResult.buildMetrics)
    }

    override fun flush(context: CompileContext): JpsCompileStatisticsData {
        val buildMetrics = reporter.getMetrics()
        return JpsCompileStatisticsData(
            projectName = context.projectDescriptor.project.name,
            label = label,
            taskName = moduleString,
            taskResult = exitCode,
            startTimeMs = startTime,
            durationMs = finishTime - startTime,
            tags = tags,
            buildUuid = uuid.toString(),
            changes = changedFiles,
            kotlinVersion = kotlinVersion,
            hostName = hostName,
            finishTime = finishTime,
            buildTimesMetrics = buildMetrics.buildTimes.asMapMs(),
            performanceMetrics = buildMetrics.buildPerformanceMetrics.asMap(),
            compilerArguments = compilerArguments,
            nonIncrementalAttributes = emptySet(),
            type = BuildDataType.JPS_DATA.name,
            fromKotlinPlugin = true,
            compiledSources = emptyList(),
            skipMessage = null,
            icLogLines = emptyList(),
            gcTimeMetrics = buildMetrics.gcMetrics.asGcTimeMap(),
            gcCountMetrics = buildMetrics.gcMetrics.asGcCountMap(),
            kotlinLanguageVersion = kotlinLanguageVersion
        )
    }

}
