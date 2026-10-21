/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.wasm.d8.toolchain

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
import org.jetbrains.kotlin.gradle.dsl.toolchain.d8.D8ToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.d8.D8ToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.d8.DefaultD8ToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.d8.DisabledD8ToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.d8.PreInstalledD8ToolchainService
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.toolchain.ToolchainManagementDslImpl
import org.jetbrains.kotlin.gradle.targets.toolchain.KotlinToolchainProperties
import org.jetbrains.kotlin.gradle.targets.toolchain.KotlinToolchainServiceRegistrar
import org.jetbrains.kotlin.gradle.targets.toolchain.ToolchainArch
import org.jetbrains.kotlin.gradle.targets.toolchain.ToolchainArchiveType
import org.jetbrains.kotlin.gradle.targets.toolchain.ToolchainDistributionLayout
import org.jetbrains.kotlin.gradle.targets.toolchain.ToolchainOs
import org.jetbrains.kotlin.gradle.targets.toolchain.DownloadedToolchainProvisioner
import org.jetbrains.kotlin.gradle.targets.toolchain.executableName
import org.jetbrains.kotlin.gradle.targets.toolchain.toolchainExecutable
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesD8ToolchainService
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
 * Describes how the V8 project publishes the D8 distributions.
 *
 * See https://storage.googleapis.com/chromium-v8/official/canary/v8-linux64-rel-latest.json for the latest version.
 */
internal class D8DistributionLayout(private val edition: String) : ToolchainDistributionLayout {

    override val toolName: String get() = "D8"

    override val defaultDownloadBaseUrl: String get() = OFFICIAL_D8_DOWNLOAD_BASE_URL

    override fun archiveType(platform: ToolchainPlatform): ToolchainArchiveType = ToolchainArchiveType.ZIP

    override fun distributionName(version: ToolchainVersion, platform: ToolchainPlatform): String =
        "v8-${classifier(platform)}-$edition-${version.version}"

    override fun archivePath(version: ToolchainVersion, platform: ToolchainPlatform): String =
        "${distributionName(version, platform)}.zip"

    override fun executablePath(version: ToolchainVersion, platform: ToolchainPlatform): String =
        platform.executableName(D8_COMMAND)

    override fun preInstalledCommand(platform: ToolchainPlatform): String = D8_COMMAND

    /**
     * The platform part of the distribution name, for example `linux64` or `mac-arm64`.
     */
    private fun classifier(platform: ToolchainPlatform): String {
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

    private companion object {
        private const val OFFICIAL_D8_DOWNLOAD_BASE_URL = "https://storage.googleapis.com/chromium-v8/official/canary"
    }
}

@OptIn(ExperimentalKotlinToolchainDsl::class)
internal abstract class DefaultD8ToolchainServiceImpl @Inject internal constructor(
    objects: ObjectFactory,
    private val providers: ProviderFactory,
    fs: FileSystemOperations,
    archiveOperations: ArchiveOperations,
) : DefaultD8ToolchainService {

    private val provisioner = DownloadedToolchainProvisioner(
        objects,
        fs,
        archiveOperations,
        Logging.getLogger(DefaultD8ToolchainServiceImpl::class.java),
    ) { D8DistributionLayout((it as DefaultD8ToolchainService.Parameters).edition.getOrElse(DEFAULT_D8_EDITION)) }

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        providers.provider { provisioner.provision(request, parameters) }
}

@OptIn(ExperimentalKotlinToolchainDsl::class)
internal abstract class PreInstalledD8ToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
) : PreInstalledD8ToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        parameters.executable.map { command ->
            objects.toolchainExecutable(
                executable = command,
                version = request.version.getOrElse(ToolchainVersion(DEFAULT_D8_VERSION)),
                platform = request.platform.get(),
            )
        }
}

@OptIn(ExperimentalKotlinToolchainDsl::class)
internal abstract class DisabledD8ToolchainServiceImpl : DisabledD8ToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        throw UnsupportedOperationException("D8 toolchain is disabled")
}

internal val d8ToolchainRegistrar: KotlinToolchainServiceRegistrar<D8ToolchainService<*>> =
    KotlinToolchainServiceRegistrar(
        serviceInterface = D8ToolchainService::class.java,
        downloadedServiceInterface = DefaultD8ToolchainService::class.java,
        downloadedServiceImpl = DefaultD8ToolchainServiceImpl::class.java,
        preInstalledServiceInterface = PreInstalledD8ToolchainService::class.java,
        preInstalledServiceImpl = PreInstalledD8ToolchainServiceImpl::class.java,
        disabledServiceInterface = DisabledD8ToolchainService::class.java,
        disabledServiceImpl = DisabledD8ToolchainServiceImpl::class.java,
        defaultDownloadBaseUrl = D8DistributionLayout(DEFAULT_D8_EDITION).defaultDownloadBaseUrl,
        defaultVersion = DEFAULT_D8_VERSION,
        defaultPreInstalledCommand = D8_COMMAND,
        installationDirName = "toolchains/d8",
        properties = { properties ->
            KotlinToolchainProperties(
                mode = properties.d8ToolchainMode,
                installationDir = properties.d8ToolchainInstallationDir,
                downloadUrl = properties.d8ToolchainDownloadUrl,
                preInstalledExecutablePath = properties.d8ToolchainPreinstalledExecutablePath,
            )
        },
    )

internal val d8ToolchainServiceName: String get() = d8ToolchainRegistrar.serviceName

/**
 * Registers the [D8ToolchainService] selected for the build, and makes every [UsesD8ToolchainService] task
 * in [project] use it.
 */
internal fun registerD8ToolchainServiceIfAbsent(project: Project): Provider<out D8ToolchainService<*>> =
    d8ToolchainRegistrar.registerAndWireTasks(project, UsesD8ToolchainService::class.java) { task, service ->
        task.d8ToolchainService.value(service).disallowChanges()
    }

internal fun Project.requestDefaultD8(): Provider<ToolchainRequest> = d8ToolchainRegistrar.defaultRequest(this)

internal fun D8ToolchainService<*>.isD8ToolchainDisabled(): Boolean = d8ToolchainRegistrar.isDisabled(this)

internal fun Project.isD8ToolchainDisabled(): Boolean = d8ToolchainRegistrar.isDisabled(this)

/**
 * Returns the D8 executable provisioned by this service, or [legacyExecutable] when the toolchain is disabled.
 *
 * Provisions D8, so it must only be called at execution time.
 */
internal fun D8ToolchainService<*>.resolveD8Executable(
    legacyExecutable: Provider<String>,
    request: Provider<ToolchainRequest>,
): String = if (isD8ToolchainDisabled()) {
    legacyExecutable.get()
} else {
    request(request.get()).get().executable.orNull ?: error("D8 executable should be provisioned")
}

/**
 * The `toolchainManagement { d8 { ... } }` DSL.
 *
 * @suppress
 */
@InternalKotlinGradlePluginApi
class D8ToolchainManagementDslImpl(gradle: Gradle) : ToolchainManagementDslImpl<D8ToolchainService<*>>(
    gradle,
    "d8",
    d8ToolchainRegistrar,
    DefaultD8ToolchainService::class.java,
    DisabledD8ToolchainService::class.java,
), D8ToolchainManagementDsl

private val PropertiesProvider.d8ToolchainMode get() = property(PropertiesProvider.PropertyNames.KOTLIN_WASM_D8_TOOLCHAIN_MODE)
private val PropertiesProvider.d8ToolchainInstallationDir get() = property(PropertiesProvider.PropertyNames.KOTLIN_WASM_D8_TOOLCHAIN_INSTALLATION_DIR)
private val PropertiesProvider.d8ToolchainDownloadUrl get() = property(PropertiesProvider.PropertyNames.KOTLIN_WASM_D8_TOOLCHAIN_DOWNLOAD_URL)
private val PropertiesProvider.d8ToolchainPreinstalledExecutablePath
    get() = property(PropertiesProvider.PropertyNames.KOTLIN_WASM_D8_TOOLCHAIN_PREINSTALLED_EXECUTABLE_PATH)
