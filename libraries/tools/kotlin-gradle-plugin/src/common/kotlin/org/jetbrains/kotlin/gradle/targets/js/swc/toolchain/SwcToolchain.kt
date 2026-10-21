/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.js.swc.toolchain

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
import org.jetbrains.kotlin.gradle.dsl.toolchain.swc.DefaultSwcToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.swc.DisabledSwcToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.swc.PreInstalledSwcToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.swc.SwcToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.swc.SwcToolchainService
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
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesSwcToolchainService
import javax.inject.Inject

/**
 * The default SWC version provisioned by the SWC toolchain.
 *
 * Kept in sync with the default of the SWC environment spec.
 */
internal const val DEFAULT_SWC_VERSION: String = "1.16.2"

private const val SWC_COMMAND = "swc"

/**
 * Describes how the SWC project publishes its distributions.
 *
 * SWC publishes the CLI as a single executable file rather than an archive,
 * see https://github.com/swc-project/swc/releases.
 */
internal object SwcDistributionLayout : ToolchainDistributionLayout {

    override val toolName: String get() = "SWC"

    override val defaultDownloadBaseUrl: String get() = "https://github.com/swc-project/swc/releases/download"

    override fun archiveType(platform: ToolchainPlatform): ToolchainArchiveType =
        ToolchainArchiveType.RAW_EXECUTABLE

    override fun distributionName(version: ToolchainVersion, platform: ToolchainPlatform): String =
        "swc-${version.version}-${classifier(platform)}"

    override fun archivePath(version: ToolchainVersion, platform: ToolchainPlatform): String {
        val extension = if (platform.isWindows) ".exe" else ""
        return "v${version.version}/swc-${classifier(platform)}$extension"
    }

    override fun executablePath(version: ToolchainVersion, platform: ToolchainPlatform): String =
        platform.executableName(SWC_COMMAND)

    override fun preInstalledCommand(platform: ToolchainPlatform): String = SWC_COMMAND

    /**
     * The platform part of the published file name, for example `linux-x64-gnu` or `darwin-arm64`.
     */
    private fun classifier(platform: ToolchainPlatform): String {
        val os = when (platform.os) {
            ToolchainOs.WINDOWS -> "win32"
            ToolchainOs.MACOS -> "darwin"
            else -> "linux"
        }
        val arch = when (platform.arch) {
            ToolchainArch.AARCH64 -> "arm64"
            ToolchainArch.X86 -> "ia32"
            else -> "x64"
        }
        val cStdlib = when (platform.os) {
            ToolchainOs.LINUX -> "-gnu"
            ToolchainOs.WINDOWS -> "-msvc"
            else -> ""
        }
        return "$os-$arch$cStdlib"
    }
}

@OptIn(ExperimentalKotlinToolchainDsl::class)
internal abstract class DefaultSwcToolchainServiceImpl @Inject internal constructor(
    objects: ObjectFactory,
    private val providers: ProviderFactory,
    fs: FileSystemOperations,
    archiveOperations: ArchiveOperations,
) : DefaultSwcToolchainService {

    private val provisioner = DownloadedToolchainProvisioner(
        objects,
        fs,
        archiveOperations,
        Logging.getLogger(DefaultSwcToolchainServiceImpl::class.java),
    ) { SwcDistributionLayout }

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        providers.provider { provisioner.provision(request, parameters) }
}

@OptIn(ExperimentalKotlinToolchainDsl::class)
internal abstract class PreInstalledSwcToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
) : PreInstalledSwcToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        parameters.executable.map { command ->
            objects.toolchainExecutable(
                executable = command,
                version = request.version.getOrElse(ToolchainVersion(DEFAULT_SWC_VERSION)),
                platform = request.platform.get(),
            )
        }
}

@OptIn(ExperimentalKotlinToolchainDsl::class)
internal abstract class DisabledSwcToolchainServiceImpl : DisabledSwcToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        throw UnsupportedOperationException("SWC toolchain is disabled")
}

internal val swcToolchainRegistrar: KotlinToolchainServiceRegistrar<SwcToolchainService<*>> =
    KotlinToolchainServiceRegistrar(
        serviceInterface = SwcToolchainService::class.java,
        downloadedServiceInterface = DefaultSwcToolchainService::class.java,
        downloadedServiceImpl = DefaultSwcToolchainServiceImpl::class.java,
        preInstalledServiceInterface = PreInstalledSwcToolchainService::class.java,
        preInstalledServiceImpl = PreInstalledSwcToolchainServiceImpl::class.java,
        disabledServiceInterface = DisabledSwcToolchainService::class.java,
        disabledServiceImpl = DisabledSwcToolchainServiceImpl::class.java,
        defaultDownloadBaseUrl = SwcDistributionLayout.defaultDownloadBaseUrl,
        defaultVersion = DEFAULT_SWC_VERSION,
        defaultPreInstalledCommand = SWC_COMMAND,
        installationDirName = "toolchains/swc",
        properties = { properties ->
            KotlinToolchainProperties(
                mode = properties.swcToolchainMode,
                installationDir = properties.swcToolchainInstallationDir,
                downloadUrl = properties.swcToolchainDownloadUrl,
                preInstalledExecutablePath = properties.swcToolchainPreinstalledExecutablePath,
            )
        },
    )

internal val swcToolchainServiceName: String get() = swcToolchainRegistrar.serviceName

/**
 * Registers the [SwcToolchainService] selected for the build, and makes every [UsesSwcToolchainService] task
 * in [project] use it.
 */
internal fun registerSwcToolchainServiceIfAbsent(project: Project): Provider<out SwcToolchainService<*>> =
    swcToolchainRegistrar.registerAndWireTasks(project, UsesSwcToolchainService::class.java) { task, service ->
        task.swcToolchainService.value(service).disallowChanges()
    }

internal fun Project.requestDefaultSwc(): Provider<ToolchainRequest> = swcToolchainRegistrar.defaultRequest(this)

internal fun SwcToolchainService<*>.isSwcToolchainDisabled(): Boolean = swcToolchainRegistrar.isDisabled(this)

internal fun Project.isSwcToolchainDisabled(): Boolean = swcToolchainRegistrar.isDisabled(this)

/**
 * Returns the SWC executable provisioned by this service, or [legacyExecutable] when the toolchain is disabled.
 *
 * Provisions SWC, so it must only be called at execution time.
 */
internal fun SwcToolchainService<*>.resolveSwcExecutable(
    legacyExecutable: Provider<String>,
    request: Provider<ToolchainRequest>,
): String = if (isSwcToolchainDisabled()) {
    legacyExecutable.get()
} else {
    request(request.get()).get().executable.orNull ?: error("SWC executable should be provisioned")
}

/**
 * The `toolchainManagement { swc { ... } }` DSL.
 *
 * @suppress
 */
@InternalKotlinGradlePluginApi
class SwcToolchainManagementDslImpl(gradle: Gradle) : ToolchainManagementDslImpl<SwcToolchainService<*>>(
    gradle,
    "swc",
    swcToolchainRegistrar,
    DefaultSwcToolchainService::class.java,
    DisabledSwcToolchainService::class.java,
), SwcToolchainManagementDsl

private val PropertiesProvider.swcToolchainMode
    get() = property(PropertiesProvider.PropertyNames.KOTLIN_JS_SWC_TOOLCHAIN_MODE)
private val PropertiesProvider.swcToolchainInstallationDir
    get() = property(PropertiesProvider.PropertyNames.KOTLIN_JS_SWC_TOOLCHAIN_INSTALLATION_DIR)
private val PropertiesProvider.swcToolchainDownloadUrl
    get() = property(PropertiesProvider.PropertyNames.KOTLIN_JS_SWC_TOOLCHAIN_DOWNLOAD_URL)
private val PropertiesProvider.swcToolchainPreinstalledExecutablePath
    get() = property(PropertiesProvider.PropertyNames.KOTLIN_JS_SWC_TOOLCHAIN_PREINSTALLED_EXECUTABLE_PATH)
