/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.apache.commons.io.file.FilesUncheck.copy
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionCommonizerLock
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.StandardCopyOption

internal abstract class NativeVersionValueSource :
    ValueSource<String, NativeVersionValueSource.Params> {

    interface Params : ValueSourceParameters {
        val bundleDirectory: Property<String>
        val reinstallBundle: Property<Boolean>
        val simpleKotlinNativeVersion: Property<String>
        val kotlinNativeVersion: Property<String>
        val kotlinNativeCompilerConfiguration: Property<ConfigurableFileCollection>
//        val kotlinNativeCompilerConfiguration: ConfigurableFileCollection
    }

    override fun obtain(): String {
        val kotlinNativeVersion = parameters.kotlinNativeVersion.get()
        prepareKotlinNativeBundle(
//            parameters.kotlinNativeCompilerConfiguration,
            parameters.kotlinNativeCompilerConfiguration.get(),
            kotlinNativeVersion,
            File(parameters.bundleDirectory.get()),
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
        kotlinNativeBundleConfiguration: FileCollection,
        kotlinNativeVersion: String,
        bundleDir: File,
        reinstallFlag: Boolean,
    ) {
        processToolchain(bundleDir, reinstallFlag, kotlinNativeVersion, kotlinNativeBundleConfiguration)
    }

    private fun processToolchain(
        bundleDir: File,
        reinstallFlag: Boolean,
        kotlinNativeVersion: String,
        kotlinNativeBundleConfiguration: FileCollection,
    ) {
        val lock =
            NativeDistributionCommonizerLock(bundleDir) { message -> logger.info("Kotlin Native Bundle: $message") }

        lock.withLock {
            val needToReinstall = isSnapshotVersion(parameters.simpleKotlinNativeVersion.get())
            if (needToReinstall) {
                logger.debug("Snapshot version could be changed, to be sure that up-to-date version is used, Kotlin/Native should be reinstalled")
            }

            removeBundleIfNeeded(reinstallFlag || needToReinstall, bundleDir)

            if (!bundleDir.resolve(MARKER_FILE).exists()) {
                val gradleCachesKotlinNativeDir =
                    resolveKotlinNativeConfiguration(kotlinNativeVersion, kotlinNativeBundleConfiguration)

                copyNativeBundleDistribution(gradleCachesKotlinNativeDir, bundleDir)
            }
        }
    }

    private fun removeBundleIfNeeded(
        reinstallFlag: Boolean,
        bundleDir: File,
    ) {
        if (reinstallFlag && canBeReinstalled) {
            logger.info("Removing Kotlin/Native bundle")
            bundleDir.deleteRecursively()
            canBeReinstalled = false // we don't need to reinstall k/n if it was reinstalled once during the same build
        }
    }

    private fun resolveKotlinNativeConfiguration(
        kotlinNativeVersion: String,
        kotlinNativeCompilerConfiguration: FileCollection,
    ): File {
        val resolutionErrorMessage = "Kotlin Native dependency has not been properly resolved. " +
                "Please, make sure that you've declared the repository, which contains $kotlinNativeVersion."

        val gradleCachesKotlinNativeDir = kotlinNativeCompilerConfiguration
            .singleOrNull()
            ?.resolve(kotlinNativeVersion)
            ?: error(resolutionErrorMessage)

        if (!gradleCachesKotlinNativeDir.exists()) {
            error(
                "Kotlin Native bundle dependency was used. " +
                        "Please provide the corresponding version in 'kotlin.native.version' property instead of any other ways."
            )
        }
        return gradleCachesKotlinNativeDir
    }

    companion object {
        private var canBeReinstalled: Boolean = true // we can reinstall a k/n bundle once during the build
        internal const val MARKER_FILE = "provisioned.ok"
        val logger = LoggerFactory.getLogger("org.jetbrains.kotlin.gradle.targets.native.toolchain")
        internal fun isSnapshotVersion(kotlinNativeVersion: String): Boolean =
            KotlinToolingVersion(kotlinNativeVersion).maturity == KotlinToolingVersion.Maturity.SNAPSHOT

        internal fun copyNativeBundleDistribution(
            fromDirectory: File,
            toDirectory: File,
        ) {
            logger.info("Moving Kotlin/Native bundle from tmp directory $fromDirectory to ${toDirectory.absolutePath}")
            if (!toDirectory.list().isNullOrEmpty()) {
                logger.warn("Kotlin/Native bundle directory ${toDirectory.absolutePath} is not empty. Native bundle files will be overwritten.")
            }

            fromDirectory.walk().forEach { sourceFile ->
                val relativePath = sourceFile.toRelativeString(fromDirectory)
                val bundleDirFile = toDirectory.resolve(relativePath)
                when {
                    sourceFile.isFile -> copy(sourceFile.toPath(), bundleDirFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    sourceFile.isDirectory -> bundleDirFile.mkdir()
                }
            }

            createSuccessfulInstallationFile(toDirectory)

            logger.info("Moved Kotlin/Native bundle from $fromDirectory to ${toDirectory.absolutePath}")
        }

        private fun createSuccessfulInstallationFile(bundleDir: File) {
            bundleDir.resolve(MARKER_FILE).createNewFile()
        }
    }
}
