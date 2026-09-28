/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.targets.native.internal.KotlinInterprocessDirectoryLock
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.NodeJsToolchainService.Companion.nodeJsExecutableFile
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Installs Node.js distributions into a shared, machine-wide location.
 *
 * An installation is identified by the requested version and platform, so the very same distribution is
 * reused by all requests in a build, and by all independent Gradle builds on the machine.
 *
 * Installing is safe against concurrent requests, both from the same build and from other processes:
 * the target installation is guarded by a file-based lock, the distribution is unpacked into a temporary
 * directory, and only then atomically renamed to the final location. Therefore, an existing installation
 * directory is always a complete installation.
 */
internal class NodeJsDistributionInstaller(
    private val fs: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
    private val logger: Logger,
) {

    /**
     * Returns the directory with the installed [version] of Node.js for [platform], installing it if needed.
     *
     * @param installationsDir the directory containing all Node.js installations.
     * @param downloadBaseUrl the base URL of the Node.js distributions.
     * @param offline whether downloading is forbidden.
     */
    fun install(
        installationsDir: File,
        version: NodeJsVersion,
        platform: BuildPlatform,
        downloadBaseUrl: String,
        offline: Boolean,
    ): File {
        val distributionId = nodeJsDistributionName(version, platform)
        val distributionPath = installationsDir.resolve(distributionId)

        if (isCompleteInstallation(distributionPath, platform)) return distributionPath

        if (offline) {
            throw IOException(
                "Node.js $version for $platform is not installed in '$installationsDir', " +
                        "and the build is running in offline mode, so it cannot be downloaded.\n" +
                        "Either run the build without '--offline', or point the Node.js toolchain " +
                        "at a pre-installed Node.js distribution."
            )
        }

        // The lock is taken beside the target directory, so that installations of different distributions
        // do not block each other.
        val lockDir = installationsDir.resolve(".$distributionId.lock")
        KotlinInterprocessDirectoryLock(lockDir, logInfo = { logger.info(it) }).withLock {
            // Another process may have completed the installation while the lock was being acquired.
            if (isCompleteInstallation(distributionPath, platform)) return@withLock

            if (distributionPath.exists()) {
                logger.info("'$distributionPath' is exist")
            }
            //TODO should we clean the directory at some point?

            val tempDir = File("${distributionPath.path}$TEMP_DIR_SUFFIX")

            try {
                val archive = tempDir.resolve("$distributionId.${nodeJsArchiveExtension(platform)}")
                download(version, platform, downloadBaseUrl, archive)

                archiveOperations.extractNodeJs(fs, archive, tempDir)

                val unpacked = tempDir.resolve(distributionId)
                check(unpacked.isDirectory) {
                    "The Node.js distribution archive '${archive.name}' does not contain the expected " +
                            "'$distributionId' directory"
                }
                setUpNodeJs(logger, archive, unpacked, platform.isWindows, nodeJsExecutableFile(unpacked, platform))

                installationsDir.mkdirs()
                try {
                    Files.move(unpacked.toPath(), distributionPath.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (e: Exception) {
                    throw IllegalStateException("The Node.js can nott be installed in the '$distributionPath' directory", e)
                }
            } finally {
                tempDir.deleteRecursively()
            }

            logger.info("Installed Node.js $version for $platform into '$distributionPath'")
        }

        return distributionPath
    }

    private fun isCompleteInstallation(installationDir: File, platform: BuildPlatform): Boolean =
        installationDir.isDirectory && nodeJsExecutableFile(installationDir, platform).isFile

    private fun download(
        version: NodeJsVersion,
        platform: BuildPlatform,
        downloadBaseUrl: String,
        target: File,
    ) {
        val versionUrl = "${downloadBaseUrl.trimEnd('/')}/v${version.normalized}"
        val archiveUrl = "$versionUrl/${target.name}"

        //TODO use
        logger.lifecycle("Downloading Node.js $version for $platform from $archiveUrl")
        downloadFile(archiveUrl, target)
    }

    //TODO reuse org/jetbrains/kotlin/konan/util/DependencyDownloader.kt
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

    private fun readText(url: String): String = openConnection(url).use { it.reader().readText() }

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

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        private const val TEMP_DIR_SUFFIX = ".tmp"
        private const val DOWNLOAD_ATTEMPTS = 3
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 60_000
    }
}
