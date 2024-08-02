/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File

internal fun ObjectFactory.KotlinNativeCompilerRunner(
    metricsReporter: Provider<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>>,
    classLoadersCachingBuildService: Provider<ClassLoadersCachingBuildService>,
    shouldDisableKonanDaemon: Provider<Boolean>,
    useXcodeMessageStyle: Provider<Boolean>,
    isUseEmbeddableCompilerJar: Provider<Boolean>,
    actualNativeHomeDirectory: Provider<File>,
    jvmArgs: Provider<List<String>>,
    konanPropertiesBuildService: Provider<KonanPropertiesBuildService>,
): KotlinNativeToolRunner = newInstance<KotlinNativeToolRunner>(
    metricsReporter,
    classLoadersCachingBuildService,
    kotlinToolSpec(
        shouldDisableKonanDaemon,
        useXcodeMessageStyle,
        isUseEmbeddableCompilerJar,
        actualNativeHomeDirectory,
        jvmArgs,
        konanPropertiesBuildService,
    )
)

private fun ObjectFactory.kotlinToolSpec(
    shouldDisableKonanDaemon: Provider<Boolean>,
    useXcodeMessageStyle: Provider<Boolean>,
    isUseEmbeddableCompilerJar: Provider<Boolean>,
    actualNativeHomeDirectory: Provider<File>,
    jvmArgs: Provider<List<String>>,
    konanPropertiesBuildService: Provider<KonanPropertiesBuildService>,
) = KotlinNativeToolRunner.ToolSpec(
    displayName = property("konanc"),
    optionalToolName = property("konanc"),
    mainClass = nativeMainClass,
    daemonEntryPoint = useXcodeMessageStyle.nativeDaemonEntryPoint(),
    classpath = nativeCompilerClasspath(actualNativeHomeDirectory, isUseEmbeddableCompilerJar),
    jvmArgs = listProperty<String>().value(jvmArgs),
    shouldPassArgumentsViaArgFile = shouldDisableKonanDaemon,
    systemProperties = nativeExecSystemProperties(useXcodeMessageStyle),
    environment = useXcodeMessageStyle.map {
        val messageRenderer = if (it) MessageRenderer.XCODE_STYLE else MessageRenderer.GRADLE_STYLE
        mapOf(MessageRenderer.PROPERTY_KEY to messageRenderer.name)
    }.get(),
    environmentBlacklist = konanPropertiesBuildService.get().environmentBlacklist,
).disableC2().enableAssertions().configureDefaultMaxHeapSize()