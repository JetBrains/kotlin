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
import org.jetbrains.kotlin.gradle.logging.Errors
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.reportToIde
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.kotlinErrorsDir
import org.jetbrains.kotlin.statistics.fileloggers.MetricsContainer
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.String

internal abstract class BuildFinishBuildService : BuildService<BuildFinishBuildService.Parameters>, AutoCloseable {
    protected val buildId = parameters.buildId.get()
    private val log = Logging.getLogger(this.javaClass)
    private val errorWasReported = AtomicBoolean(false)

    interface Parameters : BuildServiceParameters {
        val buildId: Property<String>
        val fusReportDirectory: Property<File>
        val kotlinVersion: Property<String>
        val errorDirs: ListProperty<File>
    }

    companion object {
        private val serviceName =
            "${BuildFinishBuildService::class.java.canonicalName}_${BuildFinishBuildService::class.java.classLoader.hashCode()}"

        fun registerIfAbsent(project: Project, buildUidService: Provider<BuildUidService>, kotlinPluginVersion: String) {
            if (!project.buildServiceShouldBeCreated) {
                return
            }

            if (project.gradle.sharedServices.registrations.findByName(serviceName) != null) {
                return
            }

            val reportDir = project.getFusDirectoryFromPropertyService()
            val useFlowAction = GradleVersion.current().baseVersion >= GradleVersion.version("8.1")

            project.gradle.sharedServices.registerIfAbsent(serviceName, BuildFinishBuildService::class.java) { spec ->
                spec.parameters.buildId.value(buildUidService.map { it.buildId }).disallowChanges()
                spec.parameters.fusReportDirectory.value(reportDir).disallowChanges()
                spec.parameters.errorDirs.add(project.kotlinErrorsDir)
                if (!project.kotlinPropertiesProvider.kotlinProjectPersistentDirGradleDisableWrite) {
                    spec.parameters.errorDirs.add(project.rootDir.resolve(".gradle/kotlin/errors/"))
                }
                spec.parameters.errorDirs.disallowChanges()
                spec.parameters.kotlinVersion.value(kotlinPluginVersion).disallowChanges()
            }.get()

            if (useFlowAction) { //otherwise CloseActionBuildFusService.close() will aggregate fus metrics into single file
                BuildFinishFlowProviderManager.getInstance(project).subscribeForBuildResults()
            }
        }

        internal fun collectAllFusReportsIntoOne(
            buildUid: String,
            fusReportDirectory: File,
            kotlinVersion: String,
            log: Logger,
        ): Errors {
            try {
                val metricContainer = MetricsContainer()

                fusReportDirectory.listFiles()
                    .filter { it.name.startsWith(buildUid) && (it.name.endsWith("plugin-profile") || it.name.endsWith("kotlin-profile")) }
                    .forEach {
                        MetricsContainer.readFromFile(it) {
                            metricContainer.populateFromMetricsContainer(it)
                        }
                    }

                val fusFile = fusReportDirectory.resolve("$buildUid.profile")
                fusFile.writer().buffered().use {
                    it.appendLine("Build: $buildUid")
                    it.appendLine("Kotlin version: $kotlinVersion")
                    metricContainer.flush(it)
                }

                if (!fusReportDirectory.resolve("$buildUid.finish-profile").createNewFile()) {
                    log.debug("File $fusReportDirectory/$buildUid.finish-profile already exists")
                    return listOf("File $fusReportDirectory/$buildUid.finish-profile already exists")
                }

            } catch (e: Exception) {
                log.debug("Unable to collect finish file for build $buildUid: ${e.message}")
                return listOf("Error while creating finish file: ${e.message}" + e.stackTrace.joinToString("\n"))
            }
            log.debug("Single fus file was created for build $buildUid ")
            return emptyList()
        }
    }

    override fun close() {
        log.debug("Build service $serviceName closed for build $buildId")
    }

    private fun File.errorFile() = resolve("errors-$buildId-${System.currentTimeMillis()}.log")

    internal fun collectAllFusReportsIntoOne() {
        val errorMessages = collectAllFusReportsIntoOne(buildId, parameters.fusReportDirectory.get(), parameters.kotlinVersion.get(), log)

        //KT-79408 skip reporting to IDE if there is already a reported fus related error file with the same buildId
        if (errorMessages.isNotEmpty()) {
            if (errorWasReported.compareAndSet(false, true)) {
                errorMessages.reportToIde(
                    parameters.errorDirs.get().map { it.errorFile() }, parameters.kotlinVersion.get(), buildId,
                    GradleKotlinLogger(log)
                )
            }
        }
    }
}