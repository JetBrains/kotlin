/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.File

internal fun ObjectFactory.KotlinNativeCommonizerToolRunner(
    settings: KotlinNativeCommonizerToolRunner.Settings,
    metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
): KotlinNativeCommonizerToolRunner = newInstance(settings, metricsReporter)

internal abstract class KotlinNativeCommonizerToolRunner(
    private val settings: Settings,
    metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    objectFactory: ObjectFactory,
    execOperations: ExecOperations,
) : KotlinToolRunner(metricsReporter, objectFactory, execOperations) {

    class Settings(
        val kotlinPluginVersion: String,
        val classpath: Set<File>,
        val customJvmArgs: List<String>,
        val compilerArgumentsLogLevel: Provider<KotlinCompilerArgumentsLogLevel>,
    )

    override val displayName get() = "Kotlin/Native KLIB commonizer"

    override val mainClass: String get() = "org.jetbrains.kotlin.commonizer.cli.CommonizerCLI"

    override val classpath: Set<File> get() = settings.classpath

    override val isolatedClassLoaderCacheKey get() = settings.kotlinPluginVersion

    override val defaultMaxHeapSize: String get() = "4G"

    override val mustRunViaExec get() = true // because it's not enough the standard Gradle wrapper's heap size

    override fun getCustomJvmArgs() = settings.customJvmArgs

    override val compilerArgumentsLogLevel: KotlinCompilerArgumentsLogLevel get() = settings.compilerArgumentsLogLevel.get()
}

