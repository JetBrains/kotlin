/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.utils.API
import org.jetbrains.kotlin.gradle.utils.COMPILE
import org.jetbrains.kotlin.gradle.utils.IMPLEMENTATION
import org.jetbrains.kotlin.gradle.utils.RUNTIME
import org.jetbrains.kotlin.statistics.BuildSessionLogger
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
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
        gradle: Gradle?,
        beanName: ObjectName,
        sessionLogger: BuildSessionLogger,
        action: String?,
        failure: Throwable?
    ) {
        runSafe("${KotlinBuildStatHandler::class.java}.buildFinished") {
            try {
                try {
                    if (gradle != null) reportGlobalMetrics(gradle, sessionLogger)
                } finally {
                    sessionLogger.finishBuildSession(action, failure)
                }
            } finally {
                val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
                if (mbs.isRegistered(beanName)) {
                    mbs.unregisterMBean(beanName)
                }
            }
        }
    }

    internal fun reportGlobalMetrics(gradle: Gradle, sessionLogger: BuildSessionLogger) {
        sessionLogger.report(StringMetrics.PROJECT_PATH, gradle.rootProject.projectDir.absolutePath)
        System.getProperty("os.name")?.also { sessionLogger.report(StringMetrics.OS_TYPE, System.getProperty("os.name")) }
        sessionLogger.report(NumericalMetrics.CPU_NUMBER_OF_CORES, Runtime.getRuntime().availableProcessors().toLong())
        sessionLogger.report(StringMetrics.GRADLE_VERSION, gradle.gradleVersion)
        sessionLogger.report(BooleanMetrics.EXECUTED_FROM_IDEA, System.getProperty("idea.active") != null)
        sessionLogger.report(NumericalMetrics.GRADLE_DAEMON_HEAP_SIZE, Runtime.getRuntime().maxMemory())
        sessionLogger.report(
            BooleanMetrics.KOTLIN_OFFICIAL_CODESTYLE,
            gradle.rootProject.properties["kotlin.code.style"] == "official"
        ) // constants are saved in IDEA plugin and could not be accessed directly

        gradle.taskGraph.whenReady() { taskExecutionGraph ->
            val executedTaskNames = taskExecutionGraph.allTasks.map { it.name }.distinct()
            report(sessionLogger, BooleanMetrics.MAVEN_PUBLISH_EXECUTED, executedTaskNames.contains("install"), null)
        }

        fun buildSrcExists(project: Project) = File(project.projectDir, "buildSrc").exists()
        sessionLogger.report(BooleanMetrics.BUILD_SRC_EXISTS, buildSrcExists(gradle.rootProject))

        val statisticOverhead = measureTimeMillis {
            gradle.allprojects { project ->
                for (configuration in project.configurations) {
                    val configurationName = configuration.name
                    val dependencies = configuration.dependencies

                    when (configurationName) {
                        "kapt" -> {
                            sessionLogger.report(BooleanMetrics.ENABLED_KAPT, true)
                            for (dependency in dependencies) {
                                when (dependency.group) {
                                    "com.google.dagger" -> sessionLogger.report(BooleanMetrics.ENABLED_DAGGER, true)
                                    "com.android.databinding" -> sessionLogger.report(BooleanMetrics.ENABLED_DATABINDING, true)
                                }
                            }
                        }
                        API -> {
                            sessionLogger.report(NumericalMetrics.CONFIGURATION_API_COUNT, 1)
                            reportLibrariesVersions(sessionLogger, dependencies)
                        }
                        IMPLEMENTATION -> {
                            sessionLogger.report(NumericalMetrics.CONFIGURATION_IMPLEMENTATION_COUNT, 1)
                            reportLibrariesVersions(sessionLogger, dependencies)
                        }
                        COMPILE -> {
                            sessionLogger.report(NumericalMetrics.CONFIGURATION_COMPILE_COUNT, 1)
                            reportLibrariesVersions(sessionLogger, dependencies)
                        }
                        RUNTIME -> {
                            sessionLogger.report(NumericalMetrics.CONFIGURATION_RUNTIME_COUNT, 1)
                            reportLibrariesVersions(sessionLogger, dependencies)
                        }
                    }
                }
                val taskNames = project.tasks.names

                sessionLogger.report(NumericalMetrics.NUMBER_OF_SUBPROJECTS, 1)
                sessionLogger.report(BooleanMetrics.KOTLIN_KTS_USED, project.buildscript.sourceFile?.name?.endsWith(".kts") ?: false)
                sessionLogger.report(NumericalMetrics.GRADLE_NUMBER_OF_TASKS, taskNames.size.toLong())
                sessionLogger.report(
                    NumericalMetrics.GRADLE_NUMBER_OF_UNCONFIGURED_TASKS,
                    taskNames.count { name ->
                        try {
                            project.tasks.named(name).javaClass.name.contains("TaskCreatingProvider")
                        } catch (_: Exception) {
                            true
                        }
                    }.toLong()
                )

                if (buildSrcExists(project)) {
                    sessionLogger.report(NumericalMetrics.BUILD_SRC_COUNT, 1)
                    sessionLogger.report(BooleanMetrics.BUILD_SRC_EXISTS, true)
                }
            }
        }
        sessionLogger.report(NumericalMetrics.STATISTICS_VISIT_ALL_PROJECTS_OVERHEAD, statisticOverhead)
    }

    private fun reportLibrariesVersions(sessionLogger: BuildSessionLogger, dependencies: DependencySet?) {
        dependencies?.forEach { dependency ->
            when {
                dependency.group?.startsWith("org.springframework") ?: false -> sessionLogger.report(
                    StringMetrics.LIBRARY_SPRING_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group?.startsWith("com.vaadin") ?: false -> sessionLogger.report(
                    StringMetrics.LIBRARY_VAADIN_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group?.startsWith("com.google.gwt") ?: false -> sessionLogger.report(
                    StringMetrics.LIBRARY_GWT_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group?.startsWith("org.hibernate") ?: false -> sessionLogger.report(
                    StringMetrics.LIBRARY_HIBERNATE_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "org.jetbrains.kotlin" && dependency.name.startsWith("kotlin-stdlib") -> sessionLogger.report(
                    StringMetrics.KOTLIN_STDLIB_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "org.jetbrains.kotlinx" && dependency.name == "kotlinx-coroutines" -> sessionLogger.report(
                    StringMetrics.KOTLIN_COROUTINES_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "org.jetbrains.kotlin" && dependency.name == "kotlin-reflect" -> sessionLogger.report(
                    StringMetrics.KOTLIN_REFLECT_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "org.jetbrains.kotlinx" && dependency.name
                    .startsWith("kotlinx-serialization-runtime") -> sessionLogger.report(
                    StringMetrics.KOTLIN_SERIALIZATION_VERSION,
                    dependency.version ?: "0.0.0"
                )
                dependency.group == "com.android.tools.build" && dependency.name.startsWith("gradle") -> sessionLogger.report(
                    StringMetrics.ANDROID_GRADLE_PLUGIN_VERSION,
                    dependency.version ?: "0.0.0"
                )
            }
        }
    }

    internal fun report(sessionLogger: BuildSessionLogger, metric: BooleanMetrics, value: Boolean, subprojectName: String?) {
        runSafe("report metric ${metric.name}") {
            sessionLogger.report(metric, value, subprojectName)
        }

    }

    internal fun report(sessionLogger: BuildSessionLogger, metric: NumericalMetrics, value: Long, subprojectName: String?) {
        runSafe("report metric ${metric.name}") {
            sessionLogger.report(metric, value, subprojectName)
        }
    }

    internal fun report(sessionLogger: BuildSessionLogger, metric: StringMetrics, value: String, subprojectName: String?) {
        runSafe("report metric ${metric.name}") {
            sessionLogger.report(metric, value, subprojectName)
        }
    }
}
