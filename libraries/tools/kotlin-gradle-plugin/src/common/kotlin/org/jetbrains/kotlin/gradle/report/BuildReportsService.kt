/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import com.google.gson.Gson
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.gradle.tooling.events.task.TaskSuccessResult
import org.jetbrains.kotlin.build.report.metrics.SizeMetricType
import org.jetbrains.kotlin.gradle.plugin.BuildEventsListenerRegistryHolder
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.stat.BuildFinishStatisticsData
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatisticsData
import org.jetbrains.kotlin.gradle.plugin.stat.GradleBuildStartParameters
import org.jetbrains.kotlin.gradle.plugin.stat.StatTag
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionData
import org.jetbrains.kotlin.gradle.utils.formatSize
import org.jetbrains.kotlin.gradle.utils.isConfigurationCacheAvailable
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import java.io.IOException
import java.lang.management.ManagementFactory
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

abstract class BuildReportsService : BuildService<BuildReportsService.Parameters>, AutoCloseable, OperationCompletionListener {

    private val log = Logging.getLogger(this.javaClass)

    private val startTime = System.nanoTime()
    private val buildUuid = UUID.randomUUID().toString()
    private var executorService: ExecutorService = Executors.newSingleThreadExecutor()

    private val tags = LinkedHashSet<String>()
    private var customValues = 0 // doesn't need to be thread-safe

    init {
        log.info("Build report service is registered. Unique build id: $buildUuid")
    }

    interface Parameters : BuildServiceParameters {
        val startParameters: Property<GradleBuildStartParameters>
        val reportingSettings: Property<ReportingSettings>
        var buildMetricsService: Provider<BuildMetricsService>

        val label: Property<String?>
        val projectName: Property<String>
        val kotlinVersion: Property<String>
        val additionalTags: ListProperty<StatTag>
    }

    override fun close() {
        val buildData = BuildExecutionData(
            startParameters = parameters.startParameters.get(),
            failureMessages = parameters.buildMetricsService.orNull?.failureMessages?.toList() ?: emptyList(),
            buildOperationRecord = parameters.buildMetricsService.orNull?.buildOperationRecords?.sortedBy { it.startTimeMs } ?: emptyList()
        )

        val reportingSettings = parameters.reportingSettings.get()

        reportingSettings.httpReportSettings?.also {
            executorService.submit { reportBuildFinish() } //
        }
        reportingSettings.fileReportSettings?.also {
            reportBuildStatInFile(it, buildData)
        }

        reportingSettings.singleOutputFile?.also { singleOutputFile ->
            MetricsWriter(singleOutputFile.absoluteFile).process(buildData, log)
        }

        //It's expected that bad internet connection can cause a significant delay for big project
        executorService.shutdown()
    }

    override fun onFinish(event: FinishEvent?) {
        addHttpReport(event)
    }

    private fun reportBuildStatInFile(fileReportSettings: FileReportSettings, buildData: BuildExecutionData) {
        val ts = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().time)
        val reportFile = fileReportSettings.buildReportDir.resolve("${parameters.projectName.get()}-build-$ts.txt")

        PlainTextBuildReportWriter(
            outputFile = reportFile,
            printMetrics = fileReportSettings.includeMetricsInReport
        ).process(buildData, log)
    }

    private fun reportBuildFinish() {
        val buildFinishData = BuildFinishStatisticsData(
            projectName = parameters.projectName.get(),
            startParameters = parameters.startParameters.get()
                .includeVerboseEnvironment(parameters.reportingSettings.get().httpReportSettings?.verboseEnvironment ?: false),
            buildUuid = buildUuid,
            label = parameters.label.orNull,
            totalTime = (System.nanoTime() - startTime) / 1_000_000,
            finishTime = System.currentTimeMillis(),
            hostName = hostName
        )
        sendDataViaHttp(buildFinishData)
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

    private fun addHttpReport(event: FinishEvent?) {
        if (parameters.reportingSettings.get().httpReportSettings != null) {
            if (event is TaskFinishEvent) {
                val data =
                    prepareData(
                        event,
                        parameters.projectName.get(),
                        buildUuid,
                        parameters.label.orNull,
                        parameters.kotlinVersion.get(),
                        parameters.additionalTags.get()
                    )
                data?.also { executorService.submit { sendDataViaHttp(data) } }
            }
        }

    }

    private var invalidUrl = false
    private var requestPreviousFailed = false
    private fun sendDataViaHttp(data: Any) {
        val httpSettings = parameters.reportingSettings.get().httpReportSettings ?: return

        val elapsedTime = measureTimeMillis {
            if (invalidUrl) {
                return
            }
            val connection = try {
                URL(httpSettings.url).openConnection() as HttpURLConnection
            } catch (e: IOException) {
                log.warn("Unable to open connection to ${httpSettings.url}: ${e.message}")
                invalidUrl = true
                return
            }

            try {
                if (httpSettings.user != null && httpSettings.password != null) {
                    val auth = Base64.getEncoder()
                        .encode("${httpSettings.user}:${httpSettings.password}".toByteArray())
                        .toString(Charsets.UTF_8)
                    connection.addRequestProperty("Authorization", "Basic $auth")
                }
                connection.addRequestProperty("Content-Type", "application/json")
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.outputStream.use {
                    it.write(Gson().toJson(data).toByteArray())
                }
                connection.connect()
                checkResponseAndLog(connection)
            } catch (e: Exception) {
                log.debug("Unexpected exception happened ${e.message}: ${e.stackTrace}")
                checkResponseAndLog(connection)
            } finally {
                connection.disconnect()
            }
        }
        log.debug("Report statistic by http takes $elapsedTime ms")
    }

    private fun checkResponseAndLog(connection: HttpURLConnection) {
        val isResponseBad = connection.responseCode !in 200..299
        if (isResponseBad) {
            val message = "Failed to send statistic to ${connection.url} with ${connection.responseCode}: ${connection.responseMessage}"
            if (!requestPreviousFailed) {
                log.warn(message)
            } else {
                log.debug(message)
            }
            requestPreviousFailed = true
        }
    }


    private fun addBuildScanReport(event: FinishEvent?, buildScan: BuildScanExtensionHolder) {
        val buildScanSettings = parameters.reportingSettings.orNull?.buildScanReportSettings
        if (buildScanSettings != null && buildScan.buildScan != null) {
            if (event is TaskFinishEvent) {
                val (collectDataDuration, compileStatData) = measureTimeMillisWithResult {
                    prepareData(event, parameters.projectName.get(), buildUuid, parameters.label.orNull, parameters.kotlinVersion.get())
                }
                log.debug("Collect data takes $collectDataDuration: $compileStatData")

                compileStatData?.also {
                    addBuildScanReport(it, buildScanSettings.customValueLimit, buildScan)
                }
            }
        }
    }

    private fun addBuildScanReport(data: CompileStatisticsData, customValuesLimit: Int, buildScan: BuildScanExtensionHolder) {
        val elapsedTime = measureTimeMillis {
            buildScan.buildScan?.also {
                if (!tags.contains(buildUuid)) {
                    addBuildScanTag(buildScan, buildUuid)
                }
                data.label?.takeIf { !tags.contains(it) }?.also {
                    addBuildScanTag(buildScan, it)
                }
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
                SizeMetricType.BYTES -> "${key.readableString}: ${formatSize(value)}"
                else -> "${key.readableString}: $value}"
            }
        }
        timeData.union(perfData).joinTo(readableString, ",", "Performance: [", "]")

        return splitStringIfNeed(readableString.toString(), lengthLimit)
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

    companion object {

        private val log = Logging.getLogger(this.javaClass)
        const val lengthLimit = 100_000

        fun getStartParameters(project: Project) = project.gradle.startParameter.let {
            GradleBuildStartParameters(
                tasks = it.taskRequests.flatMap { it.args },
                excludedTasks = it.excludedTaskNames,
                currentDir = it.currentDir.path,
                projectProperties = it.projectProperties.map { (key, value) -> "$key: $value" },
                systemProperties = it.systemPropertiesArgs.map { (key, value) -> "$key: $value" },
            )
        }

        fun registerIfAbsent(project: Project, buildMetricsService: Provider<BuildMetricsService>): Provider<BuildReportsService>? {
            val serviceClass = BuildReportsService::class.java
            val serviceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"

            val reportingSettings = reportingSettings(project.rootProject)
            if (reportingSettings.buildReportOutputs.isEmpty()) {
                return null //no need to collect data
            }

            val kotlinVersion = project.getKotlinPluginVersion()
            val gradle = project.gradle
            project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
                @Suppress("UNCHECKED_CAST")
                return it.service as Provider<BuildReportsService>
            }

            return gradle.sharedServices.registerIfAbsent(serviceName, serviceClass) {
                it.parameters.label.set(reportingSettings.buildReportLabel)
                it.parameters.projectName.set(project.rootProject.name)
                it.parameters.kotlinVersion.set(kotlinVersion)
                it.parameters.startParameters.set(getStartParameters(project))
                it.parameters.reportingSettings.set(reportingSettings)
                it.parameters.buildMetricsService = buildMetricsService

                //init gradle tags for build scan and http reports
                it.parameters.additionalTags.value(setupTags(gradle))
            }.also {
                if (reportingSettings.httpReportSettings != null) {
                    BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(it)
                }

                val buildScanExtension = project.rootProject.extensions.findByName("buildScan")
                if (reportingSettings.buildScanReportSettings != null && buildScanExtension != null) {
                    BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(project.provider {
                        OperationCompletionListener { event ->
                            it.get().addBuildScanReport(event, BuildScanExtensionHolder(buildScanExtension))
                        }
                    })
                }
            }
        }

        private fun setupTags(gradle: Gradle): ArrayList<StatTag> {
            val additionalTags = ArrayList<StatTag>()
            if (isConfigurationCacheAvailable(gradle)) {
                additionalTags.add(StatTag.CONFIGURATION_CACHE)
            }
            if (gradle.startParameter.isBuildCacheEnabled) {
                additionalTags.add(StatTag.BUILD_CACHE)
            }
            return additionalTags
        }

        internal fun prepareData(
            event: TaskFinishEvent,
            projectName: String,
            uuid: String,
            label: String?,
            kotlinVersion: String,
            additionalTags: List<StatTag> = emptyList()
        ): CompileStatisticsData? {
            val result = event.result
            val taskPath = event.descriptor.taskPath
            val durationMs = result.endTime - result.startTime
            val taskResult = when (result) {
                is TaskSuccessResult -> when {
                    result.isFromCache -> TaskExecutionState.FROM_CACHE
                    result.isUpToDate -> TaskExecutionState.UP_TO_DATE
                    else -> TaskExecutionState.SUCCESS
                }

                is TaskSkippedResult -> TaskExecutionState.SKIPPED
                is TaskFailureResult -> TaskExecutionState.FAILED
                else -> TaskExecutionState.UNKNOWN
            }

            if (!availableForStat(taskPath)) {
                return null
            }

            val taskExecutionResult = TaskExecutionResults[taskPath]
            val buildTimesMs = taskExecutionResult?.buildMetrics?.buildTimes?.asMapMs()?.filterValues { value -> value != 0L } ?: emptyMap()
            val perfData =
                taskExecutionResult?.buildMetrics?.buildPerformanceMetrics?.asMap()?.filterValues { value -> value != 0L } ?: emptyMap()
            val changes = when (val changedFiles = taskExecutionResult?.taskInfo?.changedFiles) {
                is ChangedFiles.Known -> changedFiles.modified.map { it.absolutePath } + changedFiles.removed.map { it.absolutePath }
                else -> emptyList<String>()
            }
            return CompileStatisticsData(
                durationMs = durationMs,
                taskResult = taskResult.name,
                label = label,
                buildTimesMetrics = buildTimesMs,
                performanceMetrics = perfData,
                projectName = projectName,
                taskName = taskPath,
                changes = changes,
                tags = parseTags(taskExecutionResult, additionalTags).map { it.name },
                nonIncrementalAttributes = taskExecutionResult?.buildMetrics?.buildAttributes?.asMap()?.filter { it.value > 0 }?.keys
                    ?: emptySet(),
                hostName = hostName,
                kotlinVersion = kotlinVersion,
                buildUuid = uuid,
                finishTime = System.currentTimeMillis(),
                compilerArguments = taskExecutionResult?.taskInfo?.compilerArguments?.asList() ?: emptyList()
            )
        }

        val hostName: String? = try {
            InetAddress.getLocalHost().hostName
        } catch (_: Exception) {
            //do nothing
            null
        }

        private fun availableForStat(taskPath: String): Boolean {
            return taskPath.contains("Kotlin") && (TaskExecutionResults[taskPath] != null)
        }

        private fun parseTags(taskExecutionResult: TaskExecutionResult?, additionalTags: List<StatTag>): List<StatTag> {
            val tags = ArrayList(additionalTags)

            val nonIncrementalAttributes = taskExecutionResult?.buildMetrics?.buildAttributes?.asMap() ?: emptyMap()

            if (nonIncrementalAttributes.isEmpty()) {
                tags.add(StatTag.INCREMENTAL)
            } else {
                tags.add(StatTag.NON_INCREMENTAL)
            }

            val taskInfo = taskExecutionResult?.taskInfo

            taskInfo?.withAbiSnapshot?.ifTrue {
                tags.add(StatTag.ABI_SNAPSHOT)
            }
            taskInfo?.withArtifactTransform?.ifTrue {
                tags.add(StatTag.ARTIFACT_TRANSFORM)
            }

            val debugConfiguration = "-agentlib:"
            if (ManagementFactory.getRuntimeMXBean().inputArguments.firstOrNull { it.startsWith(debugConfiguration) } != null) {
                tags.add(StatTag.GRADLE_DEBUG)
            }
            return tags
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
