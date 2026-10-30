/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.toolchain

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainExecutable
import org.jetbrains.kotlin.gradle.targets.native.internal.KotlinInterprocessDirectoryLock
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

/**
 * The settings every download-based toolchain service has. A tool declares its own settings class implementing it.
 */
internal interface DownloadedToolchainSettings {

    /**
     * The directory containing all installations of the tool.
     */
    val installationDir: File

    /**
     * The base URL the distributions are downloaded from.
     */
    val downloadBaseUrl: String

    /**
     * Whether downloading is forbidden.
     */
    val offline: Boolean
}

/**
 * Resolves [path] against [DownloadedToolchainSettings.downloadBaseUrl].
 */
internal fun DownloadedToolchainSettings.downloadUrl(path: String): String =
    "${downloadBaseUrl.trimEnd('/')}/${path.trimStart('/')}"

/**
 * The Gradle services a toolchain service provides to an [ExternalToolchainProvisioner].
 */
internal class ToolchainProvisioningServices(
    val fs: FileSystemOperations,
    val archiveOperations: ArchiveOperations,
    val logger: Logger,
)

/**
 * Makes the requested toolchain available on the machine.
 */
internal fun interface ExternalToolchainProvisioner<in T : ExternalToolchainForKotlin> {

    /**
     * Returns the directory with the installation of [toolchain], provisioning it if needed.
     *
     * May download the toolchain, so must only be called at execution time.
     */
    fun provision(toolchain: T, services: ToolchainProvisioningServices): File
}

/**
 * Provisions [toolchain] and returns its executable.
 */
internal fun <T : ExternalToolchainForKotlin> ExternalToolchainProvisioner<T>.provisionExecutable(
    toolchain: T,
    services: ToolchainProvisioningServices,
    objects: ObjectFactory,
): ToolchainExecutable = objects.toolchainExecutable(
    executable = provision(toolchain, services).resolve(toolchain.executablePath).absolutePath,
    version = toolchain.version,
    platform = toolchain.platform,
)

/**
 * Provisions a toolchain by downloading its archive with [archiveDownloader], and extracting it with [extractor]
 * into a shared, machine-wide location, [installationsDir].
 *
 * An installation is identified by [ExternalToolchainForKotlin.installationName], so the very same distribution is
 * reused by all requests in a build, and by all independent Gradle builds on the machine.
 *
 * Provisioning is transactional, and safe against concurrent requests, both from the same build and from other
 * processes: the target installation is guarded by a file-based lock, the distribution is downloaded and extracted
 * into a temporary directory, and only then atomically renamed to the final location. Therefore, an existing
 * installation directory is always a complete installation.
 *
 * Installations already provisioned by this provisioner are cached, so that a distribution requested by several
 * consumers is only provisioned once.
 *
 * @param installationsDir the directory containing all installations of the tool.
 * @param offline whether downloading is forbidden.
 */
internal class DefaultExternalProvisioner<T : ExternalToolchainForKotlin>(
    private val archiveDownloader: ExternalToolchainDownloader<T>,
    private val extractor: ExternalToolchainExtractor<T>,
    private val installationsDir: File,
    private val offline: Boolean,
) : ExternalToolchainProvisioner<T> {

    private val installations = ConcurrentHashMap<T, File>()

    override fun provision(toolchain: T, services: ToolchainProvisioningServices): File =
        installations.computeIfAbsent(toolchain) { install(it, services) }

    private fun install(toolchain: T, services: ToolchainProvisioningServices): File {
        val installationName = toolchain.installationName
        val installation = installationsDir.resolve(installationName)

        if (toolchain.isInstalledIn(installation)) return installation

        if (offline) {
            val toolName = toolchain.toolName
            throw IOException(
                "$toolName ${toolchain.version.version} for ${toolchain.platform} is not installed in '$installationsDir', " +
                        "and the build is running in offline mode, so it cannot be downloaded.\n" +
                        "Either run the build without '--offline', or point the $toolName toolchain " +
                        "at a pre-installed $toolName distribution."
            )
        }

        // The lock is taken beside the target directory, so that installations of different distributions
        // do not block each other.
        val lockDir = installationsDir.resolve(".$installationName.lock")
        KotlinInterprocessDirectoryLock(lockDir, logInfo = { services.logger.info(it) }).withLock {
            // Another process may have completed the installation while the lock was being acquired.
            if (toolchain.isInstalledIn(installation)) return@withLock

            val tempDir = File("${installation.path}$TEMP_DIR_SUFFIX")
            tempDir.deleteRecursively()

            try {
                val archive = archiveDownloader.download(toolchain, tempDir, services.logger)

                val staged = tempDir.resolve(STAGED_DIR_NAME)
                extractor.extract(
                    ToolchainExtractionContext(toolchain, archive, services.fs, services.archiveOperations),
                    staged,
                )

                installationsDir.mkdirs()
                try {
                    Files.move(
                        staged.toPath(),
                        installation.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE,
                    )
                } catch (e: Exception) {
                    throw IllegalStateException("${toolchain.toolName} cannot be installed in the '$installation' directory", e)
                }
            } finally {
                tempDir.deleteRecursively()
            }

            services.logger.info(
                "Installed ${toolchain.toolName} ${toolchain.version.version} for ${toolchain.platform} into '$installation'"
            )
        }

        return installation
    }

    private fun ExternalToolchainForKotlin.isInstalledIn(installation: File): Boolean =
        installation.isDirectory && installation.resolve(executablePath).isFile

    private companion object {
        private const val TEMP_DIR_SUFFIX = ".tmp"
        private const val STAGED_DIR_NAME = "installation"
    }
}
