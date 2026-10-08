/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain

import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.DefaultWasmToolsToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsBuildPlatform
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsExecutable
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsRequest
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsVersion
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.userKotlinPersistentDir
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * The default [WasmToolsToolchainService] used by the Kotlin Gradle Plugin.
 *
 * Downloads the requested wasm-tools distribution and installs it into a shared, machine-wide directory,
 * so that a distribution is downloaded once and then reused by all builds on the machine.
 */
@OptIn(ExperimentalWasmDsl::class)
internal abstract class DefaultWasmToolsToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    fs: FileSystemOperations,
    archiveOperations: ArchiveOperations,
) : DefaultWasmToolsToolchainService {

    private val logger = Logging.getLogger(DefaultWasmToolsToolchainServiceImpl::class.java)

    private val installer = WasmToolsDistributionInstaller(fs, archiveOperations, logger)

    /**
     * Installations already provisioned by this build, so that a distribution requested by several
     * consumers is only installed once.
     */
    private val installations = ConcurrentHashMap<WasmToolsDistribution, File>()

    override fun request(wasmToolsRequest: WasmToolsRequest): Provider<WasmToolsExecutable> {
        return providers.provider {
            val distribution = WasmToolsDistribution(
                version = wasmToolsRequest.version.orNull ?: error("A wasm-tools version must be requested"),
                platform = wasmToolsRequest.platform.orNull ?: error("A wasm-tools platform must be requested")
            )
            provision(distribution)
        }
    }

    private fun provision(distribution: WasmToolsDistribution): WasmToolsExecutable {
        val installationDir = installations.computeIfAbsent(distribution) {
            val downloadBaseUrl = parameters.downloadBaseUrl.getOrElse(OFFICIAL_WASM_TOOLS_DOWNLOAD_BASE_URL)
            installer.install(
                installationsDir = parameters.installationDir.get().asFile,
                version = it.version,
                platform = it.platform,
                downloadBaseUrl = downloadBaseUrl,
                offline = parameters.offline.getOrElse(false),
            )
        }

        return objects.newInstance<WasmToolsExecutable>().apply {
            executable.set(wasmToolsExecutableFile(installationDir, distribution.platform).absolutePath)
            version.set(distribution.version)
            platform.set(distribution.platform)
        }
    }

    companion object {
        internal fun registerIfAbsent(
            gradle: Gradle,
            configureBuildServiceParameters: (DefaultWasmToolsToolchainService.Parameters) -> Unit,
        ): Provider<DefaultWasmToolsToolchainServiceImpl> {
            return gradle.sharedServices.registerIfAbsent(
                wasmToolsServiceName,
                DefaultWasmToolsToolchainServiceImpl::class.java
            ) { spec ->
                spec.parameters.downloadBaseUrl.convention(OFFICIAL_WASM_TOOLS_DOWNLOAD_BASE_URL)
                spec.parameters.offline.convention(gradle.startParameter.isOffline)
                configureBuildServiceParameters(spec.parameters)
            }
        }

        internal fun registerIfAbsent(project: Project): Provider<DefaultWasmToolsToolchainServiceImpl> {
            return registerIfAbsent(project.gradle) { parameters ->
                parameters.installationDir.fileProvider(project.wasmToolsToolchainInstallationDir)
                parameters.downloadBaseUrl.set(
                    project.kotlinPropertiesProvider.wasmToolsToolchainDefaultDownloadUrl
                        .orElse(OFFICIAL_WASM_TOOLS_DOWNLOAD_BASE_URL)
                )
            }
        }

        private const val WASM_TOOLS_TOOLCHAINS_DIR_NAME = "toolchains/wasm-tools"

        /**
         * The base URL of the official wasm-tools distributions.
         */
        private const val OFFICIAL_WASM_TOOLS_DOWNLOAD_BASE_URL = "https://github.com/bytecodealliance/wasm-tools/releases/download"

        /**
         * The directory all wasm-tools installations are shared through, `<user.home>/.kotlin/toolchains/wasm-tools`.
         *
         * The base directory can be customized with the `kotlin.user.home` Gradle property,
         * and defaults to `~/.kotlin`.
         */
        private val Project.wasmToolsToolchainInstallationDir: Provider<File>
            get() {
                if (kotlinPropertiesProvider.wasmToolsToolchainDefaultInstallPath.isPresent) {
                    return kotlinPropertiesProvider.wasmToolsToolchainDefaultInstallPath.map { File(it) }
                }

                return providers.provider { userKotlinPersistentDir.resolve(WASM_TOOLS_TOOLCHAINS_DIR_NAME) }
            }
    }

    private data class WasmToolsDistribution(
        val version: WasmToolsVersion,
        val platform: WasmToolsBuildPlatform,
    )
}
