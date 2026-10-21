/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.wasm.binaryen.toolchain

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
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.BinaryenToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.BinaryenToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.DefaultBinaryenToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.DisabledBinaryenToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.PreInstalledBinaryenToolchainService
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
import org.jetbrains.kotlin.gradle.targets.toolchain.toolchainExecutable
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesBinaryenToolchainService
import javax.inject.Inject

/**
 * The default Binaryen version provisioned by the Binaryen toolchain.
 *
 * Kept in sync with the default of the Binaryen environment spec.
 */
internal const val DEFAULT_BINARYEN_VERSION: String = "133"

private const val WASM_OPT_COMMAND = "wasm-opt"

/**
 * Describes how the Binaryen project publishes its distributions.
 *
 * See https://github.com/WebAssembly/binaryen/releases for the available archives.
 */
internal object BinaryenDistributionLayout : ToolchainDistributionLayout {

    override val toolName: String get() = "Binaryen"

    override val defaultDownloadBaseUrl: String
        get() = "https://github.com/WebAssembly/binaryen/releases/download"

    override fun archiveType(platform: ToolchainPlatform): ToolchainArchiveType = ToolchainArchiveType.TAR_GZ

    override fun distributionName(version: ToolchainVersion, platform: ToolchainPlatform): String =
        "binaryen-version_${version.version}-${classifier(platform)}"

    override fun archivePath(version: ToolchainVersion, platform: ToolchainPlatform): String =
        "version_${version.version}/${distributionName(version, platform)}.tar.gz"

    override fun archiveRootDirName(version: ToolchainVersion, platform: ToolchainPlatform): String =
        "binaryen-version_${version.version}"

    override fun executablePath(version: ToolchainVersion, platform: ToolchainPlatform): String =
        "bin/${platform.executableName(WASM_OPT_COMMAND)}"

    override fun preInstalledCommand(platform: ToolchainPlatform): String = WASM_OPT_COMMAND

    /**
     * The platform part of the archive name, for example `x86_64-linux` or `arm64-macos`.
     */
    private fun classifier(platform: ToolchainPlatform): String {
        val os = when (platform.os) {
            ToolchainOs.WINDOWS -> "windows"
            ToolchainOs.MACOS -> "macos"
            else -> "linux"
        }
        val arch = when {
            platform.arch != ToolchainArch.AARCH64 -> ToolchainArch.X86_64
            // Binaryen publishes the macOS arm archives as `arm64`, and the Linux ones as `aarch64`.
            platform.os == ToolchainOs.MACOS -> "arm64"
            else -> ToolchainArch.AARCH64
        }
        return "$arch-$os"
    }
}

@OptIn(ExperimentalKotlinToolchainDsl::class)
internal abstract class DefaultBinaryenToolchainServiceImpl @Inject internal constructor(
    objects: ObjectFactory,
    private val providers: ProviderFactory,
    fs: FileSystemOperations,
    archiveOperations: ArchiveOperations,
) : DefaultBinaryenToolchainService {

    private val provisioner = DownloadedToolchainProvisioner(
        objects,
        fs,
        archiveOperations,
        Logging.getLogger(DefaultBinaryenToolchainServiceImpl::class.java),
    ) { BinaryenDistributionLayout }

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        providers.provider { provisioner.provision(request, parameters) }
}

@OptIn(ExperimentalKotlinToolchainDsl::class)
internal abstract class PreInstalledBinaryenToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
) : PreInstalledBinaryenToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        parameters.executable.map { command ->
            objects.toolchainExecutable(
                executable = command,
                version = request.version.getOrElse(ToolchainVersion(DEFAULT_BINARYEN_VERSION)),
                platform = request.platform.get(),
            )
        }
}

@OptIn(ExperimentalKotlinToolchainDsl::class)
internal abstract class DisabledBinaryenToolchainServiceImpl : DisabledBinaryenToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        throw UnsupportedOperationException("Binaryen toolchain is disabled")
}

internal val binaryenToolchainRegistrar: KotlinToolchainServiceRegistrar<BinaryenToolchainService<*>> =
    KotlinToolchainServiceRegistrar(
        serviceInterface = BinaryenToolchainService::class.java,
        downloadedServiceInterface = DefaultBinaryenToolchainService::class.java,
        downloadedServiceImpl = DefaultBinaryenToolchainServiceImpl::class.java,
        preInstalledServiceInterface = PreInstalledBinaryenToolchainService::class.java,
        preInstalledServiceImpl = PreInstalledBinaryenToolchainServiceImpl::class.java,
        disabledServiceInterface = DisabledBinaryenToolchainService::class.java,
        disabledServiceImpl = DisabledBinaryenToolchainServiceImpl::class.java,
        defaultDownloadBaseUrl = BinaryenDistributionLayout.defaultDownloadBaseUrl,
        defaultVersion = DEFAULT_BINARYEN_VERSION,
        defaultPreInstalledCommand = WASM_OPT_COMMAND,
        installationDirName = "toolchains/binaryen",
        properties = { properties ->
            KotlinToolchainProperties(
                mode = properties.binaryenToolchainMode,
                installationDir = properties.binaryenToolchainInstallationDir,
                downloadUrl = properties.binaryenToolchainDownloadUrl,
                preInstalledExecutablePath = properties.binaryenToolchainPreinstalledExecutablePath,
            )
        },
    )

internal val binaryenToolchainServiceName: String get() = binaryenToolchainRegistrar.serviceName

/**
 * Registers the [BinaryenToolchainService] selected for the build, and makes every
 * [UsesBinaryenToolchainService] task in [project] use it.
 */
internal fun registerBinaryenToolchainServiceIfAbsent(project: Project): Provider<out BinaryenToolchainService<*>> =
    binaryenToolchainRegistrar.registerAndWireTasks(project, UsesBinaryenToolchainService::class.java) { task, service ->
        task.binaryenToolchainService.value(service).disallowChanges()
    }

internal fun Project.requestDefaultBinaryen(): Provider<ToolchainRequest> =
    binaryenToolchainRegistrar.defaultRequest(this)

internal fun BinaryenToolchainService<*>.isBinaryenToolchainDisabled(): Boolean =
    binaryenToolchainRegistrar.isDisabled(this)

internal fun Project.isBinaryenToolchainDisabled(): Boolean = binaryenToolchainRegistrar.isDisabled(this)

/**
 * Returns the `wasm-opt` executable provisioned by this service, or [legacyExecutable] when the toolchain
 * is disabled.
 *
 * Provisions Binaryen, so it must only be called at execution time.
 */
internal fun BinaryenToolchainService<*>.resolveBinaryenExecutable(
    legacyExecutable: Provider<String>,
    request: Provider<ToolchainRequest>,
): String = if (isBinaryenToolchainDisabled()) {
    legacyExecutable.get()
} else {
    request(request.get()).get().executable.orNull ?: error("Binaryen executable should be provisioned")
}

/**
 * The `toolchainManagement { binaryen { ... } }` DSL.
 *
 * @suppress
 */
@InternalKotlinGradlePluginApi
class BinaryenToolchainManagementDslImpl(gradle: Gradle) :
    ToolchainManagementDslImpl<BinaryenToolchainService<*>>(
        gradle,
        "binaryen",
        binaryenToolchainRegistrar,
        DefaultBinaryenToolchainService::class.java,
        DisabledBinaryenToolchainService::class.java,
    ),
    BinaryenToolchainManagementDsl

private val PropertiesProvider.binaryenToolchainMode
    get() = property(PropertiesProvider.PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_MODE)
private val PropertiesProvider.binaryenToolchainInstallationDir
    get() = property(PropertiesProvider.PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_INSTALLATION_DIR)
private val PropertiesProvider.binaryenToolchainDownloadUrl
    get() = property(PropertiesProvider.PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_DOWNLOAD_URL)
private val PropertiesProvider.binaryenToolchainPreinstalledExecutablePath
    get() = property(PropertiesProvider.PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_PREINSTALLED_EXECUTABLE_PATH)
