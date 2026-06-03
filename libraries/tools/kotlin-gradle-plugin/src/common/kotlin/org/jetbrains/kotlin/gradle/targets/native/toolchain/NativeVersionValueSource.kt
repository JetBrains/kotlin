/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionCommonizerLock
import org.jetbrains.kotlin.konan.util.ArchiveType
import org.jetbrains.kotlin.konan.util.DependencyExtractor
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal abstract class NativeVersionValueSource :
    ValueSource<String, NativeVersionValueSource.Params> {

    interface Params : ValueSourceParameters {
        val bundleDirectory: Property<String>
        val reinstallBundle: Property<Boolean>
        val simpleKotlinNativeVersion: Property<String>
        val kotlinNativeVersion: Property<String>
        val kotlinNativeCompilerConfiguration: Property<ConfigurableFileCollection>
    }

    override fun obtain(): String {
        val kotlinNativeVersion = parameters.kotlinNativeVersion.get()
        prepareKotlinNativeBundle(
            parameters.kotlinNativeCompilerConfiguration.get(),
            kotlinNativeVersion,
            Paths.get(parameters.bundleDirectory.get()),
            parameters.reinstallBundle.get(),
        )
        return kotlinNativeVersion
    }

    /**
     * This function downloads and installs a Kotlin Native bundle if needed
     * and then prepares its platform libraries if needed.
     *
     * @param project The Gradle project object.
     * @param kotlinNativeBundleConfiguration Gradle configuration for Kotlin Native Bundle
     * @param kotlinNativeVersion The version of Kotlin/Native to install
     * @param bundleDir The directory to store the Kotlin/Native bundle.
     * @param reinstallFlag A flag indicating whether to reinstall the bundle.
     * @param konanTargets The set of KonanTarget objects representing the targets for the Kotlin/Native bundle.
     * @return kotlin native version if toolchain was used, path to konan home if konan home was used
     */
    private fun prepareKotlinNativeBundle(
        kotlinNativeBundleConfiguration: ConfigurableFileCollection,
        kotlinNativeVersion: String,
        bundleDir: Path,
        reinstallFlag: Boolean,
    ) {
        processToolchain(bundleDir, reinstallFlag, kotlinNativeVersion, kotlinNativeBundleConfiguration)
    }

    private fun processToolchain(
        bundleDir: Path,
        reinstallFlag: Boolean,
        kotlinNativeVersion: String,
        kotlinNativeBundleConfiguration: ConfigurableFileCollection,
    ) {
        val lock =
            NativeDistributionCommonizerLock(bundleDir) { message -> logger.info("Kotlin Native Bundle: $message") }

        lock.withLock {
            val needToReinstall = isSnapshotVersion(parameters.simpleKotlinNativeVersion.get())
            if (needToReinstall) {
                logger.debug("Snapshot version could be changed, to be sure that up-to-date version is used, Kotlin/Native should be reinstalled")
            }

            removeBundleIfNeeded(reinstallFlag || needToReinstall, bundleDir)

            if (!Files.exists(bundleDir.resolve(MARKER_FILE))) {
                val gradleCachesKotlinNativeDir =
                    resolveKotlinNativeConfiguration(kotlinNativeVersion, kotlinNativeBundleConfiguration)

                copyNativeBundleDistribution(gradleCachesKotlinNativeDir, bundleDir)
            }
        }
    }

    private fun removeBundleIfNeeded(
        reinstallFlag: Boolean,
        bundleDir: Path,
    ) {
        if (reinstallFlag && canBeReinstalled) {
            logger.info("Removing Kotlin/Native bundle")
            bundleDir.toFile().deleteRecursively()
            canBeReinstalled = false // we don't need to reinstall k/n if it was reinstalled once during the same build
        }
    }

    private fun resolveKotlinNativeConfiguration(
        kotlinNativeVersion: String,
        kotlinNativeCompilerConfiguration: ConfigurableFileCollection,
    ): Path {
        val resolutionErrorMessage = "Kotlin Native dependency has not been properly resolved. " +
                "Please, make sure that you've declared the repository, which contains $kotlinNativeVersion."

        val gradleCachesKotlinNativeDir = kotlinNativeCompilerConfiguration
            .singleOrNull() ?: error(resolutionErrorMessage)

        return gradleCachesKotlinNativeDir.toPath()
    }

    companion object {
        private var canBeReinstalled: Boolean = true // we can reinstall a k/n bundle once during the build
        internal const val MARKER_FILE = "provisioned.ok"
        val logger = LoggerFactory.getLogger("org.jetbrains.kotlin.gradle.targets.native.toolchain")
        internal fun isSnapshotVersion(kotlinNativeVersion: String): Boolean =
            KotlinToolingVersion(kotlinNativeVersion).maturity == KotlinToolingVersion.Maturity.SNAPSHOT

        internal fun copyNativeBundleDistribution(
            fromDirectory: Path,
            toDirectory: Path,
        ) {
            logger.info("Moving Kotlin/Native bundle from  $fromDirectory to ${toDirectory.toAbsolutePath()}")
            if (Files.isDirectory(toDirectory) && Files.list(toDirectory).use { it.findAny().isPresent }) {
                logger.warn("Kotlin/Native bundle directory ${toDirectory.toAbsolutePath()} is not empty. Native bundle files will be overwritten.")
            }
            unzipTo(fromDirectory, toDirectory.parent)

            checkKotlinNativeVersionWasDownloaded(toDirectory)

            createSuccessfulInstallationFile(toDirectory)

            logger.info("Moved Kotlin/Native bundle from $fromDirectory to ${toDirectory.toAbsolutePath()}")
        }

        private fun checkKotlinNativeVersionWasDownloaded(
            gradleKotlinNativeDir: Path,
        ) {
            //check that native was actually downloaded and unpacked and there is something except .lock file
            if (!Files.exists(gradleKotlinNativeDir) || Files.list(gradleKotlinNativeDir).use { it.count() <= 1 }) {
                error(
                    "Kotlin Native bundle dependency was used. " +
                            "Please provide the corresponding version in 'kotlin.native.version' property instead of any other ways."
                )
            }
        }

        private fun unzipTo(archive: Path, toDirectory: Path) {
            when {
                archive.fileName.toString().endsWith("zip") -> DependencyExtractor().extract(archive.toFile(), toDirectory.toFile(), ArchiveType.ZIP)
                archive.fileName.toString().endsWith(".tar.gz") -> DependencyExtractor().extract(archive.toFile(), toDirectory.toFile(), ArchiveType.TAR_GZ)
                else -> error("Unsupported format for unzipping $archive")
            }
        }

        private fun createSuccessfulInstallationFile(bundleDir: Path) {
            Files.createFile(bundleDir.resolve(MARKER_FILE))
        }
    }
}
