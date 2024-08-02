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
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.File

internal fun ObjectFactory.KotlinNativeCInteropRunner(
    metricsReporter: Provider<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>>,
    classLoadersCachingBuildService: Provider<ClassLoadersCachingBuildService>,
    kotlinNativeCompilerJar: Provider<File>,
    actualNativeHomeDirectory: Provider<File>,
    jvmArgs: Provider<List<String>>,
    useXcodeMessageStyle: Provider<Boolean>,
    konanPropertiesBuildService: Provider<KonanPropertiesBuildService>,
): KotlinNativeToolRunner = newInstance(
    metricsReporter,
    classLoadersCachingBuildService,
    kotlinToolSpec(kotlinNativeCompilerJar, actualNativeHomeDirectory, jvmArgs, useXcodeMessageStyle, konanPropertiesBuildService)
)

private fun ObjectFactory.kotlinToolSpec(
    kotlinNativeCompilerJar: Provider<File>,
    actualNativeHomeDirectory: Provider<File>,
    jvmArgs: Provider<List<String>>,
    useXcodeMessageStyle: Provider<Boolean>,
    konanPropertiesBuildService: Provider<KonanPropertiesBuildService>,
) = KotlinNativeToolRunner.ToolSpec(
    displayName = property("cinterop"),
    optionalToolName = property("cinterop"),
    mainClass = nativeMainClass,
    daemonEntryPoint = useXcodeMessageStyle.nativeDaemonEntryPoint(),
    classpath = nativeCompilerClasspath(kotlinNativeCompilerJar, actualNativeHomeDirectory),
    jvmArgs = listProperty<String>().value(jvmArgs),
    shouldPassArgumentsViaArgFile = property(false),
    systemProperties = nativeExecSystemProperties(useXcodeMessageStyle),
    environment = nativeExecLLVMEnvironment,
    environmentBlacklist = konanPropertiesBuildService.get().environmentBlacklist,
).configureDefaultMaxHeapSize().enableAssertions()