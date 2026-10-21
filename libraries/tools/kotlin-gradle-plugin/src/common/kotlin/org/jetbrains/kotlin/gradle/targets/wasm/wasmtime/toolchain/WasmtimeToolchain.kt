/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.toolchain

import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainExecutable
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainPlatform
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainRequest
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainVersion
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtime.DefaultWasmtimeToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtime.DisabledWasmtimeToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtime.PreInstalledWasmtimeToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtime.WasmtimeToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtime.WasmtimeToolchainService
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.toolchain.DownloadedToolchainProvisioner
import org.jetbrains.kotlin.gradle.targets.toolchain.ToolchainManagementDslImpl
import org.jetbrains.kotlin.gradle.targets.toolchain.KotlinToolchainProperties
import org.jetbrains.kotlin.gradle.targets.toolchain.KotlinToolchainServiceRegistrar
import org.jetbrains.kotlin.gradle.targets.toolchain.ToolchainArch
import org.jetbrains.kotlin.gradle.targets.toolchain.ToolchainArchiveType
import org.jetbrains.kotlin.gradle.targets.toolchain.ToolchainDistributionLayout
import org.jetbrains.kotlin.gradle.targets.toolchain.ToolchainOs
import org.jetbrains.kotlin.gradle.targets.toolchain.executableName
import org.jetbrains.kotlin.gradle.targets.toolchain.isWindows
import org.jetbrains.kotlin.gradle.targets.toolchain.toolchainExecutable
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesWasmtimeToolchainService
import javax.inject.Inject

/**
 * The default Wasmtime version provisioned by the Wasmtime toolchain.
 *
 * Kept in sync with the default of the Wasmtime environment spec.
 */
internal const val DEFAULT_WASMTIME_VERSION: String = "49.0.1"

private const val WASMTIME_COMMAND = "wasmtime"

/**
 * Describes how the Bytecode Alliance publishes the Wasmtime distributions.
 *
 * See https://github.com/bytecodealliance/wasmtime/releases for the available archives.
 */
internal object WasmtimeDistributionLayout : ToolchainDistributionLayout {

    override val toolName: String get() = "Wasmtime"

    override val defaultDownloadBaseUrl: String
        get() = "https://github.com/bytecodealliance/wasmtime/releases/download"

    override fun archiveType(platform: ToolchainPlatform): ToolchainArchiveType =
        if (platform.isWindows) ToolchainArchiveType.ZIP else ToolchainArchiveType.TAR_XZ

    override fun distributionName(version: ToolchainVersion, platform: ToolchainPlatform): String =
        "wasmtime-v${version.version}-${classifier(platform)}"

    override fun archivePath(version: ToolchainVersion, platform: ToolchainPlatform): String {
        val extension = if (platform.isWindows) "zip" else "tar.xz"
        return "v${version.version}/${distributionName(version, platform)}.$extension"
    }

    override fun archiveRootDirName(version: ToolchainVersion, platform: ToolchainPlatform): String =
        distributionName(version, platform)

    override fun executablePath(version: ToolchainVersion, platform: ToolchainPlatform): String =
        platform.executableName(WASMTIME_COMMAND)

    override fun preInstalledCommand(platform: ToolchainPlatform): String = WASMTIME_COMMAND

    /**
     * The platform part of the archive name, for example `x86_64-linux` or `aarch64-macos`.
     */
    private fun classifier(platform: ToolchainPlatform): String {
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

@OptIn(ExperimentalKotlinToolchainDsl::class)
internal abstract class DefaultWasmtimeToolchainServiceImpl @Inject internal constructor(
    objects: ObjectFactory,
    private val providers: ProviderFactory,
    fs: FileSystemOperations,
    archiveOperations: ArchiveOperations,
) : DefaultWasmtimeToolchainService {

    private val provisioner = DownloadedToolchainProvisioner(
        objects,
        fs,
        archiveOperations,
        Logging.getLogger(DefaultWasmtimeToolchainServiceImpl::class.java),
    ) { WasmtimeDistributionLayout }

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        providers.provider { provisioner.provision(request, parameters) }
}

@OptIn(ExperimentalKotlinToolchainDsl::class)
internal abstract class PreInstalledWasmtimeToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
) : PreInstalledWasmtimeToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        parameters.executable.map { command ->
            objects.toolchainExecutable(
                executable = command,
                version = request.version.getOrElse(ToolchainVersion(DEFAULT_WASMTIME_VERSION)),
                platform = request.platform.get(),
            )
        }
}

@OptIn(ExperimentalKotlinToolchainDsl::class)
internal abstract class DisabledWasmtimeToolchainServiceImpl : DisabledWasmtimeToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        throw UnsupportedOperationException("Wasmtime toolchain is disabled")
}

internal val wasmtimeToolchainRegistrar: KotlinToolchainServiceRegistrar<WasmtimeToolchainService<*>> =
    KotlinToolchainServiceRegistrar(
        serviceInterface = WasmtimeToolchainService::class.java,
        downloadedServiceInterface = DefaultWasmtimeToolchainService::class.java,
        downloadedServiceImpl = DefaultWasmtimeToolchainServiceImpl::class.java,
        preInstalledServiceInterface = PreInstalledWasmtimeToolchainService::class.java,
        preInstalledServiceImpl = PreInstalledWasmtimeToolchainServiceImpl::class.java,
        disabledServiceInterface = DisabledWasmtimeToolchainService::class.java,
        disabledServiceImpl = DisabledWasmtimeToolchainServiceImpl::class.java,
        defaultDownloadBaseUrl = WasmtimeDistributionLayout.defaultDownloadBaseUrl,
        defaultVersion = DEFAULT_WASMTIME_VERSION,
        defaultPreInstalledCommand = WASMTIME_COMMAND,
        installationDirName = "toolchains/wasmtime",
        properties = { properties ->
            KotlinToolchainProperties(
                mode = properties.wasmtimeToolchainMode,
                installationDir = properties.wasmtimeToolchainInstallationDir,
                downloadUrl = properties.wasmtimeToolchainDownloadUrl,
                preInstalledExecutablePath = properties.wasmtimeToolchainPreinstalledExecutablePath,
            )
        },
    )

internal val wasmtimeToolchainServiceName: String get() = wasmtimeToolchainRegistrar.serviceName

/**
 * Registers the [WasmtimeToolchainService] selected for the build, and makes every
 * [UsesWasmtimeToolchainService] task in [project] use it.
 */
internal fun registerWasmtimeToolchainServiceIfAbsent(project: Project): Provider<out WasmtimeToolchainService<*>> =
    wasmtimeToolchainRegistrar.registerAndWireTasks(project, UsesWasmtimeToolchainService::class.java) { task, service ->
        task.wasmtimeToolchainService.value(service).disallowChanges()
    }

internal fun Project.requestDefaultWasmtime(): Provider<ToolchainRequest> =
    wasmtimeToolchainRegistrar.defaultRequest(this)

internal fun WasmtimeToolchainService<*>.isWasmtimeToolchainDisabled(): Boolean =
    wasmtimeToolchainRegistrar.isDisabled(this)

internal fun Project.isWasmtimeToolchainDisabled(): Boolean = wasmtimeToolchainRegistrar.isDisabled(this)

/**
 * Returns the Wasmtime executable provisioned by this service, or [legacyExecutable] when the toolchain
 * is disabled.
 *
 * Provisions Wasmtime, so it must only be called at execution time.
 */
internal fun WasmtimeToolchainService<*>.resolveWasmtimeExecutable(
    legacyExecutable: Provider<String>,
    request: Provider<ToolchainRequest>,
): String = if (isWasmtimeToolchainDisabled()) {
    legacyExecutable.get()
} else {
    request(request.get()).get().executable.orNull ?: error("Wasmtime executable should be provisioned")
}

/**
 * The `toolchainManagement { wasmtime { ... } }` DSL.
 *
 * @suppress
 */
@InternalKotlinGradlePluginApi
class WasmtimeToolchainManagementDslImpl(gradle: Gradle) :
    ToolchainManagementDslImpl<WasmtimeToolchainService<*>>(
        gradle,
        "wasmtime",
        wasmtimeToolchainRegistrar,
        DefaultWasmtimeToolchainService::class.java,
        DisabledWasmtimeToolchainService::class.java,
    ),
    WasmtimeToolchainManagementDsl

private val PropertiesProvider.wasmtimeToolchainMode
    get() = property(PropertiesProvider.PropertyNames.KOTLIN_WASM_WASMTIME_TOOLCHAIN_MODE)
private val PropertiesProvider.wasmtimeToolchainInstallationDir
    get() = property(PropertiesProvider.PropertyNames.KOTLIN_WASM_WASMTIME_TOOLCHAIN_INSTALLATION_DIR)
private val PropertiesProvider.wasmtimeToolchainDownloadUrl
    get() = property(PropertiesProvider.PropertyNames.KOTLIN_WASM_WASMTIME_TOOLCHAIN_DOWNLOAD_URL)
private val PropertiesProvider.wasmtimeToolchainPreinstalledExecutablePath
    get() = property(PropertiesProvider.PropertyNames.KOTLIN_WASM_WASMTIME_TOOLCHAIN_PREINSTALLED_EXECUTABLE_PATH)
