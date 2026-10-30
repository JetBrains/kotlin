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
import org.jetbrains.kotlin.gradle.dsl.toolchain.d8.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames
import org.jetbrains.kotlin.gradle.targets.toolchain.*
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesD8ToolchainService
import java.io.File
import javax.inject.Inject

/**
 * The default D8 version provisioned by the D8 toolchain.
 *
 * Kept in sync with the default of the D8 environment spec.
 */
internal const val DEFAULT_D8_VERSION: String = "15.0.45"

internal const val DEFAULT_D8_EDITION: String = "rel"

private const val D8_COMMAND = "d8"

/**
 * See https://storage.googleapis.com/chromium-v8/official/canary/v8-linux64-rel-latest.json for the latest version.
 */
private const val D8_DOWNLOAD_BASE_URL = "https://storage.googleapis.com/chromium-v8/official/canary"

private const val D8_INSTALLATION_DIR_NAME = "toolchains/d8"

/**
 * The requested D8 distribution, and how the V8 project publishes it.
 */
internal data class KotlinD8Toolchain(
    override val version: ToolchainVersion,
    override val platform: ToolchainPlatform,
    val edition: String,
) : ExternalToolchainForKotlin {

    override val toolName: String get() = "D8"

    override val installationName: String get() = "v8-$classifier-$edition-${version.version}"

    override val executablePath: String get() = platform.executableName(D8_COMMAND)

    val archivePath: String get() = "$installationName.zip"

    /**
     * The platform part of the distribution name, for example `linux64` or `mac-arm64`.
     */
    private val classifier: String
        get() {
            val os = when (platform.os) {
                ToolchainOs.WINDOWS -> "win"
                ToolchainOs.MACOS -> "mac"
                else -> "linux"
            }
            return when (platform.arch) {
                ToolchainArch.AARCH64 -> "$os-arm64"
                ToolchainArch.X86 -> "${os}32"
                else -> "${os}64"
            }
        }
}

/**
 * All settings of the download-based D8 toolchain service the user can configure.
 */
internal data class D8ToolchainSettings(
    override val installationDir: File,
    override val downloadBaseUrl: String,
    override val offline: Boolean,
    val edition: String,
) : DownloadedToolchainSettings

private val d8Settings = ToolchainSettingsMapping<DefaultD8ToolchainService.Parameters, D8ToolchainSettings>(
    defaults = { gradle ->
        D8ToolchainSettings(
            installationDir = defaultInstallationDir(D8_INSTALLATION_DIR_NAME),
            downloadBaseUrl = D8_DOWNLOAD_BASE_URL,
            offline = gradle.startParameter.isOffline,
            edition = DEFAULT_D8_EDITION,
        )
    },
    fromGradleProperties = { project, properties ->
        D8ToolchainSettings(
            installationDir = properties.installationDir(project, D8_INSTALLATION_DIR_NAME),
            downloadBaseUrl = properties.downloadBaseUrl(project, D8_DOWNLOAD_BASE_URL),
            offline = project.gradle.startParameter.isOffline,
            edition = DEFAULT_D8_EDITION,
        )
    },
    fromParameters = {
        D8ToolchainSettings(
            installationDir = it.installationDir.get().asFile,
            downloadBaseUrl = it.downloadBaseUrl.get(),
            offline = it.offline.get(),
            edition = it.edition.get(),
        )
    },
    applyConventions = { parameters, settings ->
        parameters.applyConventions(settings)
        parameters.edition.convention(settings.edition)
    },
)

private val d8DownloadedService = DownloadedToolchainServiceVariant(
    serviceType = DefaultD8ToolchainService::class.java,
    implementation = DefaultD8ToolchainServiceImpl::class.java,
    settings = d8Settings,
    toolchainFor = { request, settings ->
        KotlinD8Toolchain(request.requiredVersion("D8"), request.requiredPlatform("D8"), settings.edition)
    },
    provisionerFor = { settings ->
        DefaultExternalProvisioner(
            archiveDownloader = DefaultExternalToolchainDownloader { settings.downloadUrl(it.archivePath) },
            extractor = DefaultExternalToolchainExtractor(archiveExtractor = ZipToolchainExtractor),
            installationsDir = settings.installationDir,
            offline = settings.offline,
        )
    },
)

private val d8PreInstalledService = PreInstalledToolchainServiceVariant(
    serviceType = PreInstalledD8ToolchainService::class.java,
    implementation = PreInstalledD8ToolchainServiceImpl::class.java,
    defaultCommand = D8_COMMAND,
    defaultVersion = DEFAULT_D8_VERSION,
)

internal val d8Toolchain: ExternalToolchainRegistration<D8ToolchainService<*>> = registerExternalToolchain(
    name = "D8",
    defaultVersion = DEFAULT_D8_VERSION,
    properties = ToolchainGradleProperties(
        mode = PropertyNames.KOTLIN_WASM_D8_TOOLCHAIN_MODE,
        installationDir = PropertyNames.KOTLIN_WASM_D8_TOOLCHAIN_INSTALLATION_DIR,
        downloadUrl = PropertyNames.KOTLIN_WASM_D8_TOOLCHAIN_DOWNLOAD_URL,
        preInstalledExecutablePath = PropertyNames.KOTLIN_WASM_D8_TOOLCHAIN_PREINSTALLED_EXECUTABLE_PATH,
    ),
    tasks = ToolchainTaskBinding(UsesD8ToolchainService::class.java) { it.d8ToolchainService },
    downloaded = d8DownloadedService,
    preInstalled = d8PreInstalledService,
    disabled = disabledToolchainService<DisabledD8ToolchainService, DisabledD8ToolchainServiceImpl>(),
)

internal abstract class DefaultD8ToolchainServiceImpl @Inject internal constructor(
    objects: ObjectFactory,
    providers: ProviderFactory,
    fs: FileSystemOperations,
    archiveOperations: ArchiveOperations,
) : DefaultD8ToolchainService {

    private val provisioning = d8DownloadedService.provisioning(objects, providers, fs, archiveOperations) { parameters }

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        provisioning.request(request)
}

internal abstract class PreInstalledD8ToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) : PreInstalledD8ToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        d8PreInstalledService.request(objects, providers, request, parameters)
}

internal abstract class DisabledD8ToolchainServiceImpl : DisabledD8ToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        throw UnsupportedOperationException("D8 toolchain is disabled")
}

/**
 * The `toolchainManagement { d8 { ... } }` DSL.
 *
 * @suppress
 */
@InternalKotlinGradlePluginApi
class D8ToolchainManagementDslImpl(gradle: Gradle) :
    D8ToolchainManagementDsl,
    ToolchainManagementDsl by ToolchainManagementDslImpl(gradle, d8Toolchain)
