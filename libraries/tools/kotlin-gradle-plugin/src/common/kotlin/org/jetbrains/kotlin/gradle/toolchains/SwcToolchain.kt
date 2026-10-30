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
import org.jetbrains.kotlin.gradle.dsl.toolchain.swc.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames
import org.jetbrains.kotlin.gradle.targets.toolchain.*
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesSwcToolchainService
import java.io.File
import javax.inject.Inject

/**
 * The default SWC version provisioned by the SWC toolchain.
 *
 * Kept in sync with the default of the SWC environment spec.
 */
internal const val DEFAULT_SWC_VERSION: String = "1.16.2"

private const val SWC_COMMAND = "swc"

private const val SWC_DOWNLOAD_BASE_URL = "https://github.com/swc-project/swc/releases/download"

/**
 * The requested SWC distribution, and how the SWC project publishes it.
 *
 * SWC publishes the CLI as a single executable file rather than an archive,
 * see https://github.com/swc-project/swc/releases.
 */
internal data class KotlinSwcToolchain(
    override val version: ToolchainVersion,
    override val platform: ToolchainPlatform,
) : ExternalToolchainForKotlin {

    override val toolName: String get() = "SWC"

    override val installationName: String get() = "swc-${version.version}-$classifier"

    override val executablePath: String get() = platform.executableName(SWC_COMMAND)

    val archivePath: String
        get() {
            val extension = if (platform.isWindows) ".exe" else ""
            return "v${version.version}/swc-$classifier$extension"
        }

    /**
     * The platform part of the published file name, for example `linux-x64-gnu` or `darwin-arm64`.
     */
    private val classifier: String
        get() {
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

/**
 * All settings of the download-based SWC toolchain service the user can configure.
 */
internal data class SwcToolchainSettings(
    override val installationDir: File,
    override val downloadBaseUrl: String,
    override val offline: Boolean,
) : DownloadedToolchainSettings

private val swcDownloadedService = DownloadedToolchainServiceVariant(
    serviceType = DefaultSwcToolchainService::class.java,
    implementation = DefaultSwcToolchainServiceImpl::class.java,
    settings = downloadedToolchainSettings(
        defaultDownloadBaseUrl = SWC_DOWNLOAD_BASE_URL,
        installationDirName = "toolchains/swc",
        create = ::SwcToolchainSettings,
    ),
    toolchainFor = { request, _ ->
        KotlinSwcToolchain(request.requiredVersion("SWC"), request.requiredPlatform("SWC"))
    },
    provisionerFor = { settings ->
        DefaultExternalProvisioner(
            archiveDownloader = DefaultExternalToolchainDownloader { settings.downloadUrl(it.archivePath) },
            extractor = DefaultExternalToolchainExtractor(archiveExtractor = RawExecutableToolchainExtractor),
            installationsDir = settings.installationDir,
            offline = settings.offline,
        )
    },
)

private val swcPreInstalledService = PreInstalledToolchainServiceVariant(
    serviceType = PreInstalledSwcToolchainService::class.java,
    implementation = PreInstalledSwcToolchainServiceImpl::class.java,
    defaultCommand = SWC_COMMAND,
    defaultVersion = DEFAULT_SWC_VERSION,
)

internal val swcToolchain: ExternalToolchainRegistration<SwcToolchainService<*>> = registerExternalToolchain(
    name = "SWC",
    defaultVersion = DEFAULT_SWC_VERSION,
    properties = ToolchainGradleProperties(
        mode = PropertyNames.KOTLIN_JS_SWC_TOOLCHAIN_MODE,
        installationDir = PropertyNames.KOTLIN_JS_SWC_TOOLCHAIN_INSTALLATION_DIR,
        downloadUrl = PropertyNames.KOTLIN_JS_SWC_TOOLCHAIN_DOWNLOAD_URL,
        preInstalledExecutablePath = PropertyNames.KOTLIN_JS_SWC_TOOLCHAIN_PREINSTALLED_EXECUTABLE_PATH,
    ),
    tasks = ToolchainTaskBinding(UsesSwcToolchainService::class.java) { it.swcToolchainService },
    downloaded = swcDownloadedService,
    preInstalled = swcPreInstalledService,
    disabled = disabledToolchainService<DisabledSwcToolchainService, DisabledSwcToolchainServiceImpl>(),
)

internal abstract class DefaultSwcToolchainServiceImpl @Inject internal constructor(
    objects: ObjectFactory,
    providers: ProviderFactory,
    fs: FileSystemOperations,
    archiveOperations: ArchiveOperations,
) : DefaultSwcToolchainService {

    private val provisioning = swcDownloadedService.provisioning(objects, providers, fs, archiveOperations) { parameters }

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        provisioning.request(request)
}

internal abstract class PreInstalledSwcToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) : PreInstalledSwcToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        swcPreInstalledService.request(objects, providers, request, parameters)
}

internal abstract class DisabledSwcToolchainServiceImpl : DisabledSwcToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        throw UnsupportedOperationException("SWC toolchain is disabled")
}

/**
 * The `toolchainManagement { swc { ... } }` DSL.
 *
 * @suppress
 */
@InternalKotlinGradlePluginApi
class SwcToolchainManagementDslImpl(gradle: Gradle) :
    SwcToolchainManagementDsl,
    ToolchainManagementDsl by ToolchainManagementDslImpl(gradle, swcToolchain)
