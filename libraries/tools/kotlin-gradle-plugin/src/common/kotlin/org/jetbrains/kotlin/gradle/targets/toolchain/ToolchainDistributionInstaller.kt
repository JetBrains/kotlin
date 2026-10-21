/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.toolchain

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainPlatform
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainVersion
import org.jetbrains.kotlin.gradle.targets.native.internal.KotlinInterprocessDirectoryLock
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Installs tool distributions described by a [ToolchainDistributionLayout] into a shared, machine-wide location.
 *
 * An installation is identified by the requested version and platform, so the very same distribution is
 * reused by all requests in a build, and by all independent Gradle builds on the machine.
 *
 * Installing is safe against concurrent requests, both from the same build and from other processes:
 * the target installation is guarded by a file-based lock, the distribution is unpacked into a temporary
 * directory, and only then atomically renamed to the final location. Therefore, an existing installation
 * directory is always a complete installation.
 */
internal class ToolchainDistributionInstaller(
    private val layout: ToolchainDistributionLayout,
    private val fs: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
    private val logger: Logger,
) {

    /**
     * Returns the directory with the installed [version] of the tool for [platform], installing it if needed.
     *
     * @param installationsDir the directory containing all installations of the tool.
     * @param downloadBaseUrl the base URL of the tool distributions.
     * @param offline whether downloading is forbidden.
     */
    fun install(
        installationsDir: File,
        version: ToolchainVersion,
        platform: ToolchainPlatform,
        downloadBaseUrl: String,
        offline: Boolean,
    ): File {
        val distributionId = layout.distributionName(version, platform)
        val distributionPath = installationsDir.resolve(distributionId)

        if (isCompleteInstallation(distributionPath, version, platform)) return distributionPath

        if (offline) {
            throw IOException(
                "${layout.toolName} ${version.version} for $platform is not installed in '$installationsDir', " +
                        "and the build is running in offline mode, so it cannot be downloaded.\n" +
                        "Either run the build without '--offline', or point the ${layout.toolName} toolchain " +
                        "at a pre-installed ${layout.toolName} distribution."
            )
        }

        // The lock is taken beside the target directory, so that installations of different distributions
        // do not block each other.
        val lockDir = installationsDir.resolve(".$distributionId.lock")
        KotlinInterprocessDirectoryLock(lockDir, logInfo = { logger.info(it) }).withLock {
            // Another process may have completed the installation while the lock was being acquired.
            if (isCompleteInstallation(distributionPath, version, platform)) return@withLock

            val tempDir = File("${distributionPath.path}$TEMP_DIR_SUFFIX")
            tempDir.deleteRecursively()

            try {
                val archivePath = layout.archivePath(version, platform)
                val archive = tempDir.resolve(archivePath.substringAfterLast('/'))
                val archiveUrl = "${downloadBaseUrl.trimEnd('/')}/${archivePath.trimStart('/')}"

                logger.lifecycle("Downloading ${layout.toolName} ${version.version} for $platform from $archiveUrl")
                downloadFile(archiveUrl, archive)

                val unpacked = unpack(archive, tempDir, version, platform)

                val executable = unpacked.resolve(layout.executablePath(version, platform))
                check(executable.isFile) {
                    "The ${layout.toolName} distribution downloaded from '$archiveUrl' does not contain the " +
                            "'${layout.executablePath(version, platform)}' executable"
                }
                if (!platform.isWindows) {
                    executable.setExecutable(true)
                }

                installationsDir.mkdirs()
                try {
                    Files.move(
                        unpacked.toPath(),
                        distributionPath.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE,
                    )
                } catch (e: Exception) {
                    throw IllegalStateException(
                        "${layout.toolName} cannot be installed in the '$distributionPath' directory", e
                    )
                }
            } finally {
                tempDir.deleteRecursively()
            }

            logger.info("Installed ${layout.toolName} ${version.version} for $platform into '$distributionPath'")
        }

        return distributionPath
    }

    /**
     * Unpacks [archive] into [tempDir] and returns the directory that becomes the installation.
     */
    private fun unpack(
        archive: File,
        tempDir: File,
        version: ToolchainVersion,
        platform: ToolchainPlatform,
    ): File {
        val unpackDir = tempDir.resolve(UNPACK_DIR_NAME)

        when (layout.archiveType(platform)) {
            ToolchainArchiveType.RAW_EXECUTABLE -> {
                val executable = unpackDir.resolve(layout.executablePath(version, platform))
                executable.parentFile.mkdirs()
                Files.move(archive.toPath(), executable.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            ToolchainArchiveType.ZIP -> fs.copy {
                it.from(archiveOperations.zipTree(archive))
                it.into(unpackDir)
            }
            ToolchainArchiveType.TAR_GZ -> fs.copy {
                it.from(archiveOperations.tarTree(archive))
                it.into(unpackDir)
            }
            ToolchainArchiveType.TAR_XZ -> {
                val tarFile = tempDir.resolve("${archive.nameWithoutExtension}.tar")
                XZCompressorInputStream(archive.inputStream().buffered()).use { xzIn ->
                    tarFile.outputStream().buffered().use { tarOut -> xzIn.copyTo(tarOut) }
                }
                fs.copy {
                    it.from(archiveOperations.tarTree(tarFile))
                    it.into(unpackDir)
                }
            }
        }

        val rootDirName = layout.archiveRootDirName(version, platform) ?: return unpackDir
        val root = unpackDir.resolve(rootDirName)
        check(root.isDirectory) {
            "The ${layout.toolName} distribution archive '${archive.name}' does not contain the expected " +
                    "'$rootDirName' directory"
        }
        return root
    }

    private fun isCompleteInstallation(
        installationDir: File,
        version: ToolchainVersion,
        platform: ToolchainPlatform,
    ): Boolean = installationDir.isDirectory && installationDir.resolve(layout.executablePath(version, platform)).isFile

    private fun downloadFile(url: String, target: File) {
        target.parentFile.mkdirs()

        var lastFailure: IOException? = null
        repeat(DOWNLOAD_ATTEMPTS) { attempt ->
            try {
                openConnection(url).use { input ->
                    target.outputStream().use(input::copyTo)
                }
                return
            } catch (e: IOException) {
                lastFailure = e
                target.delete()
                logger.info("Failed to download $url (attempt ${attempt + 1} of $DOWNLOAD_ATTEMPTS): ${e.message}")
            }
        }
        throw IOException("Cannot download $url", lastFailure)
    }

    private fun openConnection(url: String) =
        (URL(url).openConnection() as HttpURLConnection).run {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            if (responseCode !in 200..299) {
                val message = "$url returned HTTP $responseCode $responseMessage"
                disconnect()
                throw IOException(message)
            }
            inputStream
        }

    private companion object {
        private const val TEMP_DIR_SUFFIX = ".tmp"
        private const val UNPACK_DIR_NAME = "unpacked"
        private const val DOWNLOAD_ATTEMPTS = 3
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 60_000
    }
}
