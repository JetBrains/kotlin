/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.statistic

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleLevelBuilder
import org.jetbrains.kotlin.build.report.FileReportSettings
import org.jetbrains.kotlin.build.report.HttpReportSettings
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.statistics.*
import org.jetbrains.kotlin.compilerRunner.JpsCompilationResult
import org.jetbrains.kotlin.compilerRunner.JpsKotlinLogger
import org.jetbrains.kotlin.jps.build.KotlinChunk
import org.jetbrains.kotlin.jps.build.KotlinDirtySourceFilesHolder
import java.io.File
import java.net.InetAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList

interface JpsBuilderMetricReporter : BuildMetricsReporter<JpsBuildTime, JpsBuildPerformanceMetric> {
    fun flush(context: CompileContext): JpsCompileStatisticsData

    fun buildFinish(moduleChunk: ModuleChunk, context: CompileContext)

    fun addChangedFiles(files: List<String>)

    fun addCompilerArguments(arguments: List<String>)

    fun setKotlinLanguageVersion(languageVersion: String?)

    fun setExitCode(code: String)

    fun addTag(tag: StatTag)

    fun addCompilerMetrics(jpsCompilationResult: JpsCompilationResult)
}

private const val jpsBuildTaskName = "JPS build"

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

    override fun buildFinish(moduleChunk: ModuleChunk, context: CompileContext) {
        finishTime = System.currentTimeMillis()
    }

    override fun addChangedFiles(files: List<String>) {
        changedFiles.addAll(files)
    }

    override fun addCompilerArguments(arguments: List<String>) {
        compilerArguments.addAll(arguments)
    }

    override fun setExitCode(code: String) {
        exitCode = code
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

    private val buildMetrics = ConcurrentHashMap<String, JpsBuilderMetricReporter>()
    private val finishedModuleBuildMetrics = ConcurrentLinkedQueue<JpsBuilderMetricReporter>()
    private val log = Logger.getInstance("#org.jetbrains.kotlin.jps.statistic.KotlinBuilderReportService")
    private val loggerAdapter = JpsKotlinLogger(log)
    private val httpService = httpReportSettings?.let { HttpReportService(it.url, it.user, it.password) }

    fun moduleBuildStarted(chunk: ModuleChunk) {
        val moduleName = chunk.name
        val jpsReporter = JpsBuilderMetricReporterImpl(chunk, BuildMetricsReporterImpl())
        if (buildMetrics.putIfAbsent(moduleName, jpsReporter) != jpsReporter) {
            log.warn("Service already initialized for context")
            return
        }
        log.info("JpsStatisticsReportService: Service started")
    }

    internal fun getMetricReporter(chunk: ModuleChunk): JpsBuilderMetricReporter? {
        val moduleName = chunk.name
        return getMetricReporter(moduleName)
    }
    private fun getMetricReporter(moduleName: String): JpsBuilderMetricReporter? {
        val metricReporter = buildMetrics[moduleName]
        if (metricReporter == null) {
            //At some point log should be changed to exception
            log.warn("Service hasn't initialized for context")
            return null
        }
        return metricReporter
    }

    fun moduleBuildFinished(chunk: ModuleChunk, context: CompileContext) {
        val moduleName = chunk.name
        val metrics = buildMetrics.remove(moduleName)
        if (metrics == null) {
            log.warn("Service hasn't initialized for context")
            return
        }
        log.info("JpsStatisticsReportService: Service finished")
        metrics.buildFinish(chunk, context)
        finishedModuleBuildMetrics.add(metrics)
    }

    fun buildFinish(context: CompileContext) {
        val compileStatisticsData = finishedModuleBuildMetrics.map { it.flush(context) }
        httpService?.sendData(compileStatisticsData, loggerAdapter)
        fileReportSettings?.also {
            JpsFileReportService(
                it.buildReportDir, context.projectDescriptor.project.name, true, loggerAdapter
            ).process(
                compileStatisticsData,
                BuildStartParameters(tasks = listOf(jpsBuildTaskName)), emptyList(),
            )
        }
    }

    fun <T> reportMetrics(chunk: ModuleChunk, metric: JpsBuildTime, action: () -> T): T {
        return getMetricReporter(chunk)?.let {
            log.info("JpsStatisticsReportService: report metrics")
            it.measure(metric, action)
        } ?: action.invoke()
    }

    fun reportDirtyFiles(kotlinDirtySourceFilesHolder: KotlinDirtySourceFilesHolder) {
        getMetricReporter(kotlinDirtySourceFilesHolder.chunk)?.let {
            it.addChangedFiles(kotlinDirtySourceFilesHolder.allRemovedFilesFiles.map { it.path })
            it.addChangedFiles(kotlinDirtySourceFilesHolder.allDirtyFiles.map { it.path })
        }
    }

    fun reportCompilerArguments(chunk: ModuleChunk, kotlinChunk: KotlinChunk) {
        getMetricReporter(chunk)?.let {
            it.addCompilerArguments(kotlinChunk.compilerArguments.freeArgs)
            it.setKotlinLanguageVersion(kotlinChunk.compilerArguments.languageVersion)
        }
    }

    fun reportExitCode(chunk: ModuleChunk, actualExitCode: ModuleLevelBuilder.ExitCode) {
        getMetricReporter(chunk)?.let {
            it.setExitCode(actualExitCode.name)
        }
    }

    fun buildStarted(context: CompileContext) {
        loggerAdapter.info("Build started for $context")
    }

}



