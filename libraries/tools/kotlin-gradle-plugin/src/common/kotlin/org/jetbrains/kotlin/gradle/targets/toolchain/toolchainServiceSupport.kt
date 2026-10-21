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
import org.jetbrains.kotlin.gradle.dsl.toolchain.DownloadedToolchainParameters
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainExecutable
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainPlatform
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainRequest
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainVersion
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Provisions tool distributions for a download-based toolchain service.
 *
 * The tool-specific part is the [ToolchainDistributionLayout]; everything else - request validation, caching of
 * the installations already provisioned by this build, and the installation itself - is shared between all tools.
 *
 * @param layoutFor the layout of the tool. It is a function, because a layout may depend on the service parameters,
 * for example on the D8 edition.
 */
internal class DownloadedToolchainProvisioner(
    private val objects: ObjectFactory,
    private val fs: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
    private val logger: Logger,
    private val layoutFor: (DownloadedToolchainParameters) -> ToolchainDistributionLayout,
) {

    /**
     * Installations already provisioned by this build, so that a distribution requested by several
     * consumers is only installed once.
     */
    private val installations = ConcurrentHashMap<ToolchainDistribution, File>()

    fun provision(request: ToolchainRequest, parameters: DownloadedToolchainParameters): ToolchainExecutable {
        val layout = layoutFor(parameters)
        val distribution = ToolchainDistribution(
            version = request.version.orNull ?: error("A ${layout.toolName} version must be requested"),
            platform = request.platform.orNull ?: error("A ${layout.toolName} platform must be requested"),
        )

        val installationDir = installations.computeIfAbsent(distribution) {
            ToolchainDistributionInstaller(layout, fs, archiveOperations, logger).install(
                installationsDir = parameters.installationDir.get().asFile,
                version = it.version,
                platform = it.platform,
                downloadBaseUrl = parameters.downloadBaseUrl.getOrElse(layout.defaultDownloadBaseUrl),
                offline = parameters.offline.getOrElse(false),
            )
        }

        return objects.toolchainExecutable(
            executable = installationDir.resolve(layout.executablePath(distribution.version, distribution.platform))
                .absolutePath,
            version = distribution.version,
            platform = distribution.platform,
        )
    }

    private data class ToolchainDistribution(
        val version: ToolchainVersion,
        val platform: ToolchainPlatform,
    )
}

internal fun ObjectFactory.toolchainExecutable(
    executable: String,
    version: ToolchainVersion,
    platform: ToolchainPlatform,
): ToolchainExecutable = newInstance<ToolchainExecutable>().apply {
    this.executable.set(executable)
    this.version.set(version)
    this.platform.set(platform)
}
