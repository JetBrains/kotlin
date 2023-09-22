/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.services


import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.withType
import java.io.File
import java.util.UUID


interface UsesGradleBuildFusStatisticsService : Task {
    @get:Internal
    val fusStatisticsBuildService: Property<GradleBuildFusStatisticsService?>
}

internal abstract class InternalGradleBuildFusStatisticsService : GradleBuildFusStatisticsService,
    BuildService<InternalGradleBuildFusStatisticsService.Parameters>, AutoCloseable {

    interface Parameters : BuildServiceParameters {
        val path: Property<String>
        val uuid: Property<String>
    }

    private val metrics = HashMap<Metric, Any>()

    override fun close() {
        val reportFile = File(parameters.path.get())
            .resolve(STATISTICS_FOLDER_NAME)
            .also { it.mkdirs() }
            .resolve(parameters.uuid.get())
        reportFile.createNewFile()

        for ((metric, value) in metrics) {
            reportFile.appendText("$metric=$value\n")
        }

        reportFile.appendText(BUILD_SESSION_SEPARATOR)
    }

    override fun reportMetric(name: String, value: Any, subprojectName: String?) {
        metrics[Metric(name, subprojectName)] = value
    }

    companion object {
        private const val FUS_STATISTICS_PATH = "kotlin.fus.statistics.path"
        private const val STATISTICS_FOLDER_NAME = "kotlin-fus"

        private const val BUILD_SESSION_SEPARATOR = "BUILD FINISHED"

        private var statisticsIsEnabled: Boolean = true //KT-59629 Wait for user confirmation before start to collect metrics
        private val serviceClass = InternalGradleBuildFusStatisticsService::class.java
        private val serviceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"

        fun registerIfAbsent(project: Project): Provider<InternalGradleBuildFusStatisticsService> {
            project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
                @Suppress("UNCHECKED_CAST")
                return it.service as Provider<InternalGradleBuildFusStatisticsService>
            }

            return if (statisticsIsEnabled) {
                project.gradle.sharedServices.registerIfAbsent(serviceName, serviceClass) {
                    val customPath: String = if (project.rootProject.hasProperty(FUS_STATISTICS_PATH)) {
                        project.rootProject.property(FUS_STATISTICS_PATH) as String
                    } else {
                        project.gradle.gradleUserHomeDir.path //fix
                    }
                    it.parameters.path.set(customPath)
                    it.parameters.uuid.set(UUID.randomUUID().toString())
                }
            } else {
                project.gradle.sharedServices.registerIfAbsent(serviceName, DummyGradleBuildFusStatisticsService::class.java) {}
                    .map { it as InternalGradleBuildFusStatisticsService }
            }.also { configureTasks(project, it) }
        }

        private fun configureTasks(project: Project, serviceProvider: Provider<InternalGradleBuildFusStatisticsService>) {
            project.tasks.withType<UsesGradleBuildFusStatisticsService>().configureEach { task ->
                task.fusStatisticsBuildService.value(serviceProvider).disallowChanges()
                task.usesService(serviceProvider)
            }
        }
    }
}