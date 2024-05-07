/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.commonizer.CliCommonizer
import org.jetbrains.kotlin.internal.compilerRunner.native.KotlinNativeToolRunner
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.plugin.KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.*

private const val KOTLIN_KLIB_COMMONIZER_EMBEDDABLE = "kotlin-klib-commonizer-embeddable"

internal fun GradleCliCommonizer(
    commonizerToolRunner: KotlinNativeToolRunner,
    compilerArgumentsLogLevel: KotlinCompilerArgumentsLogLevel
): CliCommonizer {
    return CliCommonizer { arguments ->
        commonizerToolRunner.runTool(
            KotlinNativeToolRunner.ToolArguments(
                shouldRunInProcessMode = false,
                compilerArgumentsLogLevel = compilerArgumentsLogLevel,
                arguments = arguments,
            )
        )
    }
}

internal fun Project.maybeCreateCommonizerClasspathConfiguration(): Configuration {
    return configurations.findResolvable(KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME)
        ?: project.configurations.createResolvable(KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME)
            .run {
                attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                attributes.setAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
                attributes.setAttribute(Usage.USAGE_ATTRIBUTE, usageByName(Usage.JAVA_RUNTIME))
                defaultDependencies { dependencies ->
                    dependencies.add(
                        project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_KLIB_COMMONIZER_EMBEDDABLE:${getKotlinPluginVersion()}")
                    )
                }
            }
}

internal fun ObjectFactory.KotlinNativeCommonizerToolRunner(
    metricsReporter: Provider<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>>,
    classLoadersCachingBuildService: Provider<ClassLoadersCachingBuildService>,
    toolClasspath: FileCollection,
    toolJvmArgs: ListProperty<String>,
): KotlinNativeToolRunner = newInstance(
    metricsReporter,
    classLoadersCachingBuildService,
    kotlinNativeCommonizerToolSpec(
        toolClasspath,
        toolJvmArgs,
    )
)

private fun ObjectFactory.kotlinNativeCommonizerToolSpec(
    toolClasspath: FileCollection,
    toolJvmArgs: ListProperty<String>,
) = KotlinNativeToolRunner.ToolSpec(
    displayName = property("Kotlin/Native KLIB commonizer"),
    optionalToolName = property(),
    mainClass = property("org.jetbrains.kotlin.commonizer.cli.CommonizerCLI"),
    daemonEntryPoint = property("main"),
    classpath = toolClasspath,
    jvmArgs = listProperty<String>().value(toolJvmArgs).also { it.add("-Xmx4g") },
    shouldPassArgumentsViaArgFile = property<Boolean>().value(false),
).disableC2().enableAssertions()
