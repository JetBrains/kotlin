/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.NodeJsToolchainService.Companion.nodeJsExecutableFile
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.NodeJsToolchainService.Companion.nodeJsServiceName
import org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain.NodeJsToolchainService.Companion.warnIfNodeJsUnsupported
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.userKotlinPersistentDir
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * The default [NodeJsToolchainService] used by the Kotlin Gradle Plugin.
 *
 * It downloads the requested Node.js distribution and installs it into a shared, machine-wide directory,
 * so that a distribution is downloaded once and then reused by all builds on the machine.
 *
 * Downloads from the official Node.js distribution are verified against the officially published
 * SHA-256 checksums. When a custom [Parameters.downloadBaseUrl] is configured, the source is trusted
 * as configured and the download is not verified.
 */
abstract class DefaultNodeJsToolchainService @Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    fs: FileSystemOperations,
    archiveOperations: ArchiveOperations,
) : NodeJsToolchainService<DefaultNodeJsToolchainService.Parameters> {

    abstract class Parameters : NodeJsToolchainService.Parameters {

        /**
         * The platform used for requests that do not specify one explicitly.
         *
         * Defaults to the platform of the machine running the build.
         */
        abstract val defaultPlatform: Property<BuildPlatform>

        /**
         * The directory containing all Node.js installations.
         *
         * Defaults to `$KOTLIN_CACHE_DIR/toolchains/nodejs`.
         */
        abstract val installationDir: DirectoryProperty

        /**
         * The base URL the distributions are downloaded from.
         *
         * Defaults to the official Node.js distribution, `https://nodejs.org/dist`.
         */
        abstract val downloadBaseUrl: Property<String>

        /**
         * Whether the build is running in offline mode.
         *
         * When `true`, an already installed distribution is reused, and a missing one fails the build
         * instead of being downloaded.
         */
        abstract val offline: Property<Boolean>

        // note: no version. NodeJsToolchainService is version agnostic.
        // A default is defined per use-case (e.g. resolving npm dependencies)
    }

    private val logger = Logging.getLogger(DefaultNodeJsToolchainService::class.java)

    private val installer = NodeJsDistributionInstaller(fs, archiveOperations, logger)

    /**
     * Installations already provisioned by this build, so that a distribution requested by several
     * consumers is only installed once.
     */
    private val installations = ConcurrentHashMap<NodeJsDistribution, File>()

    override fun request(nodeJsRequest: NodeJsRequest): Provider<NodeJsExecutable> {
        return providers.provider {
            val distribution = NodeJsDistribution(
                version = nodeJsRequest.version.orNull ?: error("A Node.js version must be requested"),
                platform = nodeJsRequest.platform.orElse(parameters.defaultPlatform).orNull ?: error("A Node.js platform must be requested")
            )
            provision(distribution)
        }
    }

    private fun provision(distribution: NodeJsDistribution): NodeJsExecutable {
        logger.warnIfNodeJsUnsupported(distribution.version)

        val installationDir = installations.computeIfAbsent(distribution) {
            val downloadBaseUrl = parameters.downloadBaseUrl.getOrElse(OFFICIAL_NODE_JS_DOWNLOAD_BASE_URL)
            installer.install(
                installationsDir = parameters.installationDir.get().asFile,
                version = it.version,
                platform = it.platform,
                downloadBaseUrl = downloadBaseUrl,
                offline = parameters.offline.getOrElse(false),
            )
        }

        return objects.newInstance<NodeJsExecutable>().apply {
            executable.set(nodeJsExecutableFile(installationDir, distribution.platform).absolutePath)
            version.set(distribution.version)
            platform.set(distribution.platform)
        }
    }

    companion object {
        internal fun registerIfAbsent(project: Project): Provider<DefaultNodeJsToolchainService> {

            return project.gradle.sharedServices.registerIfAbsent(nodeJsServiceName, DefaultNodeJsToolchainService::class.java) { spec ->
                spec.parameters.defaultPlatform.set(project.providers.detectBuildPlatform())
                spec.parameters.installationDir.fileProvider(project.nodeJsToolchainInstallationDir)
                spec.parameters.downloadBaseUrl.set(
                    project.kotlinPropertiesProvider.nodeJsToolchainDefaultDownloadUrl.orElse(
                        OFFICIAL_NODE_JS_DOWNLOAD_BASE_URL
                    )
                )
                spec.parameters.offline.set(project.gradle.startParameter.isOffline)
            }
        }

        private const val NODE_JS_TOOLCHAINS_DIR_NAME = "toolchains/nodejs"

        /**
         * The base URL of the official Node.js distributions.
         */
        private const val OFFICIAL_NODE_JS_DOWNLOAD_BASE_URL = "https://nodejs.org/dist"

        /**
         * The directory all Node.js installations are shared through, `$KOTLIN_CACHE_DIR/toolchains/nodejs`.
         *
         * The base directory can be customized with the `kotlin.user.home` Gradle property,
         * and defaults to `~/.kotlin`.
         */
        private val Project.nodeJsToolchainInstallationDir: Provider<File>
            get() {
                if (kotlinPropertiesProvider.nodeJsToolchainDefaultInstallPath.isPresent) {
                    return kotlinPropertiesProvider.nodeJsToolchainDefaultInstallPath.map { File(it) }
                }

                return providers.provider { userKotlinPersistentDir.resolve(NODE_JS_TOOLCHAINS_DIR_NAME) }
            }
    }

    private data class NodeJsDistribution(
        val version: NodeJsVersion,
        val platform: BuildPlatform,
    )
}
