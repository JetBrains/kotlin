/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFusService
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.property

internal fun ObjectFactory.KotlinNativeLibraryGenerationRunner(
    metricsReporter: Provider<BuildMetricsReporter>,
    classLoadersCachingBuildService: Provider<ClassLoadersCachingBuildService>,
    useXcodeMessageStyle: Provider<Boolean>,
    classpath: FileCollection,
    jvmArgs: ListProperty<String>,
    environmentBlacklist: Provider<Set<String>>,
): KotlinNativeToolRunner = newInstance(
    metricsReporter,
    classLoadersCachingBuildService,
    kotlinToolSpec(useXcodeMessageStyle, classpath, jvmArgs, environmentBlacklist),
    property(BuildFusService::class.java)
)

private fun ObjectFactory.kotlinToolSpec(
    useXcodeMessageStyle: Provider<Boolean>,
    classpath: FileCollection,
    jvmArgs: ListProperty<String>,//nativeProperties.jvmArgs
    environmentBlacklist: Provider<Set<String>>,
) = KotlinNativeToolRunner.ToolSpec(
    displayName = property("generatePlatformLibraries"),
    optionalToolName = property("generatePlatformLibraries"),
    mainClass = nativeMainClass,
    daemonEntryPoint = useXcodeMessageStyle.nativeDaemonEntryPoint(),
    classpath = classpath,
    jvmArgs = listProperty<String>().value(jvmArgs),
    shouldPassArgumentsViaArgFile = property(false),
    systemProperties = nativeExecSystemProperties(useXcodeMessageStyle),
    environment = nativeExecLLVMEnvironment,
    environmentBlacklist = environmentBlacklist.get(),
    collectNativeCompilerMetrics = property(false),
).enableAssertions()
    .configureDefaultMaxHeapSize()
