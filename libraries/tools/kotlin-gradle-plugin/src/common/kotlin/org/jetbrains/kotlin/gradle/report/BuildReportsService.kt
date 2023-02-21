/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.task.TaskFinishEvent
import org.jetbrains.kotlin.build.report.metrics.ValueType
import org.jetbrains.kotlin.build.report.statistic.file.FileReportService
import org.jetbrains.kotlin.build.report.statistic.formatSize
import org.jetbrains.kotlin.gradle.plugin.stat.BuildFinishStatisticsData
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatisticsData
import org.jetbrains.kotlin.gradle.plugin.stat.GradleBuildStartParameters
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionData
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import java.io.File
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

//Because of https://github.com/gradle/gradle/issues/23359 gradle issue, two build services interaction is not reliable at the end of the build
//Switch back to proper BuildService as soon as this issue is fixed
class BuildReportsService {

    private val log = Logging.getLogger(this.javaClass)
    private val loggerAdapter = GradleLoggerAdapter(log)

    private val startTime = System.nanoTime()
    private val buildUuid = UUID.randomUUID().toString()
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    private val tags = LinkedHashSet<String>()
    private val executedTaskData = ArrayList<CompileStatisticsData>()
    private var customValues = 0 // doesn't need to be thread-safe

    init {
        log.info("Build report service is registered. Unique build id: $buildUuid")
    }

    fun close(
        buildOperationRecords: Collection<BuildOperationRecord>,
        failureMessages: List<String?>,
        parameters: BuildMetricsService.Parameters
    ) {
        val buildData = BuildExecutionData(
            startParameters = parameters.startParameters.get(),
            failureMessages = failureMessages,
            buildOperationRecord = buildOperationRecords.sortedBy { it.startTimeMs }
        )

        val reportingSettings = parameters.reportingSettings.get()

        reportingSettings.httpReportSettings?.also {
            executorService.submit { reportBuildFinish(parameters) } //
        }
        reportingSettings.fileReportSettings?.also {
            FileReportService.reportBuildStatInFile(
                it.buildReportDir,
                parameters.projectName.get(),
                it.includeMetricsInReport,
                //TODO
                buildOperationRecords.map {
                    prepareData(
                        taskResult = null,
                        it.path,
                        it.startTimeMs,
                        it.startTimeMs,
                        parameters.projectName.get(),
                        buildUuid,
                        parameters.label.orNull,
                        parameters.kotlinVersion.get(),
                        it,
                        parameters.additionalTags.get()
                    )
                }.filterNotNull(),
                parameters.startParameters.get(),
                failureMessages.filter { it != null && it.isNotEmpty() } as List<String>,
                loggerAdapter
            )
        }

        reportingSettings.singleOutputFile?.also { singleOutputFile ->
            MetricsWriter(singleOutputFile.absoluteFile).process(buildData, log)
        }

        //It's expected that bad internet connection can cause a significant delay for big project
        executorService.shutdown()
    }

    fun onFinish(event: TaskFinishEvent, buildOperation: BuildOperationRecord?, parameters: BuildMetricsService.Parameters) {
        addHttpReport(event, buildOperation, parameters)
    }

    private fun reportBuildFinish(parameters: BuildMetricsService.Parameters) {
        val httpReportSettings = parameters.reportingSettings.get().httpReportSettings ?: return

        val branchName = if (httpReportSettings.includeGitBranchName) {
            val process = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
                .directory(parameters.projectDir.asFile.get())
                .start().also {
                    it.waitFor(5, TimeUnit.SECONDS)
                }
            process.inputStream.reader().readText()
        } else "is not set"

        val buildFinishData = BuildFinishStatisticsData(
            projectName = parameters.projectName.get(),
            startParameters = parameters.startParameters.get()
                .includeVerboseEnvironment(parameters.reportingSettings.get().httpReportSettings?.verboseEnvironment ?: false),
            buildUuid = buildUuid,
            label = parameters.label.orNull,
            totalTime = TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - startTime)),
            finishTime = System.currentTimeMillis(),
            hostName = hostName,
            tags = tags.toList(),
            gitBranch = branchName
        )

        parameters.httpService.orNull?.sendData(buildFinishData, loggerAdapter)
    }

    private fun GradleBuildStartParameters.includeVerboseEnvironment(verboseEnvironment: Boolean): GradleBuildStartParameters {
        return if (verboseEnvironment) {
            this
        } else {
            GradleBuildStartParameters(
                tasks = this.tasks,
                excludedTasks = this.excludedTasks,
                currentDir = null,
                projectProperties = emptyList(),
                systemProperties = emptyList()
            )
        }
    }

    private fun addHttpReport(
        event: TaskFinishEvent,
        buildOperationRecord: BuildOperationRecord?,
        parameters: BuildMetricsService.Parameters
    ) {
        parameters.httpService.orNull?.also { httpService ->
            val data =
                prepareData(
                    event,
                    parameters.projectName.get(),
                    buildUuid,
                    parameters.label.orNull,
                    parameters.kotlinVersion.get(),
                    buildOperationRecord,
                    parameters.additionalTags.get()
                )
            data?.also {
                executorService.submit {
                    httpService.sendData(data, loggerAdapter)
                }
            }

        }

    }


    private fun addBuildScanReport(
        event: FinishEvent?,
        buildOperationRecord: BuildOperationRecord,
        parameters: BuildMetricsService.Parameters,
        buildScanExtension: BuildScanExtensionHolder
    ) {
        val buildScanSettings = parameters.reportingSettings.orNull?.buildScanReportSettings
        if (buildScanSettings != null && parameters.buildScanEnabled.get()) {
            if (event is TaskFinishEvent) {
                val (collectDataDuration, compileStatData) = measureTimeMillisWithResult {
                    prepareData(
                        event,
                        parameters.projectName.get(), buildUuid, parameters.label.orNull,
                        parameters.kotlinVersion.get(),
                        buildOperationRecord,
                        metricsToShow = buildScanSettings.metrics
                    )
                }
                log.debug("Collect data takes $collectDataDuration: $compileStatData")

                compileStatData?.also { statData ->
                    buildScanExtension?.also { addBuildScanReport(statData, buildScanSettings.customValueLimit, it) }
                }
            }
        }
    }

    private fun addBuildScanReport(data: CompileStatisticsData, customValuesLimit: Int, buildScan: BuildScanExtensionHolder) {
        val elapsedTime = measureTimeMillis {
            data.tags
                .filter { !tags.contains(it) }
                .forEach {
                    addBuildScanTag(buildScan, it)
                }

            if (customValues < customValuesLimit) {
                readableString(data).forEach {
                    if (customValues < customValuesLimit) {
                        addBuildScanValue(buildScan, data, it)
                    } else {
                        log.debug(
                            "Can't add any more custom values into build scan." +
                                    " Statistic data for ${data.taskName} was cut due to custom values limit."
                        )
                    }
                }
            } else {
                log.debug("Can't add any more custom values into build scan.")
            }
        }

        log.debug("Report statistic to build scan takes $elapsedTime ms")
    }

    private fun addBuildScanValue(
        buildScan: BuildScanExtensionHolder,
        data: CompileStatisticsData,
        customValue: String
    ) {
        buildScan.buildScan?.value(data.taskName, customValue)
        customValues++
    }

    private fun addBuildScanTag(buildScan: BuildScanExtensionHolder, tag: String) {
        buildScan.buildScan?.tag(tag)
        tags.add(tag)
    }

    private fun readableString(data: CompileStatisticsData): List<String> {
        val readableString = StringBuilder()
        if (data.nonIncrementalAttributes.isEmpty()) {
            readableString.append("Incremental build; ")
            data.changes.joinTo(readableString, prefix = "Changes: [", postfix = "]; ") { it.substringAfterLast(File.separator) }
        } else {
            data.nonIncrementalAttributes.joinTo(
                readableString,
                prefix = "Non incremental build because: [",
                postfix = "]; "
            ) { it.readableString }
        }

        val timeData =
            data.buildTimesMetrics.map { (key, value) -> "${key.readableString}: ${value}ms" } //sometimes it is better to have separate variable to be able debug
        val perfData = data.performanceMetrics.map { (key, value) ->
            when (key.type) {
                ValueType.BYTES -> "${key.readableString}: ${formatSize(value)}"
                ValueType.MILLISECONDS -> DATE_FORMATTER.format(value)
                else -> "${key.readableString}: $value}"
            }
        }
        timeData.union(perfData).joinTo(readableString, ",", "Performance: [", "]")

        return splitStringIfNeed(readableString.toString(), CUSTOM_VALUE_LENGTH_LIMIT)
    }

    private fun splitStringIfNeed(str: String, lengthLimit: Int): List<String> {
        val splattedString = ArrayList<String>()
        var tempStr = str
        while (tempStr.length > lengthLimit) {
            val subSequence = tempStr.substring(lengthLimit)
            var index = subSequence.lastIndexOf(';')
            if (index == -1) {
                index = subSequence.lastIndexOf(',')
                if (index == -1) {
                    index = lengthLimit
                }
            }
            splattedString.add(tempStr.substring(index))
            tempStr = tempStr.substring(index)

        }
        splattedString.add(tempStr)
        return splattedString
    }

    internal fun initBuildScanTags(buildScan: BuildScanExtensionHolder, parameters: BuildMetricsService.Parameters) {
        buildScan.buildScan?.tag(buildUuid)
        parameters.label.orNull?.also {
            buildScan.buildScan?.tag(it)
        }
    }

    companion object {

        const val CUSTOM_VALUE_LENGTH_LIMIT = 100_000
        private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

        fun getStartParameters(project: Project) = project.gradle.startParameter.let {
            GradleBuildStartParameters(
                tasks = it.taskRequests.flatMap { it.args },
                excludedTasks = it.excludedTaskNames,
                currentDir = it.currentDir.path,
                projectProperties = it.projectProperties.map { (key, value) -> "$key: $value" },
                systemProperties = it.systemPropertiesArgs.map { (key, value) -> "$key: $value" },
            )
        }

        val hostName: String? = try {
            InetAddress.getLocalHost().hostName
        } catch (_: Exception) {
            //do nothing
            null
        }
    }

}

enum class TaskExecutionState {
    SKIPPED,
    FAILED,
    UNKNOWN,
    SUCCESS,
    FROM_CACHE,
    UP_TO_DATE
    ;
}
