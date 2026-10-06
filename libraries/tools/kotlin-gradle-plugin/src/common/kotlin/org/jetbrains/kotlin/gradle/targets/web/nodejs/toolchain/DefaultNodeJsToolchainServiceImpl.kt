/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.BuildPlatform
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.DefaultNodeJsToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsExecutable
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsRequest
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsVersion
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.userKotlinPersistentDir
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * The default [NodeJsToolchainService] used by the Kotlin Gradle Plugin.
 *
 * Downloads the requested Node.js distribution and installs it into a shared, machine-wide directory,
 * so that a distribution is downloaded once and then reused by all builds on the machine.
 */
@OptIn(ExperimentalNodeJsToolchainDsl::class)
internal abstract class DefaultNodeJsToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    fs: FileSystemOperations,
    archiveOperations: ArchiveOperations,
) : DefaultNodeJsToolchainService {

    private val logger = Logging.getLogger(DefaultNodeJsToolchainServiceImpl::class.java)

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
                platform = nodeJsRequest.platform.orNull ?: error("A Node.js platform must be requested")
            )
            provision(distribution)
        }
    }

    private fun provision(distribution: NodeJsDistribution): NodeJsExecutable {
        logger.reportDiagnosticWhenNodeJsVersionUnsupported(distribution.version)

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
        internal fun registerIfAbsent(
            gradle: Gradle,
            configureBuildServiceParameters: (DefaultNodeJsToolchainService.Parameters) -> Unit,
        ): Provider<DefaultNodeJsToolchainServiceImpl> {
            return gradle.sharedServices.registerIfAbsent(nodeJsServiceName, DefaultNodeJsToolchainServiceImpl::class.java) { spec ->
                spec.parameters.downloadBaseUrl.convention(OFFICIAL_NODE_JS_DOWNLOAD_BASE_URL)
                spec.parameters.offline.convention(gradle.startParameter.isOffline)
                configureBuildServiceParameters(spec.parameters)
            }
        }

        internal fun registerIfAbsent(project: Project): Provider<DefaultNodeJsToolchainServiceImpl> {
            return registerIfAbsent(project.gradle) { parameters ->
                parameters.installationDir.fileProvider(project.nodeJsToolchainInstallationDir)
                parameters.downloadBaseUrl.set(
                    project.kotlinPropertiesProvider.nodeJsToolchainDefaultDownloadUrl.orElse(OFFICIAL_NODE_JS_DOWNLOAD_BASE_URL)
                )
            }
        }

        private const val NODE_JS_TOOLCHAINS_DIR_NAME = "toolchains/nodejs"

        /**
         * The base URL of the official Node.js distributions.
         */
        private const val OFFICIAL_NODE_JS_DOWNLOAD_BASE_URL = "https://nodejs.org/dist"

        /**
         * The directory all Node.js installations are shared through, `<user.home>/.kotlin/toolchains/nodejs`.
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
