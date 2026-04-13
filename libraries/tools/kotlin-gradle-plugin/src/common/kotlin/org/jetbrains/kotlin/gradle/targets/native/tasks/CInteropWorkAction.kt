/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.file.RegularFileProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerArgumentsLogLevel
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.utils.JsonUtils
import org.jetbrains.kotlin.internal.compilerRunner.native.KotlinNativeCInteropRunner
import org.jetbrains.kotlin.internal.compilerRunner.native.KotlinNativeToolRunner
import java.io.File
import javax.inject.Inject

internal interface CInteropWorkParameters : WorkParameters {
    val arguments: ListProperty<String>
    val outputFile: Property<File>
    val errorFile: Property<File>
    val ideaSyncEnabled: Property<Boolean>
    val taskPath: Property<String>
    val compilerArgumentsLogLevel: Property<KotlinCompilerArgumentsLogLevel>
    val allHeadersHashesFile: RegularFileProperty
    val headerHashMap: MapProperty<String, String>

    // Runner configuration
    val metricsReporter: Property<BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric>>
    val classLoadersCachingService: Property<ClassLoadersCachingBuildService>
    val konanPropertiesService: Property<KonanPropertiesBuildService>
    val actualNativeHomeDirectory: Property<File>
    val runnerJvmArgs: ListProperty<String>
    val useXcodeMessageStyle: Property<Boolean>
}

/**
 * Enables parallel execution of independent CInteropProcess tasks, which is critical for
 * multiplatform projects with many native targets where cinterop is a build bottleneck.
 */
internal abstract class CInteropWorkAction @Inject constructor(
    private val objectFactory: ObjectFactory,
) : WorkAction<CInteropWorkParameters> {

    private val logger = Logging.getLogger(CInteropWorkAction::class.java)

    override fun execute() {
        // TODO: Report this WorkParameters generic-erasure issue to Gradle if it is not tracked yet.
        // runnerJvmArgs reaches us as Provider<List<Object>>, which breaks
        // KotlinNativeCInteropRunner's internal listProperty<String>().value(provider) call.
        val runner = objectFactory.KotlinNativeCInteropRunner(
            parameters.metricsReporter,
            parameters.classLoadersCachingService,
            parameters.actualNativeHomeDirectory,
            parameters.runnerJvmArgs.map { it },
            parameters.useXcodeMessageStyle,
            parameters.konanPropertiesService,
        )

        val outputFile = parameters.outputFile.get()
        outputFile.parentFile.mkdirs()

        val errorFile = parameters.errorFile.get()
        errorFile.delete()

        val toolArguments = KotlinNativeToolRunner.ToolArguments(
            shouldRunInProcessMode = false,
            compilerArgumentsLogLevel = parameters.compilerArgumentsLogLevel.get(),
            arguments = parameters.arguments.get(),
        )

        if (parameters.ideaSyncEnabled.get()) {
            try {
                runner.runTool(toolArguments)
            } catch (t: Throwable) {
                val errorText = "Warning: Failed to generate cinterop for ${parameters.taskPath.get()}: ${t.message ?: ""}"
                logger.warn(errorText, t)
                outputFile.deleteRecursively()
                errorFile.writeText(errorText)
                return
            }
        } else {
            runner.runTool(toolArguments)
        }

        // Save header hashes for up-to-date checking (only on success)
        val hashFile = parameters.allHeadersHashesFile.get().asFile
        hashFile.parentFile.mkdirs()
        hashFile.writeText(JsonUtils.gson.toJson(parameters.headerHashMap.get()))
    }
}
