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
import org.jetbrains.kotlin.gradle.utils.toHexString
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

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
        if (project.kotlinNativeToolchainEnabled) {
            project.prepareKotlinNativeBundle(
                kotlinNativeVersion,
                bundleDir.asFile,
                reinstallFlag,
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
        kotlinNativeVersion: String,
        bundleDir: File,
        reinstallFlag: Boolean,
        konanTarget: KonanTarget,
    ) {

        if (reinstallFlag) {
            bundleDir.deleteRecursively()
        }

        //snapshot version can be updated so checksum is required
        if (kotlinNativeVersion.endsWith("SNAPSHOT")) {
            val snapshotCheckSum = getCheckSum(getGradleCachesKotlinNativeDir(kotlinNativeVersion))
            val checkSumFile = bundleDir.resolve("checksum")
            val currentCheckSum = if (checkSumFile.exists()) checkSumFile.readText() else null
            if (snapshotCheckSum != currentCheckSum) {
                logger.info("Delete existed Kotlin/Native ($currentCheckSum) because snapshot version was updated to $snapshotCheckSum")
                bundleDir.deleteRecursively()
            }
        }

        if (!bundleDir.resolve("bin").exists()) {
            val gradleCachesKotlinNativeDir = getGradleCachesKotlinNativeDir(kotlinNativeVersion)

            logger.info("Moving Kotlin/Native bundle from tmp directory $gradleCachesKotlinNativeDir to ${bundleDir.absolutePath}")
            copy {
                it.from(gradleCachesKotlinNativeDir)
                it.into(bundleDir)
            }

            if (kotlinNativeVersion.endsWith("SNAPSHOT")) {
                val checksumFile = bundleDir.resolve("checksum")
                getCheckSum(gradleCachesKotlinNativeDir)?.also {
                    checksumFile.writeText(it)
                }
            }

            logger.info("Moved Kotlin/Native bundle from $gradleCachesKotlinNativeDir to ${bundleDir.absolutePath}")
        }

        setupKotlinNativeDependencies(konanTarget)
    }

    private fun getGradleCachesKotlinNativeDir(kotlinNativeVersion: String) = kotlinNativeCompilerConfiguration
        .singleOrNull()
        ?.resolve(kotlinNativeVersion)
        ?: error(
            "Kotlin Native dependency has not been properly resolved. " +
                    "Please, make sure that you've declared the repository, which contains $kotlinNativeVersion."
        )

    private fun getCheckSum(gradleCachesKotlinNativeDir: File): String? {
        val sha256 = MessageDigest.getInstance("SHA-256")
        Files.walk(gradleCachesKotlinNativeDir.resolve("bin").toPath()).filter { it.toFile().isFile }.forEach {
            sha256.update(Files.readAllBytes(it))
        }
        return sha256.digest()?.toHexString()
    }

    private fun Project.setupKotlinNativeDependencies(konanTarget: KonanTarget) {
        val distributionType = NativeDistributionTypeProvider(this).getDistributionType()
        if (distributionType.mustGeneratePlatformLibs) {
            PlatformLibrariesGenerator(project, konanTarget).generatePlatformLibsIfNeeded()
        }
    }

}