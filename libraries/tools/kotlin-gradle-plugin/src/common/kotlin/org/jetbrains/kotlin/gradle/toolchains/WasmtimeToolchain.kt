/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.toolchains

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.toolchain.*
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtime.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames
import org.jetbrains.kotlin.gradle.targets.toolchain.*
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesWasmtimeToolchainService
import java.io.File
import javax.inject.Inject

/**
 * The default Wasmtime version provisioned by the Wasmtime toolchain.
 *
 * Kept in sync with the default of the Wasmtime environment spec.
 */
internal const val DEFAULT_WASMTIME_VERSION: String = "49.0.1"

private const val WASMTIME_COMMAND = "wasmtime"

/**
 * See https://github.com/bytecodealliance/wasmtime/releases for the available archives.
 */
private const val WASMTIME_DOWNLOAD_BASE_URL = "https://github.com/bytecodealliance/wasmtime/releases/download"

/**
 * The requested Wasmtime distribution, and how the Bytecode Alliance publishes it.
 */
internal data class KotlinWasmtimeToolchain(
    override val version: ToolchainVersion,
    override val platform: ToolchainPlatform,
) : ExternalToolchainForKotlin {

    override val toolName: String get() = "Wasmtime"

    override val installationName: String get() = "wasmtime-v${version.version}-$classifier"

    override val executablePath: String get() = platform.executableName(WASMTIME_COMMAND)

    val archivePath: String
        get() {
            val extension = if (platform.isWindows) "zip" else "tar.xz"
            return "v${version.version}/$installationName.$extension"
        }

    /**
     * The platform part of the archive name, for example `x86_64-linux` or `aarch64-macos`.
     */
    private val classifier: String
        get() {
            val os = when (platform.os) {
                ToolchainOs.WINDOWS -> "windows"
                ToolchainOs.MACOS -> "macos"
                else -> "linux"
            }
            val arch = when (platform.arch) {
                ToolchainArch.AARCH64 -> ToolchainArch.AARCH64
                ToolchainArch.X86_64 -> ToolchainArch.X86_64
                else -> throw IllegalArgumentException("Unsupported architecture for Wasmtime: ${platform.arch}")
            }
            return "$arch-$os"
        }
}

/**
 * All settings of the download-based Wasmtime toolchain service the user can configure.
 */
internal data class WasmtimeToolchainSettings(
    override val installationDir: File,
    override val downloadBaseUrl: String,
    override val offline: Boolean,
) : DownloadedToolchainSettings

private val wasmtimeDownloadedService = DownloadedToolchainServiceVariant(
    serviceType = DefaultWasmtimeToolchainService::class.java,
    implementation = DefaultWasmtimeToolchainServiceImpl::class.java,
    settings = downloadedToolchainSettings(
        defaultDownloadBaseUrl = WASMTIME_DOWNLOAD_BASE_URL,
        installationDirName = "toolchains/wasmtime",
        create = ::WasmtimeToolchainSettings,
    ),
    toolchainFor = { request, _ ->
        KotlinWasmtimeToolchain(request.requiredVersion("Wasmtime"), request.requiredPlatform("Wasmtime"))
    },
    provisionerFor = { settings ->
        DefaultExternalProvisioner(
            archiveDownloader = DefaultExternalToolchainDownloader { settings.downloadUrl(it.archivePath) },
            extractor = DefaultExternalToolchainExtractor(
                archiveExtractor = CombinedToolchainExtractor { context ->
                    if (context.toolchain.platform.isWindows) ZipToolchainExtractor else TarXzToolchainExtractor
                },
                archiveRootDir = { it.installationName },
            ),
            installationsDir = settings.installationDir,
            offline = settings.offline,
        )
    },
)

private val wasmtimePreInstalledService = PreInstalledToolchainServiceVariant(
    serviceType = PreInstalledWasmtimeToolchainService::class.java,
    implementation = PreInstalledWasmtimeToolchainServiceImpl::class.java,
    defaultCommand = WASMTIME_COMMAND,
    defaultVersion = DEFAULT_WASMTIME_VERSION,
)

internal val wasmtimeToolchain: ExternalToolchainRegistration<WasmtimeToolchainService<*>> = registerExternalToolchain(
    name = "Wasmtime",
    defaultVersion = DEFAULT_WASMTIME_VERSION,
    properties = ToolchainGradleProperties(
        mode = PropertyNames.KOTLIN_WASM_WASMTIME_TOOLCHAIN_MODE,
        installationDir = PropertyNames.KOTLIN_WASM_WASMTIME_TOOLCHAIN_INSTALLATION_DIR,
        downloadUrl = PropertyNames.KOTLIN_WASM_WASMTIME_TOOLCHAIN_DOWNLOAD_URL,
        preInstalledExecutablePath = PropertyNames.KOTLIN_WASM_WASMTIME_TOOLCHAIN_PREINSTALLED_EXECUTABLE_PATH,
    ),
    tasks = ToolchainTaskBinding(UsesWasmtimeToolchainService::class.java) { it.wasmtimeToolchainService },
    downloaded = wasmtimeDownloadedService,
    preInstalled = wasmtimePreInstalledService,
    disabled = disabledToolchainService<DisabledWasmtimeToolchainService, DisabledWasmtimeToolchainServiceImpl>(),
)

internal abstract class DefaultWasmtimeToolchainServiceImpl @Inject internal constructor(
    objects: ObjectFactory,
    providers: ProviderFactory,
    fs: FileSystemOperations,
    archiveOperations: ArchiveOperations,
) : DefaultWasmtimeToolchainService {

    private val provisioning = wasmtimeDownloadedService.provisioning(objects, providers, fs, archiveOperations) { parameters }

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        provisioning.request(request)
}

internal abstract class PreInstalledWasmtimeToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) : PreInstalledWasmtimeToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        wasmtimePreInstalledService.request(objects, providers, request, parameters)
}

internal abstract class DisabledWasmtimeToolchainServiceImpl : DisabledWasmtimeToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        throw UnsupportedOperationException("Wasmtime toolchain is disabled")
}

/**
 * The `toolchainManagement { wasmtime { ... } }` DSL.
 *
 * @suppress
 */
@InternalKotlinGradlePluginApi
class WasmtimeToolchainManagementDslImpl(gradle: Gradle) :
    WasmtimeToolchainManagementDsl,
    ToolchainManagementDsl by ToolchainManagementDslImpl(gradle, wasmtimeToolchain)
