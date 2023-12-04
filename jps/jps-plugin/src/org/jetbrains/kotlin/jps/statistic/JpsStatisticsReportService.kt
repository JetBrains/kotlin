/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.statistic

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.GlobalContextKey
import org.jetbrains.jps.incremental.ModuleLevelBuilder
import org.jetbrains.jps.incremental.ModuleLevelBuilder.ExitCode
import org.jetbrains.kotlin.build.report.FileReportSettings
import org.jetbrains.kotlin.build.report.HttpReportSettings
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.statistics.*
import org.jetbrains.kotlin.compilerRunner.JpsKotlinLogger
import org.jetbrains.kotlin.jps.build.KotlinChunk
import org.jetbrains.kotlin.jps.build.KotlinDirtySourceFilesHolder
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

private const val jpsBuildTaskName = "JPS build"


internal val statisticsReportServiceKey = GlobalContextKey<JpsStatisticsReportService>("jpsStatistics")

sealed class JpsStatisticsReportService {
    companion object {
        private const val DEFAULT_CHANGED_FILE_LIST_LIMIT = 20

        internal fun create(): JpsStatisticsReportService {
            val fileReportSettings = initFileReportSettings()
            val httpReportSettings = initHttpReportSettings()

            return if (fileReportSettings == null && httpReportSettings == null) {
                DummyJpsStatisticsReportService
            } else {
                JpsStatisticsReportServiceImpl(fileReportSettings, httpReportSettings)
            }
        }

        internal fun getFromContext(context: CompileContext): JpsStatisticsReportService =
            context.getUserData(statisticsReportServiceKey) ?: DummyJpsStatisticsReportService

        private fun initFileReportSettings(): FileReportSettings? {
            return System.getProperty("kotlin.build.report.file.output_dir")?.let {
                FileReportSettings(File(it), System.getProperty("kotlin.build.report.file.change_file_limit")?.toInt() ?: DEFAULT_CHANGED_FILE_LIST_LIMIT)
            }
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

    abstract fun moduleBuildFinished(chunk: ModuleChunk, context: CompileContext, exitCode: ModuleLevelBuilder.ExitCode)
    abstract fun moduleBuildStarted(chunk: ModuleChunk)

    abstract fun reportDirtyFiles(kotlinDirtySourceFilesHolder: KotlinDirtySourceFilesHolder)
    abstract fun reportCompilerArguments(chunk: ModuleChunk, kotlinChunk: KotlinChunk)
    abstract fun getMetricReporter(chunk: ModuleChunk): JpsBuilderMetricReporter?
}


object DummyJpsStatisticsReportService : JpsStatisticsReportService() {
    override fun <T> reportMetrics(chunk: ModuleChunk, metric: JpsBuildTime, action: () -> T): T {
        return action()
    }

    override fun buildStarted(context: CompileContext) {}
    override fun buildFinish(context: CompileContext) {}
    override fun moduleBuildFinished(chunk: ModuleChunk, context: CompileContext, exitCode: ExitCode) {}
    override fun moduleBuildStarted(chunk: ModuleChunk) {}

    override fun reportDirtyFiles(kotlinDirtySourceFilesHolder: KotlinDirtySourceFilesHolder) {}
    override fun reportCompilerArguments(chunk: ModuleChunk, kotlinChunk: KotlinChunk) {}
    override fun getMetricReporter(chunk: ModuleChunk): JpsBuilderMetricReporter? = null

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

    override fun getMetricReporter(chunk: ModuleChunk): JpsBuilderMetricReporter? {
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

    override fun moduleBuildFinished(chunk: ModuleChunk, context: CompileContext, exitCode: ExitCode) {
        val moduleName = chunk.name
        val metrics = buildMetrics.remove(moduleName)
        if (metrics == null) {
            log.warn("Service hasn't initialized for $moduleName module")
            return
        }
        log.debug("JpsStatisticsReportService: Build started for $moduleName module")
        metrics.buildFinish(chunk, context, exitCode.name)
        finishedModuleBuildMetrics.add(metrics)
    }

    override fun buildFinish(context: CompileContext) {
        val compileStatisticsData = finishedModuleBuildMetrics.map { it.flush(context) }
        httpService?.sendData(compileStatisticsData, loggerAdapter)
        fileReportSettings?.also {
            JpsFileReportService(
                it.buildReportDir, context.projectDescriptor.project.name, true, loggerAdapter, it.changedFileListPerLimit
            ).process(
                compileStatisticsData,
                BuildStartParameters(tasks = listOf(jpsBuildTaskName)), emptyList(),
            )
        }
    }

    override fun <T> reportMetrics(chunk: ModuleChunk, metric: JpsBuildTime, action: () -> T): T {
        return getMetricReporter(chunk)?.measure(metric, action) ?: action.invoke()
    }

    override fun reportDirtyFiles(kotlinDirtySourceFilesHolder: KotlinDirtySourceFilesHolder) {
        getMetricReporter(kotlinDirtySourceFilesHolder.chunk)?.let {
            it.addChangedFiles(kotlinDirtySourceFilesHolder.allRemovedFilesFiles.map { it.path })
            it.addChangedFiles(kotlinDirtySourceFilesHolder.allDirtyFiles.map { it.path })
        }
    }

    override fun reportCompilerArguments(chunk: ModuleChunk, kotlinChunk: KotlinChunk) {
        getMetricReporter(chunk)?.let {
            it.addCompilerArguments(kotlinChunk.compilerArguments.freeArgs)
            it.setKotlinLanguageVersion(kotlinChunk.compilerArguments.languageVersion)
        }
    }

    override fun buildStarted(context: CompileContext) {
        loggerAdapter.info("Build started for $context with enabled build metric reports.")
    }

}