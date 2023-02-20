/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.statistic

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.kotlin.build.report.FileReportSettings
import org.jetbrains.kotlin.build.report.HttpReportSettings
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.statistic.HttpReportServiceImpl
import org.jetbrains.kotlin.build.report.statistic.file.FileReportService
import org.jetbrains.kotlin.gradle.plugin.stat.BuildDataType
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatisticsData
import java.io.File
import java.io.Serializable
import java.util.*
import java.util.concurrent.TimeUnit

interface JpsBuilderMetricReporter : BuildMetricsReporter {
    fun flush(context: CompileContext): CompileStatisticsData
}

//single thread execution
class JpsBuilderMetricReporterImpl : JpsBuilderMetricReporter {
    companion object {
        private val log = Logger.getInstance("#org.jetbrains.kotlin.jps.statistic.KotlinBuilderMetricImpl")
    }

    private val buildTimes = EnumMap<BuildTime, Long>(BuildTime::class.java)
    private val buildMetricsInProcess = EnumMap<BuildTime, Long>(BuildTime::class.java)
    private val uuid = UUID.randomUUID()
    private val startTime = System.currentTimeMillis()

    override fun startMeasure(time: BuildTime) {
        if (buildMetricsInProcess[time] != null) {
            log.error("$time is already in process")
        } else {
            buildMetricsInProcess[time] = System.nanoTime()
        }
    }

    override fun endMeasure(time: BuildTime) {
        val value = buildMetricsInProcess.remove(time)
        if (value == null) {
            log.error("$time hasn't started")
        } else {
            buildTimes[time] = TimeUnit.NANOSECONDS.toMillis(value)
        }
    }

    override fun addTimeMetricNs(time: BuildTime, durationNs: Long) {
        buildTimes[time] = durationNs
    }

    override fun flush(context: CompileContext/*, listener: BuildListener*/): CompileStatisticsData {
        if (buildMetricsInProcess.isNotEmpty()) {
            log.error("Finish metric calculation, but ${buildMetricsInProcess.keys} metrics are in progress")
        }
        return CompileStatisticsData(
            version = 99, //TODO
            projectName = context.projectDescriptor.project.name,
            label = "JPS build", //TODO
            taskName = "JPS build", //TODO
            taskResult = "Unknown",
            startTimeMs = startTime,
            durationMs = System.currentTimeMillis() - startTime,
            tags = emptyList(), //TODO
            buildUuid = uuid.toString(),
            changes = emptyList(), //TODO
            kotlinVersion = "kotlin_version", //TODO
            hostName = "test", //TODO
            finishTime = System.currentTimeMillis(),
            buildTimesMetrics = buildTimes,
            performanceMetrics = emptyMap(),
            compilerArguments = emptyList(), //TODO
            nonIncrementalAttributes = emptySet(),
            type = BuildDataType.JPS_DATA.name,
            fromKotlinPlugin = true,
            compiledSources = emptyList(),
            skipMessage = null,
            icLogLines = emptyList(),
            gcTimeMetrics = null,//TODO
            gcCountMetrics = null,//TODO
        )

    }

    override fun addMetric(metric: BuildPerformanceMetric, value: Long) {
        TODO("Not yet implemented")
    }

    override fun addTimeMetric(metric: BuildPerformanceMetric) {
        TODO("Not yet implemented")
    }

    override fun addGcMetric(metric: String, value: GcMetric) {
        TODO("Not yet implemented")
    }

    override fun startGcMetric(name: String, value: GcMetric) {
        TODO("Not yet implemented")
    }

    override fun endGcMetric(name: String, value: GcMetric) {
        TODO("Not yet implemented")
    }

    override fun addAttribute(attribute: BuildAttribute) {
        TODO("Not yet implemented")
    }

    override fun getMetrics(): BuildMetrics {
        TODO("Not yet implemented")
    }

    override fun addMetrics(metrics: BuildMetrics) {
        TODO("Not yet implemented")
    }
}

// TODO test UserDataHolder in CompileContext to store CompileStatisticsData.Build or KotlinBuilderMetric
class KotlinBuilderReportService(
    private val fileReportSettings: FileReportSettings?,
    private val httpReportSettings: HttpReportSettings?
) {
    constructor() : this(
        initFileReportSettings(),
        initHttpReportSettings(),
    )

    companion object {
        private fun initFileReportSettings(): FileReportSettings? {
            return System.getProperty("kotlin.build.report.file.output_dir")?.let { FileReportSettings(File(it)) }
        }

        private fun initHttpReportSettings(): HttpReportSettings? {
            val httpReportUrl = System.getProperty("kotlin.build.report.http.url") ?: return null
            val httpReportUser = System.getProperty("kotlin.build.report.http.user")
            val httpReportPassword = System.getProperty("kotlin.build.report.http.password")
            val includeGitBranch = System.getProperty("kotlin.build.report.http.git_branch", "false").toBoolean()
            val verboseEnvironment = System.getProperty("kotlin.build.report.http.environment.verbose", "false").toBoolean()
            return HttpReportSettings(httpReportUrl, httpReportUser, httpReportPassword, verboseEnvironment, includeGitBranch)
        }
    }

    private val contextMetrics = HashMap<CompileContext, JpsBuilderMetricReporter>()
    private val log = Logger.getInstance("#org.jetbrains.kotlin.jps.statistic.KotlinBuilderReportService")
    private val loggerAdapter = JpsLoggerAdapter(log)
    private val httpService = httpReportSettings?.let { HttpReportServiceImpl(it.url, it.user, it.password) }
    fun buildStarted(context: CompileContext) {
        if (contextMetrics[context] != null) {
            log.error("Service already initialized for context")
        }
        contextMetrics[context] = JpsBuilderMetricReporterImpl()
    }

    fun buildFinished(context: CompileContext) {
        val metrics = contextMetrics.remove(context)
        if (metrics == null) {
            log.error("Service hasn't initialized for context")
            return
        }

        httpService?.sendData(metrics.flush(context), loggerAdapter)
        fileReportSettings?.also { FileReportService(it.buildReportDir, true, loggerAdapter) }
    }

    fun addMetric(context: CompileContext, metric: BuildTime, value: Long) {
        val metrics = contextMetrics[context]
        if (metrics == null) {
            log.error("Service hasn't initialized for context")
            return
        }
        metrics.addTimeMetricNs(metric, value)
    }
}



