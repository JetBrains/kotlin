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
import org.jetbrains.kotlin.build.report.statistics.*
import org.jetbrains.kotlin.build.report.statistics.file.FileReportService
import org.jetbrains.kotlin.compilerRunner.JpsKotlinLogger
import java.io.File
import java.util.*
import java.net.InetAddress

interface JpsBuilderMetricReporter : BuildMetricsReporter {
    fun flush(context: CompileContext): CompileStatisticsData
}

private const val jpsBuildTaskName = "JPS build"

class JpsBuilderMetricReporterImpl(private val reporter: BuildMetricsReporterImpl) : JpsBuilderMetricReporter, BuildMetricsReporter by reporter {

    companion object {
        private val hostName: String? = try {
            InetAddress.getLocalHost().hostName
        } catch (_: Exception) {
            //do nothing
            null
        }
    }

    private val uuid = UUID.randomUUID()
    private val startTime = System.currentTimeMillis()

    override fun flush(context: CompileContext): CompileStatisticsData {
        val buildMetrics = reporter.getMetrics()
        return CompileStatisticsData(
            projectName = context.projectDescriptor.project.name,
            label = "JPS build", //TODO will be updated in KT-58026
            taskName = jpsBuildTaskName,
            taskResult = "Unknown",//TODO will be updated in KT-58026
            startTimeMs = startTime,
            durationMs = System.currentTimeMillis() - startTime,
            tags = emptySet(),
            buildUuid = uuid.toString(),
            changes = emptyList(), //TODO will be updated in KT-58026
            kotlinVersion = "kotlin_version", //TODO will be updated in KT-58026
            hostName = hostName,
            finishTime = System.currentTimeMillis(),
            buildTimesMetrics = buildMetrics.buildTimes.asMapMs(),
            performanceMetrics = buildMetrics.buildPerformanceMetrics.asMap(),
            compilerArguments = emptyList(), //TODO will be updated in KT-58026
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
class JpsStatisticsReportService {

    private val fileReportSettings: FileReportSettings? = initFileReportSettings()
    private val httpReportSettings: HttpReportSettings? = initHttpReportSettings()

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
    private val loggerAdapter = JpsKotlinLogger(log)
    private val httpService = httpReportSettings?.let { HttpReportService(it.url, it.user, it.password) }
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

        val compileStatisticsData = metrics.flush(context)
        httpService?.sendData(compileStatisticsData, loggerAdapter)
        fileReportSettings?.also {
            FileReportService(it.buildReportDir, true, loggerAdapter)
                .process(listOf(compileStatisticsData),
                         BuildStartParameters(tasks = listOf(jpsBuildTaskName)))
        }
    }
}



