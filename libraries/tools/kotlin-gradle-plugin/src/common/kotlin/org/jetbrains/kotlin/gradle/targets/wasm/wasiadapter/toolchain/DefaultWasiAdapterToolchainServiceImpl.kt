/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasiadapter.toolchain

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.userKotlinPersistentDir
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * The default [DefaultWasiAdapterToolchainService] used by the Kotlin Gradle Plugin.
 *
 * Downloads the requested WASI preview1 adapter module and installs it into a shared,
 * machine-wide directory, so that an artifact is downloaded once and then reused by all builds on the machine.
 */
@OptIn(ExperimentalWasmDsl::class)
internal abstract class DefaultWasiAdapterToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) : DefaultWasiAdapterToolchainService {

    private val logger = Logging.getLogger(DefaultWasiAdapterToolchainServiceImpl::class.java)

    private val installer = WasiAdapterInstaller(logger)

    /**
     * Adapter installations already provisioned by this build, so that an adapter requested by several
     * consumers is only installed once.
     */
    private val adapterInstallations = ConcurrentHashMap<WasiAdapterDistributionKey, File>()

    override fun requestAdapter(adapterRequest: WasiAdapterRequest): Provider<WasiAdapter> {
        return providers.provider {
            val version = adapterRequest.version.orNull ?: error("A WASI adapter version must be requested")
            val flavor = adapterRequest.flavor.orNull ?: error("A WASI adapter flavor must be requested")
            provisionAdapter(WasiAdapterDistributionKey(version, flavor))
        }
    }

    private fun provisionAdapter(key: WasiAdapterDistributionKey): WasiAdapter {
        val adapterFile = adapterInstallations.computeIfAbsent(key) {
            val downloadBaseUrl = parameters.downloadBaseUrl.getOrElse(OFFICIAL_WASMTIME_DOWNLOAD_BASE_URL)
            installer.installAdapter(
                installationsDir = parameters.installationDir.get().asFile,
                version = it.version,
                flavor = it.flavor,
                downloadBaseUrl = downloadBaseUrl,
                offline = parameters.offline.getOrElse(false),
            )
        }

        return objects.newInstance<WasiAdapter>().apply {
            this.adapterFile.set(adapterFile)
            version.set(key.version)
            flavor.set(key.flavor)
        }
    }

    companion object {
        internal fun registerIfAbsent(
            gradle: Gradle,
            configureBuildServiceParameters: (DefaultWasiAdapterToolchainService.Parameters) -> Unit,
        ): Provider<DefaultWasiAdapterToolchainServiceImpl> {
            return gradle.sharedServices.registerIfAbsent(
                wasiAdapterServiceName,
                DefaultWasiAdapterToolchainServiceImpl::class.java
            ) { spec ->
                spec.parameters.installationDir.fileValue(
                    File(System.getProperty("user.home")).resolve(".kotlin").resolve(WASI_ADAPTER_TOOLCHAINS_DIR_NAME)
                )
                spec.parameters.downloadBaseUrl.convention(OFFICIAL_WASMTIME_DOWNLOAD_BASE_URL)
                spec.parameters.offline.convention(gradle.startParameter.isOffline)

                configureBuildServiceParameters(spec.parameters)
            }
        }

        internal fun registerIfAbsent(project: Project): Provider<DefaultWasiAdapterToolchainServiceImpl> {
            return registerIfAbsent(project.gradle) { parameters ->
                parameters.installationDir.fileProvider(project.wasiAdapterToolchainInstallationDir)
                parameters.downloadBaseUrl.set(
                    project.kotlinPropertiesProvider.wasiAdapterToolchainDefaultDownloadUrl.orElse(OFFICIAL_WASMTIME_DOWNLOAD_BASE_URL)
                )
            }
        }

        internal const val WASI_ADAPTER_TOOLCHAINS_DIR_NAME = "toolchains/wasi-adapter"

        internal const val OFFICIAL_WASMTIME_DOWNLOAD_BASE_URL = "https://github.com/bytecodealliance/wasmtime/releases/download"

        internal val Project.wasiAdapterToolchainInstallationDir: Provider<File>
            get() {
                if (kotlinPropertiesProvider.wasiAdapterToolchainInstallationDir.isPresent) {
                    return kotlinPropertiesProvider.wasiAdapterToolchainInstallationDir.map { File(it) }
                }

                val userKotlinPersistentDir = userKotlinPersistentDir

                return providers.provider { userKotlinPersistentDir.resolve(WASI_ADAPTER_TOOLCHAINS_DIR_NAME) }
            }
    }

    private data class WasiAdapterDistributionKey(
        val version: WasiAdapterVersion,
        val flavor: WasiAdapterFlavor,
    )
}
