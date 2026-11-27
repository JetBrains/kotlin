/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.toolchains

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.targets.native.internal.KotlinInterprocessDirectoryLock
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

/**
 * Configurable Kotlin external toolchain provisioner.
 *
 * A request [R] describes a distribution, such as its version, platform, and binary variant.
 * The injected downloader maps the request to a URL and downloads a single archive into a temporary directory.
 * The unpacker uses the request to extract and prepare the distribution directly in a staging directory.
 * The provisioner validates the staged distribution and atomically moves it to its final installation directory,
 * where other builds can discover and reuse it. Failed attempts are not cached.
 *
 * An existing installation directory that lacks the required files is never replaced, because it may contain
 * user data; provisioning fails instead, asking to remove the directory.
 */
internal class ExternalToolchainProvisioner<R>(
    /** Toolchain name used in diagnostics and logs. */
    private val toolchainName: String,

    /** Base directory where installations are stored and discovered. */
    private val installationsDir: File,

    /** Maps a request to the installation name shared across requests and builds. */
    private val installationName: (R) -> String,

    /**
     * Relative paths of required files in the prepared distribution, used as a sanity check.
     * Users must still trust the URLs from which toolchains are downloaded.
     */
    private val requiredFiles: (R) -> List<String>,

    /** Downloads the archive for a request and may reject unsupported URLs. */
    private val archiveDownloader: ExternalToolchainArchiveDownloader<R>,

    /**
     * Extracts the requested distribution from [archive] directly into [destination].
     * The unpacker selects the archive format and content and prepares the extracted files, for example by
     * setting executable permissions, removing unnecessary files, or accepting licenses.
     * These operations can be composed from smaller functions in the injected lambda.
     */
    private val unpackArchive: (request: R, archive: File, destination: File) -> Unit,
) {
    /**
     * Installation is slow, so computing a value can block other updates to the map's internal tables.
     * This instance is not expected to handle many concurrent requests for different distributions.
     */
    private val installations = ConcurrentHashMap<String, File>()

    fun provision(request: R): File {
        val name = installationName(request)
        return installations.computeIfAbsent(name) {
            install(request, ExternalToolchainInstallationLayout(name))
        }
    }

    private fun install(request: R, layout: ExternalToolchainInstallationLayout): File {
        val expectedFiles = requiredFiles(request)
        if (isInstalledIn(layout.installationDir, expectedFiles)) return layout.installationDir
        checkNoIncompleteInstallation(layout.installationDir, expectedFiles)
        archiveDownloader.checkIfCanBeDownloaded()

        KotlinInterprocessDirectoryLock(layout.lockDir, logInfo = { logger.info("[$toolchainName] $it") }).withLock {
            // Another service or build may have installed the distribution while we waited for the lock.
            if (isInstalledIn(layout.installationDir, expectedFiles)) return@withLock
            checkNoIncompleteInstallation(layout.installationDir, expectedFiles)

            layout.temporaryDir.deleteRecursively()
            try {
                archiveDownloader.download(request, layout.archive)
                unpackArchive(request, layout.archive, layout.stagedDir)
                check(isInstalledIn(layout.stagedDir, expectedFiles)) {
                    "The $toolchainName toolchain archive '${layout.archive}' does not contain the required files: $expectedFiles"
                }

                Files.move(layout.stagedDir.toPath(), layout.installationDir.toPath(), StandardCopyOption.ATOMIC_MOVE)
                logger.info("Installed $toolchainName toolchain into '${layout.installationDir}'")
            } finally {
                layout.temporaryDir.deleteRecursively()
            }
        }
        return layout.installationDir
    }

    override fun toString(): String = "ExternalToolchainProvisioner(toolchainName=$toolchainName)"

    private companion object {
        val logger = Logging.getLogger(ExternalToolchainProvisioner::class.java)
    }

    private fun checkNoIncompleteInstallation(installationDir: File, expectedFiles: List<String>) {
        check(!installationDir.exists()) {
            "The $toolchainName toolchain installation '$installationDir' is incomplete, " +
                    "it does not contain the required files: $expectedFiles. " +
                    "It is not replaced to keep its content; remove it to install the toolchain again."
        }
    }

    private fun isInstalledIn(directory: File, expectedFiles: List<String>): Boolean =
        directory.isDirectory && expectedFiles.all { directory.resolve(it).isFile }

    private inner class ExternalToolchainInstallationLayout(installationName: String) {
        val installationDir: File = installationsDir.resolve(installationName)
        val lockDir: File = installationsDir.resolve(".$installationName.lock")
        val temporaryDir: File = installationsDir.resolve("$installationName.tmp")
        val archive: File = temporaryDir.resolve("$installationName.archive")
        val stagedDir: File = temporaryDir.resolve("installation")
    }
}
