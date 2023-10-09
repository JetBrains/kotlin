/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.services


import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal abstract class InternalGradleBuildFusStatisticsService : GradleBuildFusStatisticsService,
    BuildService<InternalGradleBuildFusStatisticsService.Parameters>, AutoCloseable {

    interface Parameters : BuildServiceParameters {
        val fusStatisticsRootDirPath: Property<String>
        val buildId: Property<String>
    }

    private val metrics = ConcurrentHashMap<Metric, Any>()
    private val log = Logging.getLogger(this.javaClass)

    override fun close() {
        val reportFile = File(parameters.fusStatisticsRootDirPath.get(), STATISTICS_FOLDER_NAME)
            .also { Files.createDirectories(it.toPath()) }
            .resolve(parameters.buildId.get())
        reportFile.createNewFile()

        for ((metric, value) in metrics) {
            reportFile.appendText("$metric=$value\n")
        }

        reportFile.appendText(BUILD_SESSION_SEPARATOR)
    }

    override fun reportMetric(name: String, value: Any, subprojectName: String?) {
        val oldValue = metrics.getOrPut(Metric(name, subprojectName)) { value }
        if (oldValue != value) {
            log.warn("Try to override $name metric: current value is \"$oldValue\", new value is \"$value\"")
        }
    }

    companion object {
        private const val FUS_STATISTICS_PATH = "kotlin.fus.statistics.path"
        private const val STATISTICS_FOLDER_NAME = "kotlin-fus"

        private const val BUILD_SESSION_SEPARATOR = "BUILD FINISHED"

        private var statisticsIsEnabled: Boolean = true //KT-59629 Wait for user confirmation before start to collect metrics
        private val serviceClass = InternalGradleBuildFusStatisticsService::class.java
        private val serviceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"

        fun registerIfAbsent(project: Project): Provider<out GradleBuildFusStatisticsService>? {
            project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
                @Suppress("UNCHECKED_CAST")
                return it.service as Provider<GradleBuildFusStatisticsService>
            }

            return (if (statisticsIsEnabled) {
                project.gradle.sharedServices.registerIfAbsent(serviceName, serviceClass) {
                    val customPath: String = if (project.rootProject.hasProperty(FUS_STATISTICS_PATH)) {
                        project.rootProject.property(FUS_STATISTICS_PATH) as String
                    } else {
                        project.gradle.gradleUserHomeDir.path
                    }
                    it.parameters.fusStatisticsRootDirPath.set(customPath)
                    it.parameters.buildId.set(UUID.randomUUID().toString())
                }
            } else {
                project.gradle.sharedServices.registerIfAbsent(serviceName, DummyGradleBuildFusStatisticsService::class.java) {}
            }).also { configureTasks(project, it) }
        }

        private fun configureTasks(project: Project, serviceProvider: Provider<out InternalGradleBuildFusStatisticsService>) {
            project.tasks.withType(UsesGradleBuildFusStatisticsService::class.java).configureEach { task ->
                task.fusStatisticsBuildService.value(serviceProvider).disallowChanges()
                task.usesService(serviceProvider)
            }
        }
    }
}