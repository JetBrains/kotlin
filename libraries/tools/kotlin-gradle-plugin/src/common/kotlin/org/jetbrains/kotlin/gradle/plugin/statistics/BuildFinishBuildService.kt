/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.fus.BuildUidService
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.reportErrors
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.kotlinErrorsDir
import org.jetbrains.kotlin.statistics.fileloggers.MetricsContainer
import java.io.File
import kotlin.String

abstract class BuildFinishBuildService : BuildService<BuildFinishBuildService.Parameters>, AutoCloseable {
    protected val buildId = parameters.buildId.get()
    private val log = Logging.getLogger(this.javaClass)

    interface Parameters : BuildServiceParameters {
        val buildId: Property<String>
        val fusReportDirectory: Property<String> //DirectoryProperty
        val kotlinVersion: Property<String>
        val errorDirs: ListProperty<File>
        val closeActionShouldBeRun: Property<Boolean>
    }

    companion object {
        private val serviceName =
            "${BuildFinishBuildService::class.java.canonicalName}_${BuildFinishBuildService::class.java.classLoader.hashCode()}"

        fun registerIfAbsent(project: Project, buildUidService: Provider<BuildUidService>, kotlinPluginVersion: String) {
            val reportDir = project.getFusDirectoryFromPropertyService()
            val useFlowAction = GradleVersion.current().baseVersion >= GradleVersion.version("8.1")

            project.gradle.sharedServices.registerIfAbsent(serviceName, BuildFinishBuildService::class.java) { spec ->
                spec.parameters.buildId.value(buildUidService.map { it.buildId }).disallowChanges()
                spec.parameters.fusReportDirectory.value(reportDir.absolutePath).disallowChanges()
                spec.parameters.errorDirs.add(project.kotlinErrorsDir)
                if (!project.kotlinPropertiesProvider.kotlinProjectPersistentDirGradleDisableWrite) {
                    spec.parameters.errorDirs.add(project.rootDir.resolve(".gradle/kotlin/errors/"))
                }
                spec.parameters.errorDirs.disallowChanges()
                spec.parameters.kotlinVersion.value(kotlinPluginVersion).disallowChanges()
                spec.parameters.closeActionShouldBeRun.value(!useFlowAction).disallowChanges()
            }.get()

            if (useFlowAction) {
                BuildFinishFlowProviderManager.getInstance(project).subscribeForBuildResults()
            }
        }

        internal fun collectAllFusReportsIntoOne(buildUid: String, fusReportDirectory: String, log: Logger): List<String> {
            try {
                val fusDirectory = File(fusReportDirectory)
                val metricContainer = MetricsContainer()

                if (!File(fusDirectory, "$buildUid.finish-profile").createNewFile()) {
                    return listOf("File $fusDirectory/$buildUid.finish-profile already exists")
                }

                fusDirectory.listFiles().filter { it.name.startsWith(buildUid) }.forEach {
                    MetricsContainer.readFromFile(it) {
                        metricContainer.addAll(it)
                    }
                }

                val fusFile = fusDirectory.resolve("$buildUid.profile")
                fusFile.writer().buffered().use { metricContainer.flush(it) }
                fusFile.createNewFile()

            } catch (e: Exception) {
                return listOf("Error while creating finish file: ${e.message}" + e.stackTrace.joinToString("\n"))
            }
            log.debug("Single fus file was created for build $buildUid ")
            return emptyList()
        }

    }

    override fun close() {
        log.info("Build service $serviceName closed for build $buildId")
        if (parameters.closeActionShouldBeRun.get()) {
            collectAllFusReportsIntoOne()
        }
    }

    private fun File.errorFile() = resolve("errors-$buildId-${System.currentTimeMillis()}.log")

    internal fun collectAllFusReportsIntoOne() {
        val errorMessages = collectAllFusReportsIntoOne(buildId, parameters.fusReportDirectory.get(), log)
        reportErrors(
            errorMessages, parameters.errorDirs.get().map { it.errorFile() }, parameters.kotlinVersion.get(), buildId,
            GradleKotlinLogger(log)
        )
    }
}