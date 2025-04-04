/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.commonizer.KonanDistribution
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
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

internal sealed class KotlinNativeProvider(project: Project) {
    @get:Internal
    val konanDataDir: Provider<String?> = project.nativeProperties.konanDataDir.map {
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        it!!.absolutePath
    }

    @get:Internal
    val toolchainEnabled: Provider<Boolean> = project.nativeProperties.isToolchainEnabled

    @get:Internal
    //Using DirectoryProperty causes the native directory to be included in the configuration cache input.
    internal val bundleDirectory: Provider<String> = project.nativeProperties.actualNativeHomeDirectory.map { it.absolutePath }

    @get:Internal
    //Access konanDistributionProvider will lead to native dependency download.
    internal open val konanDistributionProvider: Provider<KonanDistribution> =
        project.nativeProperties.actualNativeHomeDirectory.map { KonanDistribution(it.absolutePath) }
}

/**
 * This Kotlin Native provider is a stub for the cases, when Kotlin Native tasks are not supported to be built.
 */
internal class NoopKotlinNativeProvider(project: Project) : KotlinNativeProvider(project)

/**
 * This Kotlin Native provider is used to get a kotlin native bundle from provided K/N toolchain.
 */
internal class KotlinNativeFromToolchainProvider(
    project: Project,
    konanTargets: Set<KonanTarget>,
    kotlinNativeBundleBuildService: Provider<KotlinNativeBundleBuildService>,
    enableDependenciesDownloading: Boolean = true,
) : KotlinNativeProvider(project) {
    constructor(
        project: Project,
        konanTarget: KonanTarget,
        kotlinNativeBundleBuildService: Provider<KotlinNativeBundleBuildService>,
    ) : this(project, setOf(konanTarget), kotlinNativeBundleBuildService)

    private val providerFactory = project.providers

    @get:Internal
    internal val nativeDistributionType = project.provider { PropertiesProvider(project).nativeDistributionType }

    @get:Internal
    val overriddenKonanHome: Provider<String> = project.nativeProperties.userProvidedNativeHome

    @get:Internal
    val reinstallBundle: Property<Boolean> = project.objects.property(project.kotlinPropertiesProvider.nativeReinstall)

    @get:Internal
    internal val simpleKotlinNativeVersion: Provider<String> = project.nativeProperties.kotlinNativeVersion

    @get:Input
    internal val kotlinNativeBundleVersion: Provider<String> =
        toolchainEnabled.flatMap { toolchainEnabled ->
            if (toolchainEnabled) {
                if (overriddenKonanHome.isPresent) {
                    project.logger.info("A user-provided Kotlin/Native distribution configured: ${overriddenKonanHome.get()}. Disabling Kotlin Native Toolchain auto-provisioning.")
                    overriddenKonanHome
                } else {
                    providerFactory.of(NativeVersionValueSource::class.java) {
                        it.parameters.bundleDirectory.set(project.nativeProperties.actualNativeHomeDirectory.map { it.absolutePath })
                        it.parameters.reinstallBundle.set(reinstallBundle)
                        it.parameters.kotlinNativeVersion.set(NativeCompilerDownloader.getDependencyNameWithOsAndVersion(project))
                        it.parameters.simpleKotlinNativeVersion.set(simpleKotlinNativeVersion)
                        it.parameters.kotlinNativeCompilerConfiguration.set(
                            project.objects.fileCollection()
                                .from(
                                    // without enabled there is no configuration with this name, so we should return empty provider to support configuration cache
                                    project.configurations.named(
                                        KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME
                                    )
                                )
                        )
                    }
                }
            } else {
                providerFactory.provider {
                    if (overriddenKonanHome.isPresent)
                        overriddenKonanHome.get()
                    else
                        NativeCompilerDownloader.getDependencyNameWithOsAndVersion(project)
                }
            }
        }.zip(kotlinNativeBundleBuildService) { nativeVersion, nativeBundleService ->
            nativeBundleService.setupKotlinNativePlatformLibraries(
                objects,
                konanTargetsWithNativeCacheKind,
                nativeDistributionType.orNull,
                kotlinCompilerArgumentsLogLevel,
                useXcodeMessageStyle,
                nativeClasspath.get(),
                nativeJvmArgs,
                actualNativeHomeDirectory,
                konanDataDir,
            )
            nativeVersion
        }

    @get:Input
    val kotlinNativeDependencies: Provider<Set<String>> =
        kotlinNativeBundleVersion
            .zip(bundleDirectory) { _, bundleDir ->
                if (toolchainEnabled.get() && enableDependenciesDownloading) {
                    kotlinNativeBundleBuildService.get()
                        .downloadNativeDependencies(
                            File(bundleDir),
                            konanDataDir.orNull,
                            konanTargets,
                        )
                } else {
                    emptySet()
                }
            }

    override val konanDistributionProvider: Provider<KonanDistribution>
        get() = super.bundleDirectory.zip(kotlinNativeDependencies) { bundleDir, _ -> KonanDistribution(bundleDir) }

    @get:Internal
    internal val nativeJvmArgs = project.listProperty { project.nativeProperties.jvmArgs.get() }

    @get:Internal
    internal val actualNativeHomeDirectory: Provider<File> = project.nativeProperties.actualNativeHomeDirectory

    @get:Internal
    internal val konanTargetsWithNativeCacheKind: Map<KonanTarget, Provider<NativeCacheKind>> =
        konanTargets.associateWith { konanTarget ->
            kotlinNativeBundleBuildService.flatMap { it.getNativeCacheKind(project, konanTarget) }
        }

    @get:Internal
    internal val objects = project.objects

    @get:Internal
    internal val kotlinCompilerArgumentsLogLevel = project.kotlinPropertiesProvider.kotlinCompilerArgumentsLogLevel

    @get:Internal
    internal val useXcodeMessageStyle = project.useXcodeMessageStyle

    @get:Internal
    internal val nativeClasspath = project.provider {
        project.objects.nativeCompilerClasspath(
            project.nativeProperties.actualNativeHomeDirectory,
            project.nativeProperties.shouldUseEmbeddableCompilerJar
        )
    }
}

internal fun UsesKotlinNativeBundleBuildService.chooseKotlinNativeProvider(
    enabledOnCurrenHost: Boolean,
    konanTarget: KonanTarget,
): KotlinNativeProvider {
    return if (enabledOnCurrenHost) {
        KotlinNativeFromToolchainProvider(project, konanTarget, kotlinNativeBundleBuildService)
    } else {
        NoopKotlinNativeProvider(project)
    }
}

internal val KotlinNativeProvider.konanDistribution
    get() = bundleDirectory.map {
        Distribution(it)
    }