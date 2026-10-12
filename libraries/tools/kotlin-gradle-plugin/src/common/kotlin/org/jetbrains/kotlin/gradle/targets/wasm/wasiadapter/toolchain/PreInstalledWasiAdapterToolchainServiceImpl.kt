/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasiadapter.toolchain

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.PreInstalledWasiAdapterToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapter
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapterRequest
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.File
import javax.inject.Inject

/**
 * A [PreInstalledWasiAdapterToolchainService] that uses an explicit, pre-installed WASI preview1 adapter module instead of downloading one.
 */
@OptIn(ExperimentalWasmDsl::class)
internal abstract class PreInstalledWasiAdapterToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) : PreInstalledWasiAdapterToolchainService {

    override fun requestAdapter(adapterRequest: WasiAdapterRequest): Provider<WasiAdapter> {
        return providers.provider {
            val adapterFile = if (parameters.adapterFile.isPresent) {
                parameters.adapterFile.get().asFile
            } else {
                error(
                    "WASI adapter toolchain is configured in PREINSTALLED mode, but no adapter file was provided. " +
                            "Configure the adapter file via the 'kotlin.wasm.wasiadapter.toolchain.local.path' property or the " +
                            "'toolchainManagement { wasiAdapter { toolchainService(PreInstalledWasiAdapterToolchainService::class) { adapterFile.set(...) } } }' settings block."
                )
            }

            if (!adapterFile.isFile) {
                error(
                    "Configured pre-installed WASI adapter file '$adapterFile' does not exist. " +
                        "Make sure the file exists or configure the Kotlin Gradle Plugin to download the adapter by setting 'kotlin.wasm.wasiadapter.toolchain=DOWNLOAD'."
                )
            }

            val version = adapterRequest.version.orNull ?: error("A WASI adapter version must be requested")
            val flavor = adapterRequest.flavor.orNull ?: error("A WASI adapter flavor must be requested")

            objects.newInstance<WasiAdapter>().apply {
                this.adapterFile.set(adapterFile)
                this.version.set(version)
                this.flavor.set(flavor)
            }
        }
    }

    companion object {
        internal fun registerIfAbsent(
            gradle: Gradle,
            configureBuildServiceParameters: (PreInstalledWasiAdapterToolchainService.Parameters) -> Unit,
        ): Provider<PreInstalledWasiAdapterToolchainServiceImpl> {
            return gradle.sharedServices.registerIfAbsent(
                wasiAdapterServiceName,
                PreInstalledWasiAdapterToolchainServiceImpl::class.java,
            ) { spec ->
                configureBuildServiceParameters(spec.parameters)
            }
        }

        internal fun registerIfAbsent(project: Project): Provider<PreInstalledWasiAdapterToolchainServiceImpl> {
            return registerIfAbsent(
                project.gradle,
            ) { parameters ->
                if (project.kotlinPropertiesProvider.wasiAdapterToolchainPreinstalledExecutablePath.isPresent) {
                    parameters.adapterFile.fileProvider(project.kotlinPropertiesProvider.wasiAdapterToolchainPreinstalledExecutablePath.map { File(it) })
                }
            }
        }
    }
}
