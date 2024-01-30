/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.plugin.statistics.plugins.ObservablePlugins
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import kotlin.system.measureTimeMillis

/**
 * Collect general configuration metrics
 **/
internal fun collectGeneralConfigurationTimeMetrics(
    gradle: Gradle,
    buildReportOutputs: List<BuildReportType>,
    useClasspathSnapshot: Boolean,
    pluginVersion: String,
    isProjectIsolationEnabled: Boolean,
): MetricContainer {
    val configurationTimeMetrics = MetricContainer()

    val statisticOverhead = measureTimeMillis {
        configurationTimeMetrics.put(StringMetrics.KOTLIN_COMPILER_VERSION, pluginVersion)
        configurationTimeMetrics.put(StringMetrics.USE_CLASSPATH_SNAPSHOT, useClasspathSnapshot.toString())
        buildReportOutputs.forEach {
            when (it) {
                BuildReportType.BUILD_SCAN -> configurationTimeMetrics.put(BooleanMetrics.BUILD_SCAN_BUILD_REPORT, true)
                BuildReportType.FILE -> configurationTimeMetrics.put(BooleanMetrics.FILE_BUILD_REPORT, true)
                BuildReportType.HTTP -> configurationTimeMetrics.put(BooleanMetrics.HTTP_BUILD_REPORT, true)
                BuildReportType.SINGLE_FILE -> configurationTimeMetrics.put(BooleanMetrics.SINGLE_FILE_BUILD_REPORT, true)
                BuildReportType.TRY_NEXT_CONSOLE -> {}//ignore
            }
        }
        configurationTimeMetrics.put(StringMetrics.PROJECT_PATH, gradle.rootProject.projectDir.absolutePath)
        configurationTimeMetrics.put(StringMetrics.GRADLE_VERSION, gradle.gradleVersion)

        //will be updated with KT-58266
        if (!isProjectIsolationEnabled) {
            gradle.taskGraph.whenReady { taskExecutionGraph ->
                val executedTaskNames = taskExecutionGraph.allTasks.map { it.name }.distinct()
                configurationTimeMetrics.put(BooleanMetrics.MAVEN_PUBLISH_EXECUTED, executedTaskNames.contains("install"))
            }
        }
    }
    configurationTimeMetrics.put(NumericalMetrics.STATISTICS_VISIT_ALL_PROJECTS_OVERHEAD, statisticOverhead)

    return configurationTimeMetrics

}

/**
 * Collect project's configuration metrics including applied plugins. It should be called inside afterEvaluate block.
 */
internal fun collectProjectConfigurationTimeMetrics(
    project: Project,
): MetricContainer {
    val configurationTimeMetrics = MetricContainer()

    val statisticOverhead = measureTimeMillis {
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
                    COMPILE_ONLY -> {
                        configurationTimeMetrics.put(NumericalMetrics.CONFIGURATION_COMPILE_ONLY_COUNT, 1)
                        reportLibrariesVersions(configurationTimeMetrics, dependencies)
                    }
                    RUNTIME -> {
                        configurationTimeMetrics.put(NumericalMetrics.CONFIGURATION_RUNTIME_COUNT, 1)
                        reportLibrariesVersions(configurationTimeMetrics, dependencies)
                    }
                    RUNTIME_ONLY -> {
                        configurationTimeMetrics.put(NumericalMetrics.CONFIGURATION_RUNTIME_ONLY_COUNT, 1)
                        reportLibrariesVersions(configurationTimeMetrics, dependencies)
                    }
                }
            } catch (e: Throwable) {
                // Ignore exceptions for FUS metrics
            }
        }

        configurationTimeMetrics.put(NumericalMetrics.NUMBER_OF_SUBPROJECTS, 1)


        configurationTimeMetrics.put(
            BooleanMetrics.KOTLIN_KTS_USED,
            project.buildscript.sourceFile?.name?.endsWith(".kts") ?: false
        )

        addTaskMetrics(project, configurationTimeMetrics)

        if (project.name == "buildSrc") {
            configurationTimeMetrics.put(NumericalMetrics.BUILD_SRC_COUNT, 1)
            configurationTimeMetrics.put(BooleanMetrics.BUILD_SRC_EXISTS, true)
        }
    }
    configurationTimeMetrics.put(NumericalMetrics.STATISTICS_VISIT_ALL_PROJECTS_OVERHEAD, statisticOverhead)

    return configurationTimeMetrics
}

private fun addTaskMetrics(
    project: Project,
    configurationTimeMetrics: MetricContainer,
) {
    try {
        val taskNames = project.tasks.names.toList()
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
    } catch (e: Exception) {
        //ignore exceptions for KT-62131.
    }
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

private fun reportLibrariesVersions(
    configurationTimeMetrics: MetricContainer,
    dependencies: DependencySet?,
) {
    dependencies?.filter { it !is ProjectDependency }?.forEach { dependency ->
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

