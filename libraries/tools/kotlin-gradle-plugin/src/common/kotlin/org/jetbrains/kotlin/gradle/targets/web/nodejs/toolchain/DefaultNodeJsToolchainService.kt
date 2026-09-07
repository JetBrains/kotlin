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
import org.jetbrains.kotlin.gradle.utils.newInstance
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

    override fun request(configure: NodeJsRequest.() -> Unit): Provider<NodeJsExecutable> {
        val request = objects.newInstance<NodeJsRequest>()
        request.platform.convention(parameters.defaultPlatform)
        request.configure()

        val version = request.version
        val platform = request.platform

        return providers.provider {
            val distribution = NodeJsDistribution(
                version = version.orNull ?: error("A Node.js version must be requested"),
                platform = platform.orNull ?: error("A Node.js platform must be requested"),
            )
            provision(distribution)
        }
    }

    private fun provision(distribution: NodeJsDistribution): NodeJsExecutable {
        warnIfUnsupported(distribution.version)

        val installationDir = installations.computeIfAbsent(distribution) {
            val downloadBaseUrl = parameters.downloadBaseUrl.getOrElse(OFFICIAL_NODE_JS_DOWNLOAD_BASE_URL)
            installer.install(
                installationsDir = parameters.installationDir.get().asFile,
                version = it.version,
                platform = it.platform,
                downloadBaseUrl = downloadBaseUrl,
                verifyDownload = downloadBaseUrl.trimEnd('/') == OFFICIAL_NODE_JS_DOWNLOAD_BASE_URL,
                offline = parameters.offline.getOrElse(false),
            )
        }

        return objects.newInstance<NodeJsExecutable>().apply {
            executable.set(nodeJsExecutableFile(installationDir, distribution.platform).absolutePath)
            version.set(distribution.version)
            platform.set(distribution.platform)
        }
    }

    private fun warnIfUnsupported(version: NodeJsVersion) {
        val majorVersion = version.majorVersion
        if (majorVersion != null && majorVersion < MINIMAL_SUPPORTED_NODE_JS_MAJOR_VERSION) {
            logger.warn(
                "w: Node.js $version is not supported by the Kotlin Gradle Plugin. " +
                        "The minimal supported version is $MINIMAL_SUPPORTED_NODE_JS_MAJOR_VERSION."
            )
        }
    }

    companion object {
        internal fun registerIfAbsent(project: Project): Provider<DefaultNodeJsToolchainService> {
            val defaultPlatform = project.providers.detectBuildPlatform()
            val installationDir = project.nodeJsToolchainInstallationDir
            val offline = project.gradle.startParameter.isOffline

            return project.gradle.sharedServices.registerIfAbsent(nodeJsServiceName, DefaultNodeJsToolchainService::class.java) { spec ->
                spec.parameters.defaultPlatform.set(defaultPlatform)
                spec.parameters.installationDir.fileProvider(installationDir)
                spec.parameters.downloadBaseUrl.convention(OFFICIAL_NODE_JS_DOWNLOAD_BASE_URL)
                spec.parameters.offline.convention(offline)
            }
        }

        private const val NODE_JS_TOOLCHAINS_DIR_NAME = "toolchains/nodejs"
        private const val DEFAULT_KOTLIN_USER_HOME_DIR_NAME = ".kotlin"

        /**
         * The directory all Node.js installations are shared through, `$KOTLIN_CACHE_DIR/toolchains/nodejs`.
         *
         * The base directory can be customized with the `kotlin.user.home` Gradle property,
         * and defaults to `~/.kotlin`.
         */
        private val Project.nodeJsToolchainInstallationDir: Provider<File>
            get() {
                val customUserHome = kotlinPropertiesProvider.kotlinUserHomeDir
                val userHome = if (customUserHome != null) {
                    providers.provider { File(customUserHome) }
                } else {
                    providers.systemProperty("user.home").map { File(it).resolve(DEFAULT_KOTLIN_USER_HOME_DIR_NAME) }
                }
                return userHome.map { it.resolve(NODE_JS_TOOLCHAINS_DIR_NAME) }
            }
    }

    private data class NodeJsDistribution(
        val version: NodeJsVersion,
        val platform: BuildPlatform,
    )
}
