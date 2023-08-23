/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.plugin.statistics.plugins.ObservablePlugins
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.statistics.BuildSessionLogger
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import org.jetbrains.kotlin.statistics.MetricValueValidationFailed
import java.io.File
import java.lang.management.ManagementFactory
import javax.management.MBeanServer
import javax.management.ObjectName
import kotlin.system.measureTimeMillis

class KotlinBuildStatHandler {
    companion object {

        @JvmStatic
        internal fun getLogger() = Logging.getLogger(KotlinBuildStatHandler::class.java)

        internal fun <T> runSafe(methodName: String, action: () -> T?): T? {
            return try {
                getLogger().debug("Executing [$methodName]")
                action.invoke()
            } catch (e: MetricValueValidationFailed) {
                throw e
            } catch (e: Throwable) {
                logException("Could not execute [$methodName]", e)
                null
            }
        }

        internal fun logException(description: String, e: Throwable) {
            getLogger().info(description)
            getLogger().debug(e.message, e)
        }

    }

    fun buildFinished(
        beanName: ObjectName,
    ) {
        runSafe("${KotlinBuildStatHandler::class.java}.buildFinished") {
            val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
            if (mbs.isRegistered(beanName)) {
                mbs.unregisterMBean(beanName)
            }
        }
    }

    fun reportGlobalMetrics(
        sessionLogger: BuildSessionLogger,
    ) {
        runSafe("${KotlinBuildStatHandler::class.java}.reportGlobalMetrics") {
            System.getProperty("os.name")?.also { sessionLogger.report(StringMetrics.OS_TYPE, System.getProperty("os.name")) }
            sessionLogger.report(NumericalMetrics.CPU_NUMBER_OF_CORES, Runtime.getRuntime().availableProcessors().toLong())
            sessionLogger.report(BooleanMetrics.EXECUTED_FROM_IDEA, System.getProperty("idea.active") != null)
            sessionLogger.report(NumericalMetrics.GRADLE_DAEMON_HEAP_SIZE, Runtime.getRuntime().maxMemory())
        }
    }

    internal fun reportBuildFinished(
        sessionLogger: BuildSessionLogger,
        action: String?,
        buildFailed: Boolean,
        configurationMetrics: MetricContainer,
    ) {
        runSafe("${KotlinBuildStatHandler::class.java}.reportBuildFinish") {
            configurationMetrics.report(sessionLogger)
            sessionLogger.finishBuildSession(action, buildFailed)
        }
    }

    internal fun collectConfigurationTimeMetrics(
        project: Project,
        sessionLogger: BuildSessionLogger,
        isProjectIsolationEnabled: Boolean,
    ): MetricContainer {
        val gradle = project.gradle
        val configurationTimeMetrics = MetricContainer()
        configurationTimeMetrics.put(StringMetrics.PROJECT_PATH, gradle.rootProject.projectDir.absolutePath)
        configurationTimeMetrics.put(StringMetrics.GRADLE_VERSION, gradle.gradleVersion)

        if (isProjectIsolationEnabled) { //support project isolation - KT-58768
            return configurationTimeMetrics
        }

        gradle.taskGraph.whenReady { taskExecutionGraph ->
            val executedTaskNames = taskExecutionGraph.allTasks.map { it.name }.distinct()
            configurationTimeMetrics.put(BooleanMetrics.MAVEN_PUBLISH_EXECUTED, executedTaskNames.contains("install"))
        }// constants are saved in IDEA plugin and could not be accessed directly
        fun buildSrcExists(project: Project) = File(project.projectDir, "buildSrc").exists()
        configurationTimeMetrics.put(BooleanMetrics.BUILD_SRC_EXISTS, buildSrcExists(gradle.rootProject))

        val statisticOverhead = measureTimeMillis {
            gradle.allprojects { project ->
                collectAppliedPluginsStatistics(project, configurationTimeMetrics)

                val configurations = project.configurations.asMap.values
                for (configuration in configurations) {
                    try {
                        val configurationName = configuration.name
                        val dependencies = configuration.dependencies

                        when (configurationName) {
                            "KoverEngineConfig" -> {
                                configurationTimeMetrics.put(BooleanMetrics.ENABLED_KOVER, true)
                            }
                            "kapt" -> {
                                configurationTimeMetrics.put(BooleanMetrics.ENABLED_KAPT, true)
                                for (dependency in dependencies) {
                                    when (dependency.group) {
                                        "com.google.dagger" -> configurationTimeMetrics.put(BooleanMetrics.ENABLED_DAGGER, true)
                                        "com.android.databinding" -> configurationTimeMetrics.put(BooleanMetrics.ENABLED_DATABINDING, true)
                                    }
                                }
                            }
                            API -> {
                                configurationTimeMetrics.put(NumericalMetrics.CONFIGURATION_API_COUNT, 1)
                                reportLibrariesVersions(configurationTimeMetrics, dependencies)
                            }
                            IMPLEMENTATION -> {
                                configurationTimeMetrics.put(NumericalMetrics.CONFIGURATION_IMPLEMENTATION_COUNT, 1)
                                reportLibrariesVersions(configurationTimeMetrics, dependencies)
                            }
                            COMPILE -> {
                                configurationTimeMetrics.put(NumericalMetrics.CONFIGURATION_COMPILE_COUNT, 1)
                                reportLibrariesVersions(configurationTimeMetrics, dependencies)
                            }
                            RUNTIME -> {
                                configurationTimeMetrics.put(NumericalMetrics.CONFIGURATION_RUNTIME_COUNT, 1)
                                reportLibrariesVersions(configurationTimeMetrics, dependencies)
                            }
                        }
                    } catch (e: Throwable) {
                        // log?
                    }
                }
                val taskNames = project.tasks.names.toList()

                configurationTimeMetrics.put(NumericalMetrics.NUMBER_OF_SUBPROJECTS, 1)
                configurationTimeMetrics.put(
                    BooleanMetrics.KOTLIN_KTS_USED,
                    project.buildscript.sourceFile?.name?.endsWith(".kts") ?: false
                )
                configurationTimeMetrics.put(NumericalMetrics.GRADLE_NUMBER_OF_TASKS, taskNames.size.toLong())
                configurationTimeMetrics.put(
                    NumericalMetrics.GRADLE_NUMBER_OF_UNCONFIGURED_TASKS,
                    taskNames.count { name ->
                        try {
                            project.tasks.named(name).javaClass.name.contains("TaskCreatingProvider")
                        } catch (_: Exception) {
                            true
                        }
                    }.toLong()
                )

                if (project.name == "buildSrc") {
                    configurationTimeMetrics.put(NumericalMetrics.BUILD_SRC_COUNT, 1)
                    configurationTimeMetrics.put(BooleanMetrics.BUILD_SRC_EXISTS, true)
                }
            }
        }
        sessionLogger.report(NumericalMetrics.STATISTICS_VISIT_ALL_PROJECTS_OVERHEAD, statisticOverhead)

        return configurationTimeMetrics
    }

    private fun collectAppliedPluginsStatistics(
        project: Project,
        configurationTimeMetrics: MetricContainer,
    ) {
        for (plugin in ObservablePlugins.values()) {
            project.plugins.withId(plugin.title) {
                configurationTimeMetrics.put(plugin.metric, true)
            }
        }
    }

    private fun reportLibrariesVersions(configurationTimeMetrics: MetricContainer, dependencies: DependencySet?) {
        dependencies?.forEach { dependency ->
            when {
                dependency.group?.startsWith("org.springframework") ?: false -> configurationTimeMetrics.put(
                    StringMetrics.LIBRARY_SPRING_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group?.startsWith("com.vaadin") ?: false -> configurationTimeMetrics.put(
                    StringMetrics.LIBRARY_VAADIN_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group?.startsWith("com.google.gwt") ?: false -> configurationTimeMetrics.put(
                    StringMetrics.LIBRARY_GWT_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group?.startsWith("org.hibernate") ?: false -> configurationTimeMetrics.put(
                    StringMetrics.LIBRARY_HIBERNATE_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "org.jetbrains.kotlin" && dependency.name.startsWith("kotlin-stdlib") -> configurationTimeMetrics.put(
                    StringMetrics.KOTLIN_STDLIB_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "org.jetbrains.kotlinx" && dependency.name == "kotlinx-coroutines" -> configurationTimeMetrics.put(
                    StringMetrics.KOTLIN_COROUTINES_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "org.jetbrains.kotlin" && dependency.name == "kotlin-reflect" -> configurationTimeMetrics.put(
                    StringMetrics.KOTLIN_REFLECT_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "org.jetbrains.kotlinx" && dependency.name
                    .startsWith("kotlinx-serialization-runtime") -> configurationTimeMetrics.put(
                    StringMetrics.KOTLIN_SERIALIZATION_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "com.android.tools.build" && dependency.name.startsWith("gradle") -> configurationTimeMetrics.put(
                    StringMetrics.ANDROID_GRADLE_PLUGIN_VERSION,
                    dependency.version ?: "0.0.0"
                )
            }
        }
    }

    internal fun report(
        sessionLogger: BuildSessionLogger,
        metric: BooleanMetrics,
        value: Boolean,
        subprojectName: String?,
        weight: Long? = null,
    ) = runSafe("report metric ${metric.name}") {
        sessionLogger.report(metric, value, subprojectName, weight)
    } ?: false

    internal fun report(
        sessionLogger: BuildSessionLogger,
        metric: NumericalMetrics,
        value: Long,
        subprojectName: String?,
        weight: Long? = null,
    ) = runSafe("report metric ${metric.name}") {
        sessionLogger.report(metric, value, subprojectName, weight)
    } ?: false

    internal fun report(
        sessionLogger: BuildSessionLogger,
        metric: StringMetrics,
        value: String,
        subprojectName: String?,
        weight: Long? = null,
    ) = runSafe("report metric ${metric.name}") {
        sessionLogger.report(metric, value, subprojectName, weight)
    } ?: false
}
