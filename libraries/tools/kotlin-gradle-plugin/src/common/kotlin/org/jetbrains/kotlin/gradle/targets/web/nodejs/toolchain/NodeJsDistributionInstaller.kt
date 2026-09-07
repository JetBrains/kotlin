/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.targets.js.nodejs.computeNodeBinDir
import org.jetbrains.kotlin.gradle.targets.js.nodejs.computeNpmScriptFile
import org.jetbrains.kotlin.gradle.targets.native.internal.KotlinInterprocessDirectoryLock
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.deleteIfExists

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
     * @param verifyDownload whether the downloaded archive must be verified against the published checksums.
     * @param offline whether downloading is forbidden.
     */
    fun install(
        installationsDir: File,
        version: NodeJsVersion,
        platform: BuildPlatform,
        downloadBaseUrl: String,
        verifyDownload: Boolean,
        offline: Boolean,
    ): File {
        val distributionName = nodeJsDistributionName(version, platform)
        val target = installationsDir.resolve(distributionName)

        if (isCompleteInstallation(target, platform)) return target

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
        val lockDir = installationsDir.resolve("$LOCKS_DIR_NAME/$distributionName")
        KotlinInterprocessDirectoryLock(lockDir) { logger.info(it) }.withLock {
            // Another process may have completed the installation while the lock was being acquired.
            if (isCompleteInstallation(target, platform)) return@withLock

            if (target.exists()) {
                logger.info("Removing incomplete Node.js installation '$target'")
                target.deleteRecursively()
            }

            val tempDir = File("${target.path}$TEMP_DIR_SUFFIX")
            // A leftover temporary directory means a previous installation attempt has failed.
            tempDir.deleteRecursively()

            try {
                val archive = tempDir.resolve("$distributionName.${nodeJsArchiveExtension(platform)}")
                download(version, platform, downloadBaseUrl, verifyDownload, archive)
                extract(archive, tempDir)

                val unpacked = tempDir.resolve(distributionName)
                check(unpacked.isDirectory) {
                    "The Node.js distribution archive '${archive.name}' does not contain the expected " +
                            "'$distributionName' directory"
                }

                installationsDir.mkdirs()
                moveAtomically(unpacked.toPath(), target.toPath())
            } finally {
                tempDir.deleteRecursively()
            }

            postProcessInstallation(target, platform)
            logger.info("Installed Node.js $version for $platform into '$target'")
        }

        return target
    }

    private fun isCompleteInstallation(target: File, platform: BuildPlatform): Boolean =
        target.isDirectory && nodeJsExecutableFile(target, platform).isFile

    private fun download(
        version: NodeJsVersion,
        platform: BuildPlatform,
        downloadBaseUrl: String,
        verifyDownload: Boolean,
        target: File,
    ) {
        val versionUrl = "${downloadBaseUrl.trimEnd('/')}/v${version.normalized}"
        val archiveUrl = "$versionUrl/${target.name}"

        logger.lifecycle("Downloading Node.js $version for $platform from $archiveUrl")
        downloadFile(archiveUrl, target)

        if (verifyDownload) {
            verifyChecksum(target, "$versionUrl/$CHECKSUMS_FILE_NAME")
        } else {
            logger.info(
                "Skipping verification of '${target.name}': it is not downloaded from the official " +
                        "Node.js distribution, so the source is trusted as configured"
            )
        }
    }

    /**
     * Verifies [archive] against the SHA-256 checksums published for its Node.js version.
     *
     * @see <a href="https://github.com/nodejs/node#verifying-binaries">Verifying binaries</a>
     */
    private fun verifyChecksum(archive: File, checksumsUrl: String) {
        val checksums = readText(checksumsUrl)
        val expected = checksums.lineSequence()
            .mapNotNull { line ->
                // The format is `<sha256>  <file name>`, the same as produced by `sha256sum`.
                val checksum = line.substringBefore(' ', missingDelimiterValue = "").trim()
                val name = line.substringAfterLast(' ', missingDelimiterValue = "").trim().removePrefix("*")
                if (checksum.isEmpty() || name != archive.name) null else checksum
            }
            .firstOrNull()
            ?: throw IOException(
                "Cannot verify the downloaded Node.js distribution: '${archive.name}' is not listed " +
                        "in the checksums published at $checksumsUrl"
            )

        val actual = archive.sha256()
        if (!expected.equals(actual, ignoreCase = true)) {
            archive.delete()
            throw IOException(
                "The checksum of the downloaded Node.js distribution '${archive.name}' does not match " +
                        "the checksum published at $checksumsUrl.\n" +
                        "  expected: $expected\n" +
                        "  actual:   $actual\n" +
                        "The download is not trusted and has been deleted."
            )
        }
        logger.info("Verified the checksum of '${archive.name}'")
    }

    private fun extract(archive: File, destination: File) {
        val archiveTree = if (archive.name.endsWith("zip")) {
            archiveOperations.zipTree(archive)
        } else {
            archiveOperations.tarTree(archive)
        }
        fs.copy { copy ->
            copy.from(archiveTree)
            copy.into(destination)
        }
        archive.delete()
    }

    private fun moveAtomically(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target)
        }
    }

    private fun postProcessInstallation(installationDir: File, platform: BuildPlatform) {
        if (platform.isWindows) return

        nodeJsExecutableFile(installationDir, platform).setExecutable(true)
        // Gradle's archive operations do not preserve symlinks, so `npm` and `npx` are restored manually.
        fixBrokenSymlinks(installationDir.toPath())
    }

    private fun fixBrokenSymlinks(installationDir: Path) {
        val nodeBinDir = computeNodeBinDir(installationDir, isWindows = false)
        for (name in NODE_MODULE_SCRIPTS) {
            val script = nodeBinDir.resolve(name)
            val target = nodeBinDir.relativize(computeNpmScriptFile(installationDir, name, isWindows = false))
            if (script.deleteIfExists()) {
                script.createSymbolicLinkPointingTo(target)
                logger.info("Created symlink for $name. $script -> $target")
            }
        }
    }

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
        private const val CHECKSUMS_FILE_NAME = "SHASUMS256.txt"
        private const val LOCKS_DIR_NAME = ".locks"
        private const val TEMP_DIR_SUFFIX = ".tmp"
        private const val DOWNLOAD_ATTEMPTS = 3
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 60_000

        private val NODE_MODULE_SCRIPTS = listOf("npm", "npx")
    }
}
