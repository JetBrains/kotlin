/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.useXcodeMessageStyle
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.internal.compilerRunner.native.nativeCompilerClasspath
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject

/**
 * This is a nested provider for all native tasks
 */
internal class KotlinNativeProvider(
    project: Project,
    konanTargets: Set<KonanTarget>,
    kotlinNativeBundleBuildService: Provider<KotlinNativeBundleBuildService>,
    enableDependenciesDownloading: Boolean = true,
) {
    constructor(
        project: Project,
        konanTarget: KonanTarget,
        kotlinNativeBundleBuildService: Provider<KotlinNativeBundleBuildService>,
    ) : this(project, setOf(konanTarget), kotlinNativeBundleBuildService)

    private val providerFactory = project.providers

    @get:Internal
    val konanDataDir: Provider<String?> = project.nativeProperties.konanDataDir.map {
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        it!!.absolutePath
    }

    @get:Internal
    val toolchainEnabled: Provider<Boolean> = project.nativeProperties.isToolchainEnabled

    @get:Internal
    internal val bundleDirectory: Provider<String> = project.nativeProperties.actualNativeHomeDirectory.map { it.absolutePath }

    @get:Internal
    val overriddenKonanHome: Provider<String> = project.nativeProperties.userProvidedNativeHome

    @get:Internal
    val reinstallBundle: Property<Boolean> = project.objects.property(project.kotlinPropertiesProvider.nativeReinstall)

    // Gradle tries to evaluate this val during configuration cache,
    // which lead to resolving configuration, even if k/n bundle is in konan home directory.
    @Transient
    private val kotlinNativeCompilerConfiguration: ConfigurableFileCollection = project.objects.fileCollection()
        .from(
            // without enabled there is no configuration with this name, so we should return empty provider to support configuration cache
            toolchainEnabled.flatMap { isEnabled ->
                if (isEnabled) {
                    project.configurations.named(
                        KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME
                    )
                } else {
                    providerFactory.provider { null }
                }
            }
        )

    @get:Internal
    internal val dependencyNameWithOsAndVersion = NativeCompilerDownloader.getDependencyNameWithOsAndVersion(project)

    @get:Internal
    internal val nativeKotlinVersion = project.nativeProperties.kotlinNativeVersion

    @get:Internal
    internal val nativeJvmArgs = project.listProperty { project.nativeProperties.jvmArgs.get() }

    @get:Internal
    internal val actualNativeHomeDirectory: Provider<File> = project.nativeProperties.actualNativeHomeDirectory

    @get:Internal
//    internal val nativeCacheKind: Provider<NativeCacheKind> = project.nativeProperties.getKonanCacheKind(konanTargets, konanPropertiesService)
    internal val nativeCacheKind: Provider<NativeCacheKind> = project.provider { NativeCacheKind.NONE }

    @get:Internal
    internal val objects = project.provider { project.objects }

    @get:Internal
    internal val kotlinCompilerArgumentsLogLevel = project.kotlinPropertiesProvider.kotlinCompilerArgumentsLogLevel

    @get:Internal
    internal val useXcodeMessageStyle = project.provider { project.useXcodeMessageStyle }

    @get:Internal
    internal val nativeClasspath = project.provider {
        project.objects.nativeCompilerClasspath(
            project.nativeProperties.actualNativeHomeDirectory,
            project.nativeProperties.shouldUseEmbeddableCompilerJar
        )
    }

    @get:Internal
    internal val nativeDistributionType = project.provider { PropertiesProvider(project).nativeDistributionType }

    @get:Input
    internal val kotlinNativeBundleVersion: Provider<String> = providerFactory.of(NativeVersionValueSource::class.java) {
        it.parameters.bundleDirectory.set(project.nativeProperties.actualNativeHomeDirectory.map { it.absolutePath })
        it.parameters.reinstallBundle.set(reinstallBundle)
        it.parameters.overriddenKonanHome.set(overriddenKonanHome)
        it.parameters.dependencyNameWithOsAndVersion.set(dependencyNameWithOsAndVersion)
        it.parameters.toolchainEnabled.set(toolchainEnabled)
        it.parameters.kotlinNativeCompilerConfiguration.set(kotlinNativeCompilerConfiguration)
//        it.parameters.logger.set(logger)
        it.parameters.kotlinNativeVersion.set(nativeKotlinVersion)
    }.zip(kotlinNativeBundleBuildService) { nativeVersion, nativeBundleService ->
        nativeBundleService.setupKotlinNativePlatformLibraries(
            objects.get(),
            konanTargets,
            nativeDistributionType.orNull,
            kotlinCompilerArgumentsLogLevel,
            useXcodeMessageStyle.get(),
            nativeClasspath.get(),
            nativeJvmArgs,
            actualNativeHomeDirectory,
            konanDataDir,
            nativeCacheKind,
        )
        nativeVersion
    }

    @get:Input
    val kotlinNativeDependencies: Provider<Set<String>> =
        kotlinNativeBundleBuildService.zip(
            project.nativeProperties.actualNativeHomeDirectory.map { it.absolutePath }
        ) { service, bundleDir ->
            if (toolchainEnabled.get() && enableDependenciesDownloading) {
                service.downloadNativeDependencies(
                    File(bundleDir),
                    konanDataDir.orNull,
                    konanTargets,
//                        logger
//                            project.logger
                )
            } else {
                emptySet()
            }
        }
}


internal open class FileSystemOperationsHandler @Inject constructor(val fileSystemOperations: FileSystemOperations) {}