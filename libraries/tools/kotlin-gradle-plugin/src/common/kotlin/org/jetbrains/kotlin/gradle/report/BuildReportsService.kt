/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import com.google.gson.Gson
import com.gradle.scan.plugin.BuildScanExtension
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
import org.gradle.tooling.events.task.TaskFinishEvent
import org.jetbrains.kotlin.gradle.plugin.BuildEventsListenerRegistryHolder
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.stat.BuildFinishStatisticsData
import org.jetbrains.kotlin.gradle.plugin.stat.GradleBuildStartParameters
import org.jetbrains.kotlin.gradle.plugin.stat.StatTag
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildScanStatisticsListener
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatListener
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionData
import org.jetbrains.kotlin.gradle.utils.isConfigurationCacheAvailable
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

abstract class BuildReportsService : BuildService<BuildReportsService.Parameters>, AutoCloseable, OperationCompletionListener {

    private val log = Logging.getLogger(this.javaClass)

    private val startTime = System.nanoTime()
    private val buildUuid = UUID.randomUUID().toString()
    private var executorService: ExecutorService = Executors.newSingleThreadExecutor()

    private var buildMetricsService: Provider<BuildMetricsService>? = null


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
        executorService.shutdownNow()
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
            startParameters = parameters.startParameters.get(),
            buildUuid = buildUuid,
            label = parameters.label.orNull,
            totalTime = (System.nanoTime() - startTime) / 1_000_000
        )
        sendDataViaHttp(buildFinishData)
    }

    private fun initService(project: Project) {
        addListeners(project)
    }

    private fun addBuildScanReport(project: Project) {
        if (parameters.reportingSettings.buildReportOutputs.contains(BuildReportType.BUILD_SCAN)) {
            val listenerRegistryHolder = BuildEventsListenerRegistryHolder.getInstance(project)
            project.rootProject.extensions.findByName("buildScan")
                ?.also {
                    listenerRegistryHolder.listenerRegistry.onTaskCompletion(
                        project.provider {
                            BuildScanStatisticsListener(
                                it as BuildScanExtension,
                                parameters.projectName.get(),
                                parameters.label.orNull,
                                parameters.kotlinVersion.get(),
                                buildUuid
                            )
                        }
                    )
                }
        }
    }

    private fun addHttpReport(event: FinishEvent?) {
        if (parameters.reportingSettings.get().httpReportSettings != null) {
            if (event is TaskFinishEvent) {
                val data =
                    KotlinBuildStatListener.prepareData(
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

    private fun addListeners(project: Project) {
        addBuildScanReport(project)
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

    companion object {

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
            }!!.also {
                val buildReportsService = it.get()
                buildReportsService.initService(project)
                if (buildReportsService.parameters.reportingSettings.get().buildReportOutputs.contains(BuildReportType.HTTP)) {
                    BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(it)
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
    }

}