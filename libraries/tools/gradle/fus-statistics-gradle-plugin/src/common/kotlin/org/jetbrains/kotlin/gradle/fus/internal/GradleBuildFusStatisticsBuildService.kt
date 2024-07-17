/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService
import org.jetbrains.kotlin.gradle.fus.UsesGradleBuildFusStatisticsService

abstract class GradleBuildFusStatisticsBuildService : GradleBuildFusStatisticsService,
    BuildService<GradleBuildFusStatisticsBuildService.Parameters>, AutoCloseable {
    interface Parameters : BuildServiceParameters {
        val fusStatisticsRootDirPath: Property<String>
        val configurationMetrics: ListProperty<Map<String, Any>>
        val fusStatisticIsEnabled: Property<Boolean>
        val useBuildFinishFlowAction: Property<Boolean>
    }

    companion object {
        private var statisticsIsEnabled: Boolean = true //KT-59629 Wait for user confirmation before start to collect metrics
        private const val FUS_STATISTICS_PATH = "kotlin.fus.statistics.path"
        private val serviceClass = GradleBuildFusStatisticsBuildService::class.java
        private val serviceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"
        fun registerIfAbsent(project: Project): Provider<out GradleBuildFusStatisticsService> {
            project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
                @Suppress("UNCHECKED_CAST")
                return it.service as Provider<GradleBuildFusStatisticsService>
            }

            return (if (statisticsIsEnabled) {
                project.gradle.sharedServices.registerIfAbsent(serviceName, InternalGradleBuildFusStatisticsService::class.java) {
                    val customPath: String = if (project.rootProject.hasProperty(FUS_STATISTICS_PATH)) {
                        project.rootProject.property(FUS_STATISTICS_PATH) as String
                    } else {
                        project.gradle.gradleUserHomeDir.path
                    }
                    it.parameters.fusStatisticsRootDirPath.set(customPath)
                    it.parameters.fusStatisticIsEnabled.set(statisticsIsEnabled)
                    it.parameters.configurationMetrics.add(emptyMap())
                    it.parameters.useBuildFinishFlowAction.set(GradleVersion.current().baseVersion >= GradleVersion.version("8.1"))
                }
            } else {
                project.gradle.sharedServices.registerIfAbsent(serviceName, DummyGradleBuildFusStatisticsService::class.java) {
                    it.parameters.fusStatisticIsEnabled.set(statisticsIsEnabled)
                    it.parameters.useBuildFinishFlowAction.set(GradleVersion.current().baseVersion >= GradleVersion.version("8.1"))
                }
            }).also { configureTasks(project, it) }
        }

        private fun configureTasks(project: Project, serviceProvider: Provider<out GradleBuildFusStatisticsBuildService>) {
            project.tasks.withType(UsesGradleBuildFusStatisticsService::class.java).configureEach { task ->
                task.fusStatisticsBuildService.value(serviceProvider).disallowChanges()
                task.usesService(serviceProvider)
            }
        }

        /**
         * Invokes [GradleBuildFusStatisticsBuildService] if the reporting service is initialized and add configuration time metrics.
         *
         * New value will be present in configuration cache if GradleBuildFusStatisticsBuildService.Parameters are not calculated yet
         * [GradleBuildFusStatisticsBuildService.reportMetric] should be called for execution time metrics
         */
        internal fun Project.addConfigurationMetric(reportAction: () -> Map<String, Any>) {
            project.gradle.sharedServices.registrations.findByName(serviceName)?.also {
                val parameters = it.parameters as Parameters
                parameters.configurationMetrics.add(project.provider {
                    reportAction()
                })
            }
        }
    }
}