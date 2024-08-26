/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskExecutionResult
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.statistics.HttpReportParameters
import org.jetbrains.kotlin.gradle.plugin.BuildEventsListenerRegistryHolder
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.build.report.statistics.BuildStartParameters
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.gradle.report.BuildReportsService.Companion.getStartParameters
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.StatisticsBuildFlowManager
import org.jetbrains.kotlin.gradle.plugin.internal.isConfigurationCacheEnabled
import org.jetbrains.kotlin.gradle.plugin.internal.isProjectIsolationEnabled
import java.lang.management.ManagementFactory

internal interface UsesBuildMetricsService : Task {
    @get:Internal
    val buildMetricsService: Property<BuildMetricsService?>
}

abstract class BuildMetricsService : BuildService<BuildMetricsService.Parameters>, AutoCloseable, OperationCompletionListener {

    //Part of BuildReportService
    interface Parameters : BuildServiceParameters {
        val startParameters: Property<BuildStartParameters>
        val reportingSettings: Property<ReportingSettings>
        val httpParameters: Property<HttpReportParameters>

        val projectDir: DirectoryProperty
        val label: Property<String?>
        val projectName: Property<String>
        val kotlinVersion: Property<String>
        val buildConfigurationTags: ListProperty<StatTag>
    }

    private val buildReportService = BuildReportsService()

    // Tasks and transforms' records
    private val buildOperationRecords = ConcurrentLinkedQueue<BuildOperationRecord>()
    private val failureMessages = ConcurrentLinkedQueue<String>()

    // Info for tasks only
    private val taskPathToMetricsReporter = ConcurrentHashMap<String, BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>>()
    private val taskPathToTaskClass = ConcurrentHashMap<String, String>()

    open fun addTask(
        taskPath: String,
        taskClass: Class<*>,
        metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    ) {
        taskPathToMetricsReporter.put(taskPath, metricsReporter).also {
            if (it != null) log.warn("Duplicate task path: $taskPath") // Should never happen but log it just in case
        }
        taskPathToTaskClass.put(taskPath, taskClass.name).also {
            if (it != null) log.warn("Duplicate task path: $taskPath") // Should never happen but log it just in case
        }
    }

    open fun addTransformMetrics(
        transformPath: String,
        transformClass: Class<*>,
        isKotlinTransform: Boolean,
        startTimeMs: Long,
        totalTimeMs: Long,
        buildMetrics: BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>,
        failureMessage: String?,
    ) {
        buildOperationRecords.add(
            TransformRecord(transformPath, transformClass.name, isKotlinTransform, startTimeMs, totalTimeMs, buildMetrics)
        )
        failureMessage?.let { failureMessages.add(it) }
    }

    private fun updateBuildOperationRecord(event: TaskFinishEvent): TaskRecord {
        val result = event.result
        val taskPath = event.descriptor.taskPath
        val totalTimeMs = result.endTime - result.startTime

        val buildMetrics = BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>()
        buildMetrics.buildTimes.addTimeMs(GradleBuildTime.GRADLE_TASK, totalTimeMs)
        taskPathToMetricsReporter[taskPath]?.let {
            buildMetrics.addAll(it.getMetrics())
        }
        val taskExecutionResult = TaskExecutionResults[taskPath]
        taskExecutionResult?.buildMetrics?.also {
            buildMetrics.addAll(it)
        }

        val buildOperation = TaskRecord(
            path = taskPath,
            classFqName = taskPathToTaskClass[taskPath] ?: "unknown",
            startTimeMs = result.startTime,
            totalTimeMs = totalTimeMs,
            buildMetrics = buildMetrics,
            didWork = result is TaskExecutionResult,
            skipMessage = (result as? TaskSkippedResult)?.skipMessage,
            icLogLines = taskExecutionResult?.icLogLines ?: emptyList(),
            changedFiles = taskExecutionResult?.taskInfo?.changedFiles,
            compilerArguments = taskExecutionResult?.taskInfo?.compilerArguments ?: emptyArray(),
            kotlinLanguageVersion = taskExecutionResult?.taskInfo?.kotlinLanguageVersion,
            statTags = taskExecutionResult?.taskInfo?.tags ?: emptySet()
        )
        buildOperationRecords.add(buildOperation)
        if (result is TaskFailureResult) {
            failureMessages.addAll(result.failures.map { it.message })
        }
        return buildOperation
    }

    override fun close() {
        buildReportService.close(buildOperationRecords, failureMessages.toList(), parameters.toBuildReportParameters())
    }

    override fun onFinish(event: FinishEvent?) {
        if (event is TaskFinishEvent) {
            val buildOperation = updateBuildOperationRecord(event)
            val buildParameters = parameters.toBuildReportParameters()
            buildReportService.onFinish(event, buildOperation, buildParameters)
        }
    }

    companion object {
        private val serviceClass = BuildMetricsService::class.java
        private val serviceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"
        private val log = Logging.getLogger(BuildMetricsService::class.java)

        private fun Parameters.toBuildReportParameters() = BuildReportParameters(
            startParameters = startParameters.get(),
            reportingSettings = reportingSettings.get(),
            httpReportParameters = httpParameters.orNull,
            projectDir = projectDir.asFile.get(),
            label = label.orNull,
            projectName = projectName.get(),
            kotlinVersion = kotlinVersion.get(),
            additionalTags = HashSet(buildConfigurationTags.get()),
        )

        private fun registerIfAbsentImpl(
            project: Project,
        ): Provider<BuildMetricsService>? {
            // Return early if the service was already registered to avoid the overhead of reading the reporting settings below
            project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
                @Suppress("UNCHECKED_CAST")
                return it.service as Provider<BuildMetricsService>
            }

            //do not need to collect metrics if there aren't consumers for this data
            val reportingSettings = reportingSettings(project)
            if (reportingSettings.buildReportOutputs.isEmpty()) {
                return null
            }

            val kotlinVersion = project.getKotlinPluginVersion()

            return project.gradle.sharedServices.registerIfAbsent(serviceName, serviceClass) {
                it.parameters.label.set(reportingSettings.buildReportLabel)
                it.parameters.projectName.set(project.rootProject.name)
                it.parameters.kotlinVersion.set(kotlinVersion)
                it.parameters.startParameters.set(getStartParameters(project))
                it.parameters.reportingSettings.set(reportingSettings)
                reportingSettings.httpReportSettings?.let { httpSettings ->
                    if (!httpSettings.useExecutorForHttpReport) {
                        log.warn("`kotlin.internal.build.report.http.use.executor` property is for test purposes only")
                    }

                    it.parameters.httpParameters.set(
                        HttpReportParameters(
                            httpSettings.url,
                            httpSettings.user,
                            httpSettings.password,
                            //for tests only
                            httpSettings.useExecutorForHttpReport,
                        )
                    )
                    log.debug("Http report is enabled for ${httpSettings.url}")
                } ?: log.debug("Http report is disabled")
                it.parameters.projectDir.set(project.layout.projectDirectory)
                //init gradle tags for build scan and http reports
                it.parameters.buildConfigurationTags.value(setupTags(project))
            }.also {
                subscribeForTaskEvents(project, it)
            }

        }

        private fun subscribeForTaskEvents(project: Project, buildMetricServiceProvider: Provider<BuildMetricsService>) {
            val buildScanHolder = initBuildScanHolder(project, buildMetricServiceProvider)
            if (buildScanHolder != null) {
                subscribeForTaskEventsForBuildScan(project, buildMetricServiceProvider, buildScanHolder)
            }

            val gradle80withBuildScanReport =
                GradleVersion.current().baseVersion == GradleVersion.version("8.0") && buildScanHolder != null

            if (!gradle80withBuildScanReport) {
                BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(buildMetricServiceProvider)
            }
        }

        private fun initBuildScanHolder(
            project: Project,
            buildMetricServiceProvider: Provider<BuildMetricsService>,
        ): BuildScanApi? {
            buildMetricServiceProvider.get().parameters.reportingSettings.orNull?.buildScanReportSettings ?: return null

            val rootProject = if (project.isProjectIsolationEnabled) {
                project
            } else {
                project.rootProject
            }

            val buildScan =
                rootProject.plugins.findPlugin("develocity")?.let { DevelocityPluginHolder(it) }
                ?: rootProject.extensions.findByName("buildScan")?.let { BuildScanExtensionHolder(it) }

            when {
                buildScan == null && project.isProjectIsolationEnabled ->
                    log.warn(
                        "Build report creation in the build scan format is not yet supported when the isolated projects feature is enabled." +
                                " Follow https://youtrack.jetbrains.com/issue/KT-68847 for the updates." +
                                " Build report for build scan won't be created."
                    )
                buildScan == null -> log.debug("Build scan is not enabled. Build report for build scan won't be created.")
                else -> log.debug("Build report for build scan is configured.")
            }

            return buildScan
        }

        private fun subscribeForTaskEventsForBuildScan(
            project: Project,
            buildMetricServiceProvider: Provider<BuildMetricsService>,
            buildScanHolder: BuildScanApi,
        ) {
            when {
                GradleVersion.current().baseVersion < GradleVersion.version("8.0") -> {
                    buildScanHolder.buildFinished {
                        buildMetricServiceProvider.map { it.addBuildScanReport(buildScanHolder) }.get()
                    }
                }
                GradleVersion.current().baseVersion < GradleVersion.version("8.1") -> {
                    val buildMetricService = buildMetricServiceProvider.get()
                    buildMetricService.buildReportService.initBuildScanTags(buildScanHolder, buildMetricService.parameters.label.orNull)
                    BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(project.provider {
                        OperationCompletionListener { event ->
                            if (event is TaskFinishEvent) {
                                val buildOperation = buildMetricService.updateBuildOperationRecord(event)
                                val buildParameters = buildMetricService.parameters.toBuildReportParameters()
                                val buildReportService = buildMetricServiceProvider.map { it.buildReportService }.get()
                                buildReportService.addBuildScanReport(event, buildOperation, buildParameters, buildScanHolder)
                                buildReportService.onFinish(event, buildOperation, buildParameters)
                            }
                        }

                    })
                }
                else -> {
                    StatisticsBuildFlowManager.getInstance(project).subscribeForBuildScan(buildScanHolder)
                }
            }
        }

        fun registerIfAbsent(project: Project) =
            registerIfAbsentImpl(project)?.also { serviceProvider ->
                SingleActionPerProject.run(project, UsesBuildMetricsService::class.java.name) {
                    project.tasks.withType<UsesBuildMetricsService>().configureEach { task ->
                        task.buildMetricsService.value(serviceProvider).disallowChanges()
                        task.usesService(serviceProvider)
                    }
                }
            }

        private fun setupTags(project: Project): ArrayList<StatTag> {
            val gradle = project.gradle
            val additionalTags = ArrayList<StatTag>()
            if (project.isConfigurationCacheEnabled) {
                additionalTags.add(StatTag.CONFIGURATION_CACHE)
            }
            if (gradle.startParameter.isBuildCacheEnabled) {
                additionalTags.add(StatTag.BUILD_CACHE)
            }
            val debugConfiguration = "-agentlib:"
            if (ManagementFactory.getRuntimeMXBean().inputArguments.firstOrNull { it.startsWith(debugConfiguration) } != null) {
                additionalTags.add(StatTag.GRADLE_DEBUG)
            }
            return additionalTags
        }
    }

    internal fun addBuildScanReport(buildScan: BuildScanApi?) {
        if (buildScan == null) return
        buildReportService.initBuildScanTags(buildScan, parameters.label.orNull)
        buildReportService.addBuildScanReport(buildOperationRecords, parameters.toBuildReportParameters(), buildScan)
        parameters.buildConfigurationTags.orNull?.forEach { buildScan.tag(it.readableString) }
        buildReportService.addCollectedTags(buildScan)
    }
}

internal class TaskRecord(
    override val path: String,
    override val classFqName: String,
    override val startTimeMs: Long,
    override val totalTimeMs: Long,
    override val buildMetrics: BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>,
    override val didWork: Boolean,
    override val skipMessage: String?,
    override val icLogLines: List<String>,
    val kotlinLanguageVersion: KotlinVersion?,
    val changedFiles: SourcesChanges? = null,
    val compilerArguments: Array<String> = emptyArray(),
    val statTags: Set<StatTag> = emptySet(),
) : BuildOperationRecord {
    override val isFromKotlinPlugin: Boolean = classFqName.startsWith("org.jetbrains.kotlin")
}

private class TransformRecord(
    override val path: String,
    override val classFqName: String,
    override val isFromKotlinPlugin: Boolean,
    override val startTimeMs: Long,
    override val totalTimeMs: Long,
    override val buildMetrics: BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>,
) : BuildOperationRecord {
    override val didWork: Boolean = true
    override val skipMessage: String? = null
    override val icLogLines: List<String> = emptyList()
}
