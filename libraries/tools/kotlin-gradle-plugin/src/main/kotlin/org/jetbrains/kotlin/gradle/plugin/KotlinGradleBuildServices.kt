/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import com.gradle.scan.plugin.BuildScanExtension
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskLoggers
import org.jetbrains.kotlin.gradle.plugin.stat.ReportStatistics
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildEsStatListener
import org.jetbrains.kotlin.gradle.plugin.statistics.ReportStatisticsToBuildScan
import org.jetbrains.kotlin.gradle.plugin.statistics.ReportStatisticsToElasticSearch
import org.jetbrains.kotlin.gradle.report.configureReporting
import java.io.File

abstract class KotlinGradleBuildServices : BuildService<KotlinGradleBuildServices.Parameters>, AutoCloseable {

    interface Parameters : BuildServiceParameters {
        var buildDir: File
        var rootDir: File
    }

    private val log = Logging.getLogger(this.javaClass)
    private var buildHandler: KotlinGradleFinishBuildHandler = KotlinGradleFinishBuildHandler()
    private val CLASS_NAME = KotlinGradleBuildServices::class.java.simpleName
    val INIT_MESSAGE = "Initialized $CLASS_NAME"
    val DISPOSE_MESSAGE = "Disposed $CLASS_NAME"

    init {
        log.kotlinDebug(INIT_MESSAGE)
        buildHandler.buildStart()
    }

    override fun close() {
        buildHandler.buildFinished(parameters.buildDir, parameters.rootDir)
        log.kotlinDebug(DISPOSE_MESSAGE)

        TaskLoggers.clear()
        TaskExecutionResults.clear()
    }

    companion object {

        fun registerIfAbsent(project: Project): Provider<KotlinGradleBuildServices> = project.gradle.sharedServices.registerIfAbsent(
            "kotlin-build-service-${KotlinGradleBuildServices::class.java.canonicalName}_${KotlinGradleBuildServices::class.java.classLoader.hashCode()}",
            KotlinGradleBuildServices::class.java
        ) { service ->
            configureReporting(project.gradle)
            service.parameters.rootDir = project.rootProject.rootDir
            service.parameters.buildDir = project.rootProject.buildDir
            addListeners(project)
        }

        fun addListeners(project: Project) {
            val kotlinGradleEsListenerProvider = project.provider {
                val listeners = project.rootProject.objects.listProperty(ReportStatistics::class.java)
                    .value(listOf<ReportStatistics>(ReportStatisticsToElasticSearch))
                if (project.gradle.startParameter.isBuildScan) {
                    project.rootProject.extensions.findByName("buildScan")
                        ?.also { listeners.add(ReportStatisticsToBuildScan(it as BuildScanExtension)) }
                }
                KotlinBuildEsStatListener(project.rootProject.name, listeners.get())
            }

            val listenerRegistryHolder = BuildEventsListenerRegistryHolder.getInstance(project)

            listenerRegistryHolder.listenerRegistry.onTaskCompletion(kotlinGradleEsListenerProvider)
        }

        private val multipleProjectsHolder = KotlinPluginInMultipleProjectsHolder(
            trackPluginVersionsSeparately = true
        )

        @Synchronized
        internal fun detectKotlinPluginLoadedInMultipleProjects(project: Project, kotlinPluginVersion: String) {
            val onRegister = {
                project.gradle.taskGraph.whenReady {
                    if (multipleProjectsHolder.isInMultipleProjects(project, kotlinPluginVersion)) {
                        val loadedInProjects = multipleProjectsHolder.getAffectedProjects(project, kotlinPluginVersion)!!
                        if (PropertiesProvider(project).ignorePluginLoadedInMultipleProjects != true) {
                            project.logger.warn("\n$MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING")
                            project.logger.warn(
                                MULTIPLE_KOTLIN_PLUGINS_SPECIFIC_PROJECTS_WARNING + loadedInProjects.joinToString(limit = 4) { "'$it'" }
                            )
                        }
                        project.logger.info(
                            "$MULTIPLE_KOTLIN_PLUGINS_SPECIFIC_PROJECTS_INFO: " +
                                    loadedInProjects.joinToString { "'$it'" }
                        )
                    }
                }
            }

            multipleProjectsHolder.addProject(
                project,
                kotlinPluginVersion,
                onRegister
            )
        }
    }
}


