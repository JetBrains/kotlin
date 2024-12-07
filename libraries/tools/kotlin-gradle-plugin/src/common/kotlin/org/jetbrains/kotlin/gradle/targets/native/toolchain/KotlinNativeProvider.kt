/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget

internal sealed class KotlinNativeProvider(project: Project) {
    @get:Internal
    val konanDataDir: Provider<String?> = project.nativeProperties.konanDataDir.map {
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        it!!.absolutePath
    }

    @get:Internal
    val toolchainEnabled: Provider<Boolean> = project.nativeProperties.isToolchainEnabled

    //Using DirectoryProperty causes the native directory to be included in the configuration cache input.
    @get:Internal
    val bundleDirectory: DirectoryProperty = project.objects.directoryProperty()
        .fileProvider(project.nativeProperties.actualNativeHomeDirectory)

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
    val overriddenKonanHome: Provider<String> = project.nativeProperties.userProvidedNativeHome

    @get:Internal
    val reinstallBundle: Property<Boolean> = project.objects.property(project.kotlinPropertiesProvider.nativeReinstall)

    @get:Input
    internal val kotlinNativeBundleVersion: Provider<String> = bundleDirectory.zip(reinstallBundle) { bundleDir, reinstallFlag ->
        val kotlinNativeVersion =
            if (overriddenKonanHome.isPresent)
                overriddenKonanHome.get()
            else
                NativeCompilerDownloader.getDependencyNameWithOsAndVersion(project)

        if (toolchainEnabled.get()) {
            kotlinNativeBundleBuildService.get().prepareKotlinNativeBundle(
                project,
                kotlinNativeCompilerConfiguration,
                kotlinNativeVersion,
                bundleDir.asFile,
                reinstallFlag,
                konanTargets,
                overriddenKonanHome.orNull
            )
        }
        kotlinNativeVersion
    }

    @get:Input
    val kotlinNativeDependencies: Provider<Set<String>> =
        kotlinNativeBundleVersion
            .zip(bundleDirectory) { _, bundleDir ->
                if (toolchainEnabled.get() && enableDependenciesDownloading) {
                    kotlinNativeBundleBuildService.get()
                        .downloadNativeDependencies(
                            bundleDir.asFile,
                            konanDataDir.orNull,
                            konanTargets,
                            project.logger
                        )
                } else {
                    emptySet()
                }
            }

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
}

internal fun UsesKotlinNativeBundleBuildService.chooseKotlinNativeProvider(enabledOnCurrenHost: Boolean, konanTarget: KonanTarget, ): KotlinNativeProvider {
    if (enabledOnCurrenHost) {
        return KotlinNativeFromToolchainProvider(project, konanTarget, kotlinNativeBundleBuildService)
    } else {
        return NoopKotlinNativeProvider(project)
    }
}

internal val KotlinNativeProvider.konanDistribution
    get() = bundleDirectory.map {
        Distribution(it.asFile.absolutePath)
    }