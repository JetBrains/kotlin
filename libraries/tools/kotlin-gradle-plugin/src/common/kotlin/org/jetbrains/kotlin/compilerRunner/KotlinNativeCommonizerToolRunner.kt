/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import java.io.File

internal class KotlinNativeCommonizerToolRunner(
    context: GradleExecutionContext,
    private val settings: Settings,
    metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>
) : KotlinToolRunner(context, metricsReporter) {

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

