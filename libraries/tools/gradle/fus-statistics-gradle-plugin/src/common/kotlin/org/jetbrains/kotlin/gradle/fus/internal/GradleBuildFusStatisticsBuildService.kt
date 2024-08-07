/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService
import org.jetbrains.kotlin.gradle.fus.UsesGradleBuildFusStatisticsService

abstract class GradleBuildFusStatisticsBuildService : GradleBuildFusStatisticsService, AutoCloseable {

    companion object {
        private var statisticsIsEnabled: Boolean = true //KT-59629 Wait for user confirmation before start to collect metrics
        private const val FUS_STATISTICS_PATH = "kotlin.fus.statistics.path"
        private val serviceClass = GradleBuildFusStatisticsBuildService::class.java
        internal val serviceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"
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
                project.gradle.sharedServices.registerIfAbsent(serviceName, NoConsentGradleBuildFusService::class.java) {
                }
            }).also { service ->
                project.tasks.withType(UsesGradleBuildFusStatisticsService::class.java).configureEach { task ->
                    task.fusStatisticsBuildService.value(service).disallowChanges()
                    task.usesService(service)
                }
            }
        }
    }
}

