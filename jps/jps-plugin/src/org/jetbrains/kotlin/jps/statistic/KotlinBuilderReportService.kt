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
import org.jetbrains.kotlin.build.report.statistic.BuildDataType
import org.jetbrains.kotlin.build.report.statistic.CompileStatisticsData
import org.jetbrains.kotlin.build.report.statistic.HttpReportServiceImpl
import org.jetbrains.kotlin.build.report.statistic.file.FileReportService
import java.io.File
import java.util.*
import java.net.InetAddress

interface JpsBuilderMetricReporter : BuildMetricsReporter {
    fun flush(context: CompileContext): CompileStatisticsData
}

//single thread execution
class JpsBuilderMetricReporterImpl(private val reporter: BuildMetricsReporterImpl) : JpsBuilderMetricReporter, BuildMetricsReporter by reporter {

    companion object {
        private val log = Logger.getInstance("#org.jetbrains.kotlin.jps.statistic.KotlinBuilderMetricImpl")
        private val hostName: String? = try {
            InetAddress.getLocalHost().hostName
        } catch (_: Exception) {
            //do nothing
            null
        }
    }

    private val uuid = UUID.randomUUID()
    private val startTime = System.currentTimeMillis()

    override fun flush(context: CompileContext/*, listener: BuildListener*/): CompileStatisticsData {
        val buildMetrics = reporter.getMetrics()
        return CompileStatisticsData(
            projectName = context.projectDescriptor.project.name,
            label = "JPS build", //TODO
            taskName = "JPS build",
            taskResult = "Unknown",//TODO
            startTimeMs = startTime,
            durationMs = System.currentTimeMillis() - startTime,
            tags = emptySet(),
            buildUuid = uuid.toString(),
            changes = emptyList(), //TODO
            kotlinVersion = "kotlin_version", //TODO
            hostName = hostName,
            finishTime = System.currentTimeMillis(),
            buildTimesMetrics = buildMetrics.buildTimes.asMapMs(),
            performanceMetrics = buildMetrics.buildPerformanceMetrics.asMap(),
            compilerArguments = emptyList(), //TODO
            nonIncrementalAttributes = emptySet(),
            type = BuildDataType.JPS_DATA.name,
            fromKotlinPlugin = true,
            compiledSources = emptyList(),
            skipMessage = null,
            icLogLines = emptyList(),
            gcTimeMetrics = buildMetrics.gcMetrics.asGcTimeMap(),
            gcCountMetrics = buildMetrics.gcMetrics.asGcCountMap(),
            kotlinLanguageVersion = null
        )
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
        contextMetrics[context] = JpsBuilderMetricReporterImpl(BuildMetricsReporterImpl())
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



