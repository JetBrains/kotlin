/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.statistic

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.kotlin.build.report.FileReportSettings
import org.jetbrains.kotlin.build.report.HttpReportSettings
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.statistics.*
import org.jetbrains.kotlin.build.report.statistics.file.ReadableFileReportData
import org.jetbrains.kotlin.compilerRunner.JpsKotlinLogger
import java.io.File
import java.net.InetAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

interface JpsBuilderMetricReporter : BuildMetricsReporter<JpsBuildTime, JpsBuildPerformanceMetric> {
    fun flush(context: CompileContext): JpsCompileStatisticsData

    fun buildFinish(moduleChunk: ModuleChunk, context: CompileContext)
}

private const val jpsBuildTaskName = "JPS build"

class JpsBuilderMetricReporterImpl(
    chunk: ModuleChunk,
    private val reporter: BuildMetricsReporterImpl<JpsBuildTime, JpsBuildPerformanceMetric>,
    private val label: String? = null,
    private val kotlinVersion: String = "kotlin_version",
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
    private val moduleString = chunk.name

    override fun buildFinish(moduleChunk: ModuleChunk, context: CompileContext) {
        finishTime = System.currentTimeMillis()
    }

    override fun flush(context: CompileContext): JpsCompileStatisticsData {
        val buildMetrics = reporter.getMetrics()
        return JpsCompileStatisticsData(
            projectName = context.projectDescriptor.project.name,
            label = label,
            taskName = moduleString,
            taskResult = "Unknown",//TODO will be updated in KT-58026
            startTimeMs = startTime,
            durationMs = finishTime - startTime,
            tags = tags,
            buildUuid = uuid.toString(),
            changes = emptyList(), //TODO will be updated in KT-58026
            kotlinVersion = kotlinVersion,
            hostName = hostName,
            finishTime = finishTime,
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

sealed class JpsStatisticsReportService {
    companion object {
        fun create(): JpsStatisticsReportService {
            val fileReportSettings = initFileReportSettings()
            val httpReportSettings = initHttpReportSettings()

            return if (fileReportSettings == null && httpReportSettings == null) {
                DummyJpsStatisticsReportService
            } else {
                JpsStatisticsReportServiceImpl(fileReportSettings, httpReportSettings)
            }

        }

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

    abstract fun <T> reportMetrics(chunk: ModuleChunk, metric: JpsBuildTime, action: () -> T): T
    abstract fun buildStarted(context: CompileContext)
    abstract fun buildFinish(context: CompileContext)

    abstract fun moduleBuildFinished(chunk: ModuleChunk, context: CompileContext)
    abstract fun moduleBuildStarted(chunk: ModuleChunk)
}


object DummyJpsStatisticsReportService : JpsStatisticsReportService() {
    override fun <T> reportMetrics(chunk: ModuleChunk, metric: JpsBuildTime, action: () -> T): T {
        return action()
    }

    override fun buildStarted(context: CompileContext) {}
    override fun buildFinish(context: CompileContext) {}
    override fun moduleBuildFinished(chunk: ModuleChunk, context: CompileContext) {}
    override fun moduleBuildStarted(chunk: ModuleChunk) {}

}

class JpsStatisticsReportServiceImpl(
    private val fileReportSettings: FileReportSettings?,
    httpReportSettings: HttpReportSettings?,
) : JpsStatisticsReportService() {

    private val buildMetrics = ConcurrentHashMap<String, JpsBuilderMetricReporter>()
    private val finishedModuleBuildMetrics = ConcurrentLinkedQueue<JpsBuilderMetricReporter>()
    private val log = Logger.getInstance("#org.jetbrains.kotlin.jps.statistic.KotlinBuilderReportService")
    private val loggerAdapter = JpsKotlinLogger(log)
    private val httpService = httpReportSettings?.let { HttpReportService(it.url, it.user, it.password) }

    override fun moduleBuildStarted(chunk: ModuleChunk) {
        val moduleName = chunk.name
        val jpsReporter = JpsBuilderMetricReporterImpl(chunk, BuildMetricsReporterImpl())
        if (buildMetrics.putIfAbsent(moduleName, jpsReporter) != jpsReporter) {
            log.warn("Service already initialized for $moduleName module")
            return
        }
        log.debug("JpsStatisticsReportService: Build started for $moduleName module")
    }

    private fun getMetricReporter(chunk: ModuleChunk): JpsBuilderMetricReporter? {
        val moduleName = chunk.name
        return getMetricReporter(moduleName)
    }

    private fun getMetricReporter(moduleName: String): JpsBuilderMetricReporter? {
        val metricReporter = buildMetrics[moduleName]
        if (metricReporter == null) {
            //At some point log should be changed to exception
            log.warn("Service hasn't initialized for $moduleName module")
            return null
        }
        return metricReporter
    }

    override fun moduleBuildFinished(chunk: ModuleChunk, context: CompileContext) {
        val moduleName = chunk.name
        val metrics = buildMetrics.remove(moduleName)
        if (metrics == null) {
            log.warn("Service hasn't initialized for $moduleName module")
            return
        }
        log.debug("JpsStatisticsReportService: Build started for $moduleName module")
        metrics.buildFinish(chunk, context)
        finishedModuleBuildMetrics.add(metrics)
    }

    override fun buildFinish(context: CompileContext) {
        val compileStatisticsData = finishedModuleBuildMetrics.map { it.flush(context) }
        httpService?.sendData(compileStatisticsData, loggerAdapter)
        fileReportSettings?.also {
            JpsFileReportService(
                it.buildReportDir, context.projectDescriptor.project.name, true
            ).process(
                ReadableFileReportData(
                    compileStatisticsData,
                    BuildStartParameters(tasks = listOf(jpsBuildTaskName)), emptyList()
                ),
                loggerAdapter
            )
        }
    }

    override fun <T> reportMetrics(chunk: ModuleChunk, metric: JpsBuildTime, action: () -> T): T {
        return getMetricReporter(chunk)?.measure(metric, action) ?: action.invoke()
    }

    override fun buildStarted(context: CompileContext) {
        loggerAdapter.info("Build started for $context with enabled build metric reports.")
    }

}