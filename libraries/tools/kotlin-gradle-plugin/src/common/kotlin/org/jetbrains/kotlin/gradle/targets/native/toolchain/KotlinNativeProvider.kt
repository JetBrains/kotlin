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
import org.jetbrains.kotlin.compilerRunner.konanDataDir
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.compilerRunner.kotlinNativeToolchainEnabled
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionTypeProvider
import org.jetbrains.kotlin.gradle.targets.native.internal.PlatformLibrariesGenerator
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

/**
 * This is a nested provider for all native tasks
 */
internal class KotlinNativeProvider(project: Project, konanTarget: KonanTarget) {

    @get:Internal
    val konanDataDir: Provider<String?> = project.provider { project.konanDataDir }

    @get:Internal
    val bundleDirectory: DirectoryProperty = project.objects.directoryProperty().fileProvider(
        project.provider {
            project.konanHome
        }
    )

    @get:Internal
    val reinstallBundle: Property<Boolean> = project.objects.property(project.kotlinPropertiesProvider.nativeReinstall)

    @get:Input
    internal val kotlinNativeBundleVersion: Provider<String> = bundleDirectory.zip(reinstallBundle) { bundleDir, reinstallFlag ->
        val kotlinNativeVersion = NativeCompilerDownloader.getDependencyNameWithOsAndVersion(project)
        if (project.kotlinNativeToolchainEnabled && (reinstallFlag || !bundleDir.asFile.exists())) {
            val kotlinNativeCompilerExtractedFolder =
                kotlinNativeCompilerConfiguration
                    .singleOrNull()
                    ?.resolve(kotlinNativeVersion)
                    ?: error(
                        "Kotlin Native dependency has not been properly resolved. " +
                                "Please, make sure that you've declared the repository, which contains $kotlinNativeVersion."
                    )

            project.prepareKotlinNativeBundle(
                bundleDir.asFile,
                reinstallFlag,
                kotlinNativeCompilerExtractedFolder,
                konanTarget
            )
        }
        kotlinNativeVersion
    }

    private val kotlinNativeCompilerConfiguration: ConfigurableFileCollection = project.filesProvider {
        // without enabled there is no configuration with this name, so we should return empty provider to support configuraiton cache
        if (project.kotlinNativeToolchainEnabled) {
            project.configurations.named(
                KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME
            )
        } else {
            null
        }
    }

    private fun Project.prepareKotlinNativeBundle(
        bundleDir: File,
        reinstallFlag: Boolean,
        gradleCachesKotlinNativeDir: File,
        konanTarget: KonanTarget,
    ) {

        if (reinstallFlag) {
            NativeCompilerDownloader.getCompilerDirectory(project).deleteRecursively()
        }

        if (!bundleDir.exists()) {
            logger.info("Moving Kotlin/Native bundle from tmp directory $gradleCachesKotlinNativeDir to ${bundleDir.absolutePath}")
            copy {
                it.from(gradleCachesKotlinNativeDir)
                it.into(bundleDir)
            }
            logger.info("Moved Kotlin/Native bundle from $gradleCachesKotlinNativeDir to ${bundleDir.absolutePath}")
        }

        setupKotlinNativeDependencies(konanTarget)
    }

    private fun Project.setupKotlinNativeDependencies(konanTarget: KonanTarget) {
        val distributionType = NativeDistributionTypeProvider(this).getDistributionType()
        if (distributionType.mustGeneratePlatformLibs) {
            PlatformLibrariesGenerator(project, konanTarget).generatePlatformLibsIfNeeded()
        }
    }

}