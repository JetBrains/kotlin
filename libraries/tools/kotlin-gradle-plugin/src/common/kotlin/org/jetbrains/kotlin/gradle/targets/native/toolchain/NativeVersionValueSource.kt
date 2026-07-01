/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.jetbrains.kotlin.gradle.targets.native.internal.KotlinInterprocessDirectoryLock
import org.jetbrains.kotlin.konan.util.ArchiveType
import org.jetbrains.kotlin.konan.util.DependencyExtractor
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicBoolean

internal abstract class NativeVersionValueSource :
    ValueSource<String, NativeVersionValueSource.Params> {

    interface Params : ValueSourceParameters {
        val bundleDirectory: Property<String>
        val reinstallBundle: Property<Boolean>
        val simpleKotlinNativeVersion: Property<String>
        val kotlinNativeVersion: Property<String>
        val kotlinNativeCompilerConfiguration: ConfigurableFileCollection
    }

    override fun obtain(): String {
        val kotlinNativeVersion = parameters.kotlinNativeVersion.get()
        prepareKotlinNativeBundle(
            parameters.kotlinNativeCompilerConfiguration,
            kotlinNativeVersion,
            File(parameters.bundleDirectory.get()),
            parameters.reinstallBundle.get(),
        )
        return kotlinNativeVersion
    }

    /**
     * Installs the Kotlin/Native bundle into [bundleDir] unless it is already there.
     *
     * @param kotlinNativeBundleConfiguration configuration resolving to the Kotlin/Native bundle archive
     * @param kotlinNativeVersion the Kotlin/Native version to install
     * @param bundleDir target directory for the bundle
     * @param reinstallFlag whether to reinstall the bundle
     */
    private fun prepareKotlinNativeBundle(
        kotlinNativeBundleConfiguration: ConfigurableFileCollection,
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
        kotlinNativeBundleConfiguration: ConfigurableFileCollection,
    ) {
        val lock =
            KotlinInterprocessDirectoryLock(stagingDirectoryFor(bundleDir)) { message -> logger.info("Kotlin Native Bundle: $message") }

        lock.withLock {
            val needToReinstall = isSnapshotVersion(parameters.simpleKotlinNativeVersion.get())
            if (needToReinstall) {
                logger.debug("Snapshot version could be changed, to be sure that up-to-date version is used, Kotlin/Native should be reinstalled")
            }

            removeBundleIfNeeded(reinstallFlag || needToReinstall, bundleDir)

            if (!bundleDir.resolve(MARKER_FILE).exists()) {
                val bundleArchive =
                    resolveKotlinNativeConfiguration(kotlinNativeVersion, kotlinNativeBundleConfiguration)

                extractNativeBundleDistribution(bundleArchive, bundleDir)
            }
        }
    }

    private fun removeBundleIfNeeded(
        reinstallFlag: Boolean,
        bundleDir: File,
    ) {
        if (reinstallFlag && canBeReinstalled.compareAndSet(true, false)) {
            logger.info("Removing Kotlin/Native bundle")
            bundleDir.deleteRecursively()
        }
    }

    private fun resolveKotlinNativeConfiguration(
        kotlinNativeVersion: String,
        kotlinNativeBundleConfiguration: ConfigurableFileCollection,
    ): File {
        val resolutionErrorMessage = "Kotlin Native dependency has not been properly resolved. " +
                "Please, make sure that you've declared the repository, which contains $kotlinNativeVersion."

        return kotlinNativeBundleConfiguration.singleOrNull() ?: error(resolutionErrorMessage)
    }

    companion object {
        private val canBeReinstalled: AtomicBoolean = AtomicBoolean(true) // we can reinstall a k/n bundle once during the build
        internal const val MARKER_FILE = "provisioned.ok"
        val logger: Logger = LoggerFactory.getLogger("org.jetbrains.kotlin.gradle.targets.native.toolchain")
        internal fun isSnapshotVersion(kotlinNativeVersion: String): Boolean =
            KotlinToolingVersion(kotlinNativeVersion).maturity == KotlinToolingVersion.Maturity.SNAPSHOT

        /**
         * Directory for the install `.lock` and the temporary extraction workspace. It sits beside
         * [bundleDir], not inside it, because the lock holds `.lock` open for the whole install and
         * Windows can't rename or delete a directory that has an open handle. [publishBundleAtomically]
         * renames and deletes [bundleDir], so the lock has to stay out of it. KT-86251.
         */
        private fun stagingDirectoryFor(bundleDir: File): File =
            bundleDir.resolveSibling("${bundleDir.name}.staging")

        internal fun extractNativeBundleDistribution(
            archiveFile: File,
            toDirectory: File,
        ) {
            logger.info("Installing Kotlin/Native bundle from $archiveFile to ${toDirectory.absolutePath}")

            // Already installed by another process or daemon.
            if (toDirectory.resolve(MARKER_FILE).exists()) {
                logger.info("Kotlin/Native bundle already installed at ${toDirectory.absolutePath}")
                return
            }

            // Extract inside the staging dir (the same sibling that holds the install .lock), so
            // the bundle and toDirectory share a filesystem and publishing is a cheap atomic
            // rename. A unique subdir keeps concurrent extractions from colliding.
            val stagingDir = stagingDirectoryFor(toDirectory).also { it.mkdirs() }
            val tmpParent = Files.createTempDirectory(
                stagingDir.toPath(),
                "${archiveFile.archiveBaseName}.tmp."
            ).toFile()

            try {
                unzipTo(archiveFile, tmpParent)

                val tmpContents = tmpParent.listFiles()
                    ?: error(
                        "Failed to list contents of temp extraction directory ${tmpParent.absolutePath}. " +
                                "This may indicate a filesystem I/O error."
                    )
                val extractedDir = tmpContents.singleOrNull()
                    ?: error(
                        "Kotlin/Native bundle archive $archiveFile did not contain exactly one " +
                                "top-level directory (found ${tmpContents.size} entries in ${tmpParent.absolutePath})"
                    )

                requireExtractedBundleIsNonEmpty(extractedDir)
                requireKotlinNativeVersionWasDownloaded(extractedDir, toDirectory.name)

                // Stamp the marker inside the temp dir so dir + marker move into place together.
                createSuccessfulInstallationFile(extractedDir)

                publishBundleAtomically(extractedDir, toDirectory)

                logger.info("Installed Kotlin/Native bundle at ${toDirectory.absolutePath}")
            } finally {
                tmpParent.deleteRecursively()
            }
        }

        /**
         * Renames [extractedDir] onto [toDirectory] in a single move, so a reader never sees a
         * half-written bundle: [toDirectory] is either absent or complete with [MARKER_FILE].
         *
         * Called under the install lock and only when [toDirectory] had no marker. So a
         * [toDirectory] that exists here is a leftover from an interrupted or old in-place install.
         * The compiler writes generated caches (`klib/cache/...`) only after the marker is stamped,
         * so a marker-less leftover holds no caches and we can delete it. A [toDirectory] that
         * already has a marker is a finished install that may hold caches, so we keep it and drop
         * our copy instead. KT-86251.
         */
        private fun publishBundleAtomically(extractedDir: File, toDirectory: File) {
            if (toDirectory.exists()) {
                if (toDirectory.resolve(MARKER_FILE).exists()) {
                    logger.info(
                        "Kotlin/Native bundle already installed at ${toDirectory.absolutePath}; " +
                                "keeping it and discarding the freshly extracted copy."
                    )
                    return
                }
                logger.info("Removing incomplete Kotlin/Native bundle at ${toDirectory.absolutePath}")
                toDirectory.deleteRecursively()
            }
            atomicMove(extractedDir, toDirectory)
        }

        private fun atomicMove(from: File, to: File) {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE)
        }

        private fun requireExtractedBundleIsNonEmpty(extractedDir: File) {
            val contents = extractedDir.list()
            if (contents == null || contents.isEmpty()) {
                error(
                    "Kotlin/Native bundle extraction produced an empty or missing directory at " +
                            "${extractedDir.absolutePath}. " +
                            "Ensure 'kotlin.native.version' is set to a valid published version."
                )
            }
        }

        private fun requireKotlinNativeVersionWasDownloaded(extractedDir: File, expectedName: String) {
            // Wrong version resolved: the archive's top-level dir won't match the bundle dir name.
            if (extractedDir.name != expectedName) {
                error(
                    "Kotlin/Native bundle dependency was used. " +
                            "Please provide the corresponding version in 'kotlin.native.version' property instead of any other ways."
                )
            }
        }

        private fun unzipTo(archive: File, toDirectory: File) {
            when {
                archive.name.endsWith(".zip") -> DependencyExtractor().extract(archive, toDirectory, ArchiveType.ZIP)
                archive.name.endsWith(".tar.gz") -> DependencyExtractor().extract(archive, toDirectory, ArchiveType.TAR_GZ)
                else -> error("Unsupported format for unzipping $archive")
            }
        }

        private fun createSuccessfulInstallationFile(bundleDir: File) {
            bundleDir.resolve(MARKER_FILE).createNewFile()
        }

        private val File.archiveBaseName: String
            get() = when {
                name.endsWith(".tar.gz", ignoreCase = true) -> name.dropLast(".tar.gz".length)
                name.endsWith(".zip", ignoreCase = true) -> name.dropLast(".zip".length)
                else -> nameWithoutExtension
            }
    }
}
