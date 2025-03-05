/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.fus.BuildUidService
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.BuildEventsListenerRegistryHolder
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.internal.isConfigurationCacheRequested
import org.jetbrains.kotlin.gradle.plugin.internal.isProjectIsolationEnabled
import org.jetbrains.kotlin.gradle.plugin.internal.isProjectIsolationRequested
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.report.reportingSettings
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StatisticsValuesConsumer
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.Serializable


internal interface UsesBuildFusService : Task {
    @get:Internal
    val buildFusService: Property<BuildFusService<out BuildFusService.Parameters>?>
}

abstract class BuildFusService<T : BuildFusService.Parameters> :
    BuildService<T>,
    AutoCloseable, OperationCompletionListener {
    protected var buildFailed: Boolean = false
    private val log = Logging.getLogger(this.javaClass)
    protected val buildId = parameters.buildId.get()

    init {
        log.kotlinDebug("Initialize ${this.javaClass.simpleName}")
        KotlinBuildStatsBeanService.recordBuildStart(buildId)
    }

    interface Parameters : BuildServiceParameters {
        val generalMetricsFinalized: Property<Boolean>
        val generalConfigurationMetrics: Property<MetricContainer>
        val buildStatisticsConfiguration: Property<KotlinBuildStatsConfiguration>
        val buildId: Property<String>
    }

    private val fusMetricsConsumer = SynchronizedMetricsContainer()

    internal fun getFusMetricsConsumer(): StatisticsValuesConsumer = fusMetricsConsumer

    /**
     * Collects metrics using the provided function into a temporary, non-thread-safe instance
     * of [StatisticsValuesConsumer], and then synchronizes the results into the primary [fusMetricsConsumer].
     */
    internal fun reportFusMetrics(reportAction: (StatisticsValuesConsumer) -> Unit) {
        val metricConsumer = NonSynchronizedMetricsContainer()
        reportAction(metricConsumer)
        fusMetricsConsumer.readFromMetricConsumer(metricConsumer)
    }

    private val projectEvaluatedTime: Long = System.currentTimeMillis()

    companion object {
        internal val serviceName = "${BuildFusService::class.simpleName}_${BuildFusService::class.java.classLoader.hashCode()}"
        private var buildStartTime: Long = System.currentTimeMillis()

        internal fun getBuildFusService(project: Project) =
            if (project.buildServiceShouldBeCreated) {
                project.gradle.sharedServices.registrations.findByName(serviceName).also {
                    if (it == null) {
                        project.logger.info("BuildFusService was not registered")
                    }
                }
            } else {
                null
            }


        fun registerIfAbsent(project: Project, pluginVersion: String, buildUidService: Provider<BuildUidService>) =
            if (project.buildServiceShouldBeCreated) {
                registerIfAbsentImpl(project, pluginVersion, buildUidService).also { serviceProvider ->
                    SingleActionPerProject.run(project, UsesBuildFusService::class.java.name) {
                        project.tasks.withType<UsesBuildFusService>().configureEach { task ->
                            task.buildFusService.value(serviceProvider).disallowChanges()
                            task.usesService(serviceProvider)
                        }
                    }
                }
            } else {
                null
            }

        private fun registerIfAbsentImpl(
            project: Project,
            pluginVersion: String,
            buildUidService: Provider<BuildUidService>,
        ): Provider<out BuildFusService<out Parameters>> {

            val isProjectIsolationEnabled = project.isProjectIsolationEnabled
            val isConfigurationCacheRequested = project.isConfigurationCacheRequested
            val isProjectIsolationRequested = project.isProjectIsolationRequested

            project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
                @Suppress("UNCHECKED_CAST")
                return (it.service as Provider<out BuildFusService<out Parameters>>)
            }

            //init buildStatsService
            KotlinBuildStatsBeanService.initStatsService(project)

            val buildReportOutputs = reportingSettings(project).buildReportOutputs
            val useClasspathSnapshot = PropertiesProvider(project).useClasspathSnapshot.get()
            val gradle = project.gradle
            val generalConfigurationMetricsProvider = project.provider {
                //isProjectIsolationEnabled isConfigurationCacheRequested and isProjectIsolationRequested should be calculated beforehand
                // because since Gradle 8.0 provider's calculation is made in BuildFinishFlowAction
                // and VariantImplementationFactories is not initialized at that moment
                collectGeneralConfigurationTimeMetrics(
                    project,
                    gradle,
                    buildReportOutputs,
                    useClasspathSnapshot,
                    pluginVersion,
                    isProjectIsolationEnabled,
                    isProjectIsolationRequested,
                    isConfigurationCacheRequested
                )
            }

            //Workaround for known issues for Gradle 8+: https://github.com/gradle/gradle/issues/24887:
            // when this OperationCompletionListener is called services can be already closed for Gradle 8,
            // so there is a change that no VariantImplementationFactory will be found
            val fusService = if (GradleVersion.current().baseVersion >= GradleVersion.version("8.9")) {
                FlowActionBuildFusService.registerIfAbsentImpl(project, buildUidService, generalConfigurationMetricsProvider)
            } else if (GradleVersion.current().baseVersion >= GradleVersion.version("8.1")) {
                ConfigurationMetricParameterFlowActionBuildFusService.registerIfAbsentImpl(
                    project,
                    buildUidService,
                    generalConfigurationMetricsProvider
                )
            } else {
                CloseActionBuildFusService.registerIfAbsentImpl(project, buildUidService, generalConfigurationMetricsProvider)
            }
            //DO NOT call buildService.get() before all parameters.configurationMetrics are set.
            // buildService.get() call will cause parameters calculation and configuration cache storage.

            BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(fusService)

            return fusService
        }
    }

    @Synchronized //access fusMetricsConsumer requires synchronisation as long as tasks are executed in parallel
    override fun onFinish(event: FinishEvent?) {
        if (event is TaskFinishEvent) {
            if (event.result is TaskFailureResult) {
                buildFailed = true
            }

            val taskExecutionResult = TaskExecutionResults[event.descriptor.taskPath]

            taskExecutionResult?.also { executionResult ->
                reportFusMetrics {
                    KotlinTaskExecutionMetrics.collectMetrics(executionResult, event, it)
                }
            }
        }
        reportFusMetrics {
            ExecutedTaskMetrics.collectMetrics(event, it)
        }
    }

    override fun close() {
        KotlinBuildStatsBeanService.closeServices()
        log.kotlinDebug("Close ${this.javaClass.simpleName}")
    }

    internal fun recordBuildFinished(buildFailed: Boolean, buildId: String, configurationMetrics: List<MetricContainer>) {
        BuildFinishMetrics.collectMetrics(log, buildFailed, buildStartTime, projectEvaluatedTime, fusMetricsConsumer)
        configurationMetrics.forEach { it.addToConsumer(fusMetricsConsumer) }
        parameters.generalConfigurationMetrics.orNull?.addToConsumer(fusMetricsConsumer)
        parameters.buildStatisticsConfiguration.orNull?.also {
            val loggerService = KotlinBuildStatsLoggerService(it)
            loggerService.initSessionLogger(buildId)
            loggerService.reportBuildFinished(fusMetricsConsumer)
        }
    }
}

class MetricContainer : Serializable {
    private val numericalMetrics = HashMap<NumericalMetrics, Long>()
    private val booleanMetrics = HashMap<BooleanMetrics, Boolean>()
    private val stringMetrics = HashMap<StringMetrics, String>()

    fun addToConsumer(metricsConsumer: StatisticsValuesConsumer) {
        for ((key, value) in numericalMetrics) {
            metricsConsumer.report(key, value)
        }
        for ((key, value) in booleanMetrics) {
            metricsConsumer.report(key, value)
        }
        for ((key, value) in stringMetrics) {
            metricsConsumer.report(key, value)
        }
    }

    fun put(metric: StringMetrics, value: String) = stringMetrics.put(metric, value)
    fun put(metric: BooleanMetrics, value: Boolean) = booleanMetrics.put(metric, value)
    fun put(metric: NumericalMetrics, value: Long) = numericalMetrics.put(metric, value)
}

private val Project.buildServiceShouldBeCreated
    get() = !isInIdeaSync.get() && kotlinPropertiesProvider.enableFusMetricsCollection

internal fun BuildFusService.Parameters.finalizeGeneralConfigurationMetrics() {
    if (generalMetricsFinalized.get()) return
    synchronized(this) {
        if (generalMetricsFinalized.get()) return
        generalMetricsFinalized.set(true)
        generalConfigurationMetrics.finalizeValue()
    }
}
