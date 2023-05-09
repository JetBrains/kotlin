/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.tooling.events.task.TaskFinishEvent
import org.jetbrains.kotlin.build.report.metrics.ValueType
import org.jetbrains.kotlin.build.report.statistics.HttpReportService
import org.jetbrains.kotlin.build.report.statistics.file.FileReportService
import org.jetbrains.kotlin.build.report.statistics.formatSize
import org.jetbrains.kotlin.build.report.statistics.BuildFinishStatisticsData
import org.jetbrains.kotlin.build.report.statistics.CompileStatisticsData
import org.jetbrains.kotlin.build.report.statistics.BuildStartParameters
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
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

    private val tags = LinkedHashSet<StatTag>()
    private var customValues = 0 // doesn't need to be thread-safe

    init {
        log.info("Build report service is registered. Unique build id: $buildUuid")
    }

    fun close(
        buildOperationRecords: Collection<BuildOperationRecord>,
        failureMessages: List<String>,
        parameters: BuildReportParameters
    ) {
        val buildData = BuildExecutionData(
            startParameters = parameters.startParameters,
            failureMessages = failureMessages,
            buildOperationRecord = buildOperationRecords.sortedBy { it.startTimeMs }
        )

        val reportingSettings = parameters.reportingSettings

        reportingSettings.httpReportSettings?.also {
            executorService.submit { reportBuildFinish(parameters) }
        }
        reportingSettings.fileReportSettings?.also {
            FileReportService.reportBuildStatInFile(
                it.buildReportDir,
                parameters.projectName,
                it.includeMetricsInReport,
                buildOperationRecords.mapNotNull {
                    prepareData(
                        taskResult = null,
                        it.path,
                        it.startTimeMs,
                        it.totalTimeMs + it.startTimeMs,
                        parameters.projectName,
                        buildUuid,
                        parameters.label,
                        parameters.kotlinVersion,
                        it,
                        onlyKotlinTask = false,
                        parameters.additionalTags
                    )
                },
                parameters.startParameters,
                failureMessages.filter { it.isNotEmpty() },
                loggerAdapter
            )
        }

        reportingSettings.singleOutputFile?.also { singleOutputFile ->
            MetricsWriter(singleOutputFile.absoluteFile).process(buildData, log)
        }

        if (reportingSettings.experimentalTryK2ConsoleOutput) {
            reportTryK2ToConsole(buildData)
        }

        //It's expected that bad internet connection can cause a significant delay for big project
        executorService.shutdown()
    }

    fun onFinish(
        event: TaskFinishEvent, buildOperation: BuildOperationRecord,
        parameters: BuildReportParameters, buildScan: BuildScanExtensionHolder?
    ) {
        buildScan?.also { addBuildScanReport(event, buildOperation, parameters, it) }
        addHttpReport(event, buildOperation, parameters)
    }

    private fun reportBuildFinish(parameters: BuildReportParameters) {
        val httpReportSettings = parameters.reportingSettings.httpReportSettings ?: return

        val branchName = if (httpReportSettings.includeGitBranchName) {
            val process = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
                .directory(parameters.projectDir)
                .start().also {
                    it.waitFor(5, TimeUnit.SECONDS)
                }
            process.inputStream.reader().readText()
        } else "is not set"

        val buildFinishData = BuildFinishStatisticsData(
            projectName = parameters.projectName,
            startParameters = parameters.startParameters
                .includeVerboseEnvironment(parameters.reportingSettings.httpReportSettings.verboseEnvironment),
            buildUuid = buildUuid,
            label = parameters.label,
            totalTime = TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - startTime)),
            finishTime = System.currentTimeMillis(),
            hostName = hostName,
            tags = tags,
            gitBranch = branchName
        )

        parameters.httpService?.sendData(buildFinishData, loggerAdapter)
    }

    private fun BuildStartParameters.includeVerboseEnvironment(verboseEnvironment: Boolean): BuildStartParameters {
        return if (verboseEnvironment) {
            this
        } else {
            BuildStartParameters(
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
        buildOperationRecord: BuildOperationRecord,
        parameters: BuildReportParameters
    ) {
        parameters.httpService?.also { httpService ->
            val data =
                prepareData(
                    event,
                    parameters.projectName,
                    buildUuid,
                    parameters.label,
                    parameters.kotlinVersion,
                    buildOperationRecord,
                    onlyKotlinTask = true,
                    parameters.additionalTags
                )
            data?.also {
                executorService.submit {
                    httpService.sendData(data, loggerAdapter)
                }
            }
        }

    }

    private fun addBuildScanReport(
        event: TaskFinishEvent,
        buildOperationRecord: BuildOperationRecord,
        parameters: BuildReportParameters,
        buildScanExtension: BuildScanExtensionHolder
    ) {
        val buildScanSettings = parameters.reportingSettings.buildScanReportSettings ?: return

        val (collectDataDuration, compileStatData) = measureTimeMillisWithResult {
            prepareData(
                event,
                parameters.projectName, buildUuid, parameters.label,
                parameters.kotlinVersion,
                buildOperationRecord,
                metricsToShow = buildScanSettings.metrics
            )
        }
        log.debug("Collect data takes $collectDataDuration: $compileStatData")

        compileStatData?.also {
            addBuildScanReport(it, buildScanSettings.customValueLimit, buildScanExtension)
        }
    }

    private fun addBuildScanReport(data: CompileStatisticsData, customValuesLimit: Int, buildScan: BuildScanExtensionHolder) {
        val elapsedTime = measureTimeMillis {
            tags.addAll(data.tags)
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
        buildScan.buildScan.value(data.taskName, customValue)
        customValues++
    }

    private fun reportTryK2ToConsole(
        data: BuildExecutionData
    ) {
        val tasksData = data.buildOperationRecord
            .filterIsInstance<TaskRecord>()
            .filter {
                // Filtering by only KGP tasks and by those that actually do compilation
                it.isFromKotlinPlugin && it.kotlinLanguageVersion != null
            }
        log.warn("##### 'kotlin.experimental.tryK2' results (Kotlin/Native not checked) #####")
        if (tasksData.isEmpty()) {
            log.warn("No Kotlin compilation tasks have run")
            log.warn("#####")
        } else {
            val tasksCountWithKotlin2 = tasksData.count {
                it.kotlinLanguageVersion != null && it.kotlinLanguageVersion >= KotlinVersion.KOTLIN_2_0
            }
            val taskWithK2Percent = (tasksCountWithKotlin2 * 100) / tasksData.count()
            val statsData = tasksData.map { it.path to it.kotlinLanguageVersion?.version }
            statsData.forEach { record ->
                log.warn("${record.first}: ${record.second} language version")
            }
            log.warn("##### $taskWithK2Percent% ($tasksCountWithKotlin2/${tasksData.count()}) tasks have compiled with Kotlin 2 #####")
        }
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

        data.kotlinLanguageVersion?.also {
            readableString.append("Kotlin language version: $it; ")
        }

        val timeData =
            data.buildTimesMetrics.map { (key, value) -> "${key.readableString}: ${value}ms" } //sometimes it is better to have separate variable to be able debug
        val perfData = data.performanceMetrics.map { (key, value) ->
            when (key.type) {
                ValueType.BYTES -> "${key.readableString}: ${formatSize(value)}"
                ValueType.MILLISECONDS -> DATE_FORMATTER.format(value)
                else -> "${key.readableString}: $value"
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

    internal fun initBuildScanTags(buildScan: BuildScanExtensionHolder, label: String?) {
        buildScan.buildScan.tag(buildUuid)
        label?.also {
            buildScan.buildScan.tag(it)
        }
    }

    internal fun addCollectedTags(buildScan: BuildScanExtensionHolder) {
        replaceWithCombinedTag(
            StatTag.KOTLIN_1,
            StatTag.KOTLIN_2,
            StatTag.KOTLIN_1_AND_2
        )

        replaceWithCombinedTag(
            StatTag.INCREMENTAL,
            StatTag.NON_INCREMENTAL,
            StatTag.INCREMENTAL_AND_NON_INCREMENTAL
        )

        tags.forEach { buildScan.buildScan.tag(it.readableString) }
    }

    private fun replaceWithCombinedTag(firstTag: StatTag, secondTag: StatTag, combinedTag: StatTag) {
        val containsFirstTag = tags.remove(firstTag)
        val containsSecondTag = tags.remove(secondTag)
        when {
            containsFirstTag && containsSecondTag -> tags.add(combinedTag)
            containsFirstTag -> tags.add(firstTag)
            containsSecondTag -> tags.add(secondTag)
        }
    }

    companion object {

        const val CUSTOM_VALUE_LENGTH_LIMIT = 100_000
        private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

        fun getStartParameters(project: Project) = project.gradle.startParameter.let {
            BuildStartParameters(
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

data class BuildReportParameters(
    val startParameters: BuildStartParameters,
    val reportingSettings: ReportingSettings,
    val httpService: HttpReportService?,

    val projectDir: File,
    val label: String?,
    val projectName: String,
    val kotlinVersion: String,
    val additionalTags: Set<StatTag>
)