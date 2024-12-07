/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.konanTargets
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.internal.compilerRunner.native.KotlinNativeToolRunner
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.internal.UsesClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.report.GradleBuildMetricsReporter
import org.jetbrains.kotlin.gradle.report.UsesBuildMetricsService
import org.jetbrains.kotlin.gradle.targets.native.toolchain.NoopKotlinNativeProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeFromToolchainProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.UsesKotlinNativeBundleBuildService
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.chainedFinalizeValueOnRead
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
    layout: ProjectLayout,
    providerFactory: ProviderFactory,
) : DefaultTask(),
    UsesBuildMetricsService,
    UsesKotlinNativeBundleBuildService,
    UsesClassLoadersCachingBuildService {

    private val konanHome = project.nativeProperties.actualNativeHomeDirectory

    @get:Internal
    internal val commonizerTargets: Set<SharedCommonizerTarget> by lazy {
        project.collectAllSharedCommonizerTargetsFromBuild()
    }

    @get:Classpath
    internal val commonizerClasspath: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Input
    internal val customJvmArgs: ListProperty<String> = objectFactory
        .listProperty<String>()
        .chainedFinalizeValueOnRead()

    @get:Internal
    internal abstract val kotlinCompilerArgumentsLogLevel: Property<KotlinCompilerArgumentsLogLevel>

    private val logLevel = project.commonizerLogLevel

    private val additionalSettings = project.additionalCommonizerSettings

    @Suppress("unused")
    @get:Internal
    @Deprecated("Use lazy replacement", replaceWith = ReplaceWith("rootOutputDirectoryProperty.get().asFile"))
    internal val rootOutputDirectory: File get() = rootOutputDirectoryProperty.asFile.get()

    private val kotlinPluginVersion = project.getKotlinPluginVersion()

    @get:Internal
    internal val rootOutputDirectoryProperty: DirectoryProperty = objectFactory
        .directoryProperty().fileProvider(
            konanHome.map {
                it.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
                    .resolve(KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR)
                    .resolve(URLEncoder.encode(kotlinPluginVersion, Charsets.UTF_8.name()))
            }
        )

    /**
     * With Project Isolation support, each Gradle Project with KMP enabled will have its own [NativeDistributionCommonizerTask] task.
     * And because Native Distribution can be shared with multiple Gradle Builds, its commonized libraries also can be shared.
     * So each [NativeDistributionCommonizerTask] can write to the same output directory [rootOutputDirectoryProperty].
     * But in practice only one task will do actual commonization, other will just wait for it to finish.
     * And that would make gradle to remember that outptus stored in [rootOutputDirectoryProperty] is associated with the task that did
     * the job. And will report warning about task that uses commonizer output being implicitly depended on commonizer tasks that did
     * the commonization.
     *
     * To fix that problem [commonizedNativeDistributionLocationFile] was introduced. This file referenced the actual location where
     * commonized libraries are located. But internal Project tasks can "map" this [commonizedNativeDistributionLocationFile] to avoid
     * issues with tasks dependencies.
     */
    @get:OutputFile
    internal val commonizedNativeDistributionLocationFile: RegularFileProperty = objectFactory
        .fileProperty()
        .value(layout.buildDirectory.file("kotlin/commonizedNativeDistributionLocation.txt"))

    private val isCachingEnabled = project.kotlinPropertiesProvider.enableNativeDistributionCommonizationCache

    private val commonizerCache
        get() = NativeDistributionCommonizerCache(
            outputDirectory = rootOutputDirectoryProperty.get().asFile,
            konanHome = konanHome.get(),
            logger = logger,
            isCachingEnabled = isCachingEnabled
        )

    @get:Internal
    val metrics: Property<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>> = project.objects
        .property(GradleBuildMetricsReporter())

    @get:Nested
    internal val kotlinNativeProvider: Property<KotlinNativeProvider> =
        project.objects.propertyWithConvention<KotlinNativeProvider>(
            // For KT-66452 we need to get rid of invocation of 'Task.project'.
            // That is why we moved setting this property to task registration
            // and added convention for backwards compatibility.
            project.provider {
                KotlinNativeFromToolchainProvider(
                    project,
                    commonizerTargets.flatMap { target -> target.konanTargets }.toSet(),
                    kotlinNativeBundleBuildService,
                    enableDependenciesDownloading = false
                )
            })

    @get:Internal
    internal val commonizerToolRunner: Provider<KotlinNativeToolRunner> = providerFactory.provider {
        objectFactory.KotlinNativeCommonizerToolRunner(
            metrics,
            classLoadersCachingService,
            commonizerClasspath,
            customJvmArgs
        )
    }

    @TaskAction
    protected fun run() {
        commonizedNativeDistributionLocationFile.get().asFile.writeText(rootOutputDirectoryProperty.get().asFile.absolutePath)

        val metricsReporter = metrics.get()

        addBuildMetricsForTaskAction(metricsReporter = metricsReporter, languageVersion = null) {
            commonizerCache.writeCacheForUncachedTargets(commonizerTargets) { todoOutputTargets ->
                val commonizer = GradleCliCommonizer(commonizerToolRunner.get(), kotlinCompilerArgumentsLogLevel.get())
                /* Invoke commonizer with only 'to do' targets */
                commonizer.commonizeNativeDistribution(
                    konanHome.get(),
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
            when (val kotlinNativeProvider = kotlinNativeProvider.get()) {
                is KotlinNativeFromToolchainProvider -> kotlinNativeProvider.kotlinNativeBundleVersion.get()
                is NoopKotlinNativeProvider ->
                    logger.error("Unexpected Kotlin/Native provider: $kotlinNativeProvider during commonization task. Please report an issue: https://kotl.in/issue")
            }
            commonizerCache.isUpToDate(commonizerTargets)
        }
    }
}

private fun Project.collectAllSharedCommonizerTargetsFromBuild(): Set<SharedCommonizerTarget> {
    return if (kotlinPropertiesProvider.kotlinKmpProjectIsolationEnabled) {
        collectAllSharedCommonizerTargetsFromProject()
    } else {
        allprojects.flatMap { project -> project.collectAllSharedCommonizerTargetsFromProject() }.toSet()
    }
}

private fun Project.collectAllSharedCommonizerTargetsFromProject(): Set<SharedCommonizerTarget> {
    return (project.multiplatformExtensionOrNull ?: return emptySet()).sourceSets
        .mapNotNull { sourceSet -> sourceSet.commonizerTarget.getOrThrow() }
        .filterIsInstance<SharedCommonizerTarget>()
        .toSet()
}
