/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFusService
import org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File

internal fun ObjectFactory.KotlinNativeCompilerRunner(
    metricsReporter: Provider<BuildMetricsReporter>,
    classLoadersCachingBuildService: Provider<ClassLoadersCachingBuildService>,
    shouldDisableKonanDaemon: Provider<Boolean>,
    useXcodeMessageStyle: Provider<Boolean>,
    actualNativeHomeDirectory: Provider<File>,
    jvmArgs: Provider<List<String>>,
    konanPropertiesBuildService: Provider<KonanPropertiesBuildService>,
    buildFusService: Property<out BuildFusService<out BuildFusService.Parameters>?>,
    kotlinNativeVersion: Provider<String>,
): KotlinNativeToolRunner = newInstance<KotlinNativeToolRunner>(
    metricsReporter,
    classLoadersCachingBuildService,
    kotlinToolSpec(
        shouldDisableKonanDaemon,
        useXcodeMessageStyle,
        actualNativeHomeDirectory,
        jvmArgs,
        konanPropertiesBuildService,
        kotlinNativeVersion
    ),
    buildFusService
)

private fun ObjectFactory.kotlinToolSpec(
    shouldDisableKonanDaemon: Provider<Boolean>,
    useXcodeMessageStyle: Provider<Boolean>,
    actualNativeHomeDirectory: Provider<File>,
    jvmArgs: Provider<List<String>>,
    konanPropertiesBuildService: Provider<KonanPropertiesBuildService>,
    kotlinNativeVersion: Provider<String>,
) = KotlinNativeToolRunner.ToolSpec(
    displayName = property("konanc"),
    optionalToolName = property("konanc"),
    mainClass = nativeMainClass,
    daemonEntryPoint = useXcodeMessageStyle.nativeDaemonEntryPoint(),
    classpath = nativeCompilerClasspath(actualNativeHomeDirectory),
    jvmArgs = listProperty<String>().value(jvmArgs),
    shouldPassArgumentsViaArgFile = shouldDisableKonanDaemon,
    systemProperties = nativeExecSystemProperties(useXcodeMessageStyle),
    environment = useXcodeMessageStyle.map {
        val messageRenderer = if (it) MessageRenderer.XCODE_STYLE else MessageRenderer.GRADLE_STYLE
        mapOf(MessageRenderer.PROPERTY_KEY to messageRenderer.name)
    }.get(),
    environmentBlacklist = konanPropertiesBuildService.get().environmentBlacklist,
    collectNativeCompilerMetrics = nativeCompilerPerformanceMetricsAvailable(kotlinNativeVersion)
).disableC2().enableAssertions().configureDefaultMaxHeapSize()