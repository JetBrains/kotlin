/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.konanTargets
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.compilerRunner.GradleCliCommonizer
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCommonizerToolRunner
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.report.GradleBuildMetricsReporter
import org.jetbrains.kotlin.gradle.report.UsesBuildMetricsService
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.UsesKotlinNativeBundleBuildService
import org.jetbrains.kotlin.gradle.utils.chainedFinalizeValueOnRead
import org.jetbrains.kotlin.gradle.utils.directoryProperty
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import java.io.File
import java.net.URLEncoder
import javax.inject.Inject

@DisableCachingByDefault(because = "Native Distribution Commonizer Task uses internal caching mechanism with fine grained cache control")
internal abstract class NativeDistributionCommonizerTask
@Inject constructor(
    private val objectFactory: ObjectFactory,
    private val execOperations: ExecOperations,
) : DefaultTask(), UsesBuildMetricsService, UsesKotlinNativeBundleBuildService {

    private val konanHome = project.file(project.konanHome)

    private val commonizerTargets: Set<SharedCommonizerTarget> by lazy {
        project.collectAllSharedCommonizerTargetsFromBuild()
    }

    @get:Internal
    internal val kotlinPluginVersion: Property<String> = objectFactory
        .property<String>()
        .chainedFinalizeValueOnRead()

    @get:Classpath
    internal val commonizerClasspath: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Input
    internal val customJvmArgs: ListProperty<String> = objectFactory
        .listProperty<String>()
        .chainedFinalizeValueOnRead()

    private val kotlinCompilerArgumentsLogLevel = project.kotlinPropertiesProvider.kotlinCompilerArgumentsLogLevel

    private val logLevel = project.commonizerLogLevel

    private val additionalSettings = project.additionalCommonizerSettings

    @Suppress("unused")
    @get:Internal
    @Deprecated("Use lazy replacement", replaceWith = ReplaceWith("rootOutputDirectoryProperty.get().asFile"))
    internal val rootOutputDirectory: File get() = rootOutputDirectoryProperty.asFile.get()

    @get:OutputDirectory
    internal val rootOutputDirectoryProperty: DirectoryProperty = objectFactory
        .directoryProperty(
            project.file(project.konanHome)
                .resolve(KONAN_DISTRIBUTION_KLIB_DIR)
                .resolve(KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR)
                .resolve(URLEncoder.encode(project.getKotlinPluginVersion(), Charsets.UTF_8.name()))
        )

    private val isCachingEnabled = project.kotlinPropertiesProvider.enableNativeDistributionCommonizationCache

    private val commonizerCache
        get() = NativeDistributionCommonizerCache(
            outputDirectory = rootOutputDirectoryProperty.get().asFile,
            konanHome = konanHome,
            logger = logger,
            isCachingEnabled = isCachingEnabled
        )

    @get:Internal
    val metrics: Property<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>> = project.objects
        .property(GradleBuildMetricsReporter())

    @get:Nested
    internal val kotlinNativeProvider: Provider<KotlinNativeProvider> = project.provider {
        KotlinNativeProvider(project, commonizerTargets.flatMap { target -> target.konanTargets }.toSet(), kotlinNativeBundleBuildService)
    }

    @TaskAction
    protected fun run() {
        val metricsReporter = metrics.get()

        addBuildMetricsForTaskAction(metricsReporter = metricsReporter, languageVersion = null) {
            val runnerSettings = KotlinNativeCommonizerToolRunner.Settings(
                kotlinPluginVersion.get(),
                commonizerClasspath.files,
                customJvmArgs.get(),
                kotlinCompilerArgumentsLogLevel,
            )
            val commonizerRunner = KotlinNativeCommonizerToolRunner(
                context = KotlinToolRunner.GradleExecutionContext.fromTaskContext(objectFactory, execOperations, logger),
                settings = runnerSettings,
                metricsReporter = metricsReporter,
            )

            commonizerCache.writeCacheForUncachedTargets(commonizerTargets) { todoOutputTargets ->
                val commonizer = GradleCliCommonizer(commonizerRunner)
                /* Invoke commonizer with only 'to do' targets */
                commonizer.commonizeNativeDistribution(
                    konanHome,
                    rootOutputDirectoryProperty.get().asFile,
                    todoOutputTargets,
                    logLevel,
                    additionalSettings,
                )
            }
        }
    }

    init {
        outputs.upToDateWhen {
            // upToDateWhen executes after configuration phase, but before inputs are calculated,
            // that is why we need to get k/n bundle before commonizerCache.isUpToDate here
            kotlinNativeProvider.get().kotlinNativeBundleVersion.get()
            commonizerCache.isUpToDate(commonizerTargets)
        }
    }
}

private fun Project.collectAllSharedCommonizerTargetsFromBuild(): Set<SharedCommonizerTarget> {
    return allprojects.flatMap { project -> project.collectAllSharedCommonizerTargetsFromProject() }.toSet()
}

private fun Project.collectAllSharedCommonizerTargetsFromProject(): Set<SharedCommonizerTarget> {
    return (project.multiplatformExtensionOrNull ?: return emptySet()).sourceSets
        .mapNotNull { sourceSet -> sourceSet.commonizerTarget.getOrThrow() }
        .filterIsInstance<SharedCommonizerTarget>()
        .toSet()
}
