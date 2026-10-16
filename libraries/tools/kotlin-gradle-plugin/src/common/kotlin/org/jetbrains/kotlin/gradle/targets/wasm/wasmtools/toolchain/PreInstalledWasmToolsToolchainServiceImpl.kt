/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.PreInstalledWasmToolsToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsBuildPlatform
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsExecutable
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsRequest
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsVersion
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * A [WasmToolsToolchainService] that uses a pre-installed wasm-tools instead of downloading one.
 */
@OptIn(ExperimentalWasmDsl::class)
internal abstract class PreInstalledWasmToolsToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    private val execOperations: ExecOperations,
) : PreInstalledWasmToolsToolchainService {

    private val logger = Logging.getLogger(PreInstalledWasmToolsToolchainService::class.java)

    private val detectedWasmToolsCache = ConcurrentHashMap<String, Pair<WasmToolsVersion, WasmToolsBuildPlatform>>()

    override fun request(wasmToolsRequest: WasmToolsRequest): Provider<WasmToolsExecutable> {

        return parameters.wasmToolsExecutable.map { command ->
            val (installedVersion, installedPlatform) = detectedWasmToolsCache[command]
                ?: detectInstalledWasmTools(command).also { detectedWasmToolsCache.putIfAbsent(command, it) }

            val requestedVersion = wasmToolsRequest.version.orNull
            if (requestedVersion != null && requestedVersion.normalized != installedVersion.normalized) {
                logger.warn(
                    "wasm-tools $installedVersion found by '$command' does not match the requested " +
                            "version $requestedVersion. The requested version cannot be provisioned, because " +
                            "the wasm-tools toolchain is configured to use a pre-installed wasm-tools. " +
                            "Please update the pre-installed wasm-tools or configure the Kotlin Gradle Plugin to download wasm-tools by setting kotlin.wasm.wasmtools.toolchain=DOWNLOAD."
                )
            }
            wasmToolsRequest.platform.orNull?.let { platform ->
                if (platform != installedPlatform) {
                    logger.warn(
                        "wasm-tools found by '$command' runs on $installedPlatform, but $platform was requested."
                    )
                }
            }

            objects.newInstance<WasmToolsExecutable>().apply {
                executable.set(command)
                version.set(installedVersion)
                platform.set(installedPlatform)
            }
        }
    }

    /**
     * Asks wasm-tools itself about its version. `wasm-tools --version` prints `wasm-tools <version>`,
     * so the last whitespace-separated token of the output is the version.
     * The platform is detected from the host, because wasm-tools does not report it.
     */
    private fun detectInstalledWasmTools(command: String): Pair<WasmToolsVersion, WasmToolsBuildPlatform> {
        val output = ByteArrayOutputStream()
        try {
            execOperations.exec { spec ->
                spec.executable(command)
                spec.args(VERSION_ARGUMENT)
                spec.standardOutput = output
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Cannot run '$command' to detect the pre-installed wasm-tools. " +
                        "Make sure wasm-tools is installed and available on the PATH.",
                e
            )
        }

        val versionOutput = output.toString(Charsets.UTF_8.name()).trim()
        val version = versionOutput.split(Regex("\\s+")).lastOrNull()
        check(!version.isNullOrBlank()) { "Unexpected output of '$command $VERSION_ARGUMENT': $versionOutput" }

        return WasmToolsVersion(version) to providers.detectWasmToolsPlatform().get()
    }

    companion object {
        private const val VERSION_ARGUMENT = "--version"

        private const val DEFAULT_WASM_TOOLS_COMMAND = "wasm-tools"

        internal fun registerIfAbsent(
            gradle: Gradle,
            configureBuildServiceParameters: (PreInstalledWasmToolsToolchainService.Parameters) -> Unit,
        ): Provider<PreInstalledWasmToolsToolchainServiceImpl> {
            return gradle.sharedServices.registerIfAbsent(
                wasmToolsServiceName,
                PreInstalledWasmToolsToolchainServiceImpl::class.java,
            ) { spec ->
                spec.parameters.wasmToolsExecutable.convention(DEFAULT_WASM_TOOLS_COMMAND)
                configureBuildServiceParameters(spec.parameters)
            }
        }

        internal fun registerIfAbsent(project: Project): Provider<PreInstalledWasmToolsToolchainServiceImpl> {
            return registerIfAbsent(
                project.gradle,
            ) { parameters ->
                parameters.wasmToolsExecutable.set(
                    project.kotlinPropertiesProvider.wasmToolsToolchainLocalPath.orElse(DEFAULT_WASM_TOOLS_COMMAND)
                )
            }
        }
    }
}
