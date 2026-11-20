/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExternalToolchainDsl::class)

package org.jetbrains.kotlin.gradle.toolchains.binaryen

import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.*
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.*
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesBinaryenToolchainService
import org.jetbrains.kotlin.gradle.toolchains.*
import org.jetbrains.kotlin.gradle.utils.copySubDirectoryContent
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.File
import javax.inject.Inject

internal const val DEFAULT_BINARYEN_VERSION: String = "133"

private const val WASM_OPT_COMMAND = "wasm-opt"

/**
 * See https://github.com/WebAssembly/binaryen/releases for the available archives.
 */
private const val BINARYEN_DOWNLOAD_BASE_URL = "https://github.com/WebAssembly/binaryen/releases/download"

private const val BINARYEN_INSTALLATION_DIR_NAME = "toolchains/binaryen"

private val binaryenToolchainGradleProperties = ExternalToolchainGradleProperties(
    mode = PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_MODE,
    installationDir = PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_INSTALLATION_DIR,
    downloadUrl = PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_DOWNLOAD_URL,
)

internal val BinaryenToolchainExtension = KotlinProjectSetupAction {
    val service = locateOrRegisterBinaryenToolchainService()
    tasks.withType(UsesBinaryenToolchainService::class.java).configureEach { task ->
        task.binaryenToolchainService.value(service).disallowChanges()
        task.usesService(service)
    }
}

internal fun Project.locateOrRegisterBinaryenToolchainService(): Provider<out BinaryenToolchainService<*>> =
    gradle.findExternalToolchainService(BinaryenToolchainService::class.java)
        ?: registerBinaryenToolchainServiceFromGradleProperties()

private fun Project.registerBinaryenToolchainServiceFromGradleProperties(): Provider<out BinaryenToolchainService<*>> =
    when (binaryenToolchainGradleProperties.mode(this)) {
        ExternalToolchainMode.DOWNLOAD ->
            gradle.registerDefaultBinaryenToolchainService(
                BinaryenToolchainSettings.fromGradleProperties(this)
            )
        ExternalToolchainMode.PREINSTALLED ->
            gradle.registerPreInstalledBinaryenToolchainService { parameters ->
                val propertyName = PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_PREINSTALLED_DIR
                val directory = kotlinPropertiesProvider.property(propertyName).orNull
                require(!directory.isNullOrBlank()) {
                    "The '$propertyName' Gradle property must specify the pre-installed Binaryen distribution directory"
                }
                parameters.toolchainDir.fileValue(file(directory))
            }
        ExternalToolchainMode.DISABLE ->
            gradle.registerDisabledBinaryenToolchainService()
    }

/**
 * Registers the Binaryen toolchain service selected with [serviceClass] in `settings.gradle.kts`.
 *
 * [serviceClass] is either one of the built-in Binaryen services, which is mapped to its implementation here,
 * or a custom implementation class provided by the build author.
 */
@Suppress("UNCHECKED_CAST")
internal fun Gradle.registerBinaryenToolchainService(
    serviceClass: Class<out BinaryenToolchainService<*>>,
    configure: (BuildServiceParameters) -> Unit,
): Provider<out BinaryenToolchainService<*>> {
    findExternalToolchainService(BinaryenToolchainService::class.java)?.let { return it }

    return when (serviceClass) {
        DefaultBinaryenToolchainService::class.java ->
            registerDefaultBinaryenToolchainService(BinaryenToolchainSettings.defaults(this), configure)
        PreInstalledBinaryenToolchainService::class.java ->
            registerPreInstalledBinaryenToolchainService(configure)
        DisabledBinaryenToolchainService::class.java ->
            registerDisabledBinaryenToolchainService()
        else ->
            sharedServices.registerIfAbsent(
                externalToolchainServiceName(BinaryenToolchainService::class.java),
                serviceClass as Class<BinaryenToolchainService<BuildServiceParameters>>,
            ) { spec ->
                spec.parameters?.let(configure)
            }
    }
}

/**
 * All settings of the download-based Binaryen toolchain service the user can configure.
 */
internal data class BinaryenToolchainSettings(
    val installationDir: File,
    val downloadBaseUrl: String,
    val offline: Boolean,
) {

    companion object {
        /**
         * The settings of the service selected in `settings.gradle.kts`, before the build author configures them.
         */
        fun defaults(gradle: Gradle): BinaryenToolchainSettings = BinaryenToolchainSettings(
            installationDir = defaultExternalToolchainInstallationDir(BINARYEN_INSTALLATION_DIR_NAME),
            downloadBaseUrl = BINARYEN_DOWNLOAD_BASE_URL,
            offline = gradle.startParameter.isOffline,
        )

        /**
         * The settings of the service requested by [project], configured by its Gradle properties.
         */
        fun fromGradleProperties(project: Project): BinaryenToolchainSettings = BinaryenToolchainSettings(
            installationDir = binaryenToolchainGradleProperties.installationDir(project, BINARYEN_INSTALLATION_DIR_NAME),
            downloadBaseUrl = binaryenToolchainGradleProperties.downloadBaseUrl(project, BINARYEN_DOWNLOAD_BASE_URL),
            offline = project.gradle.startParameter.isOffline,
        )

        fun fromParameters(parameters: DefaultBinaryenToolchainService.Parameters): BinaryenToolchainSettings = BinaryenToolchainSettings(
            installationDir = parameters.installationDir.get().asFile,
            downloadBaseUrl = parameters.downloadBaseUrl.get(),
            offline = parameters.offline.get(),
        )
    }
}

/**
 * Registers [DefaultBinaryenToolchainService] for the build, configured by [settings], and then by [configure].
 */
internal fun Gradle.registerDefaultBinaryenToolchainService(
    settings: BinaryenToolchainSettings,
    configure: (DefaultBinaryenToolchainService.Parameters) -> Unit = {},
): Provider<out BinaryenToolchainService<*>> =
    registerExternalToolchainService<BinaryenToolchainService<*>, DefaultBinaryenToolchainService.Parameters>(
        externalToolchainServiceName(BinaryenToolchainService::class.java),
        DefaultBinaryenToolchainServiceImpl::class.java,
    ) { parameters ->
        parameters.installationDir.fileValue(settings.installationDir)
        parameters.downloadBaseUrl.convention(settings.downloadBaseUrl)
        parameters.offline.convention(settings.offline)
        configure(parameters)
    }

/**
 * Assembles the provisioner of [DefaultBinaryenToolchainService] from its [settings].
 *
 * It is called by the service itself, because a build service is instantiated by Gradle,
 * and only receives its parameters, which the build author may change in `settings.gradle.kts`.
 */
private fun binaryenToolchainProvisioner(
    settings: BinaryenToolchainSettings,
    fs: FileSystemOperations,
    archiveOperations: ArchiveOperations,
): ExternalToolchainProvisioner<BinaryenToolchainRequest> {
    val installationName: (BinaryenToolchainRequest) -> String = { request ->
        val version = request.requiredVersion()
        val platform = request.requiredPlatform()
        val os = when (platform.os) {
            ExternalToolchainOs.WINDOWS -> "windows"
            ExternalToolchainOs.MACOS -> "macos"
            else -> "linux"
        }
        val arch = when {
            platform.arch != ExternalToolchainArch.AARCH64 -> ExternalToolchainArch.X86_64
            platform.os == ExternalToolchainOs.MACOS -> "arm64"
            else -> ExternalToolchainArch.AARCH64
        }
        "binaryen-version_$version-$arch-$os"
    }
    return ExternalToolchainProvisioner(
        toolchainName = "Binaryen",
        installationsDir = settings.installationDir,
        installationName = installationName,
        requiredFiles = { listOf(binaryenToolchainExecutablePath(it.requiredPlatform())) },
        archiveDownloader = ExternalToolchainArchiveDownloader(
            offline = settings.offline,
            urlForRequest = { request ->
                val version = request.requiredVersion()
                "${settings.downloadBaseUrl.trimEnd('/')}/version_$version/${installationName(request)}.tar.gz"
            },
        ),
        unpackArchive = { request, archive, destination ->
            fs.copySubDirectoryContent(
                archiveOperations.tarTree(archive),
                "binaryen-version_${request.requiredVersion()}",
                destination,
            )
            if (!request.requiredPlatform().isWindows) {
                destination.resolve(binaryenToolchainExecutablePath(request.requiredPlatform())).setExecutable(true)
            }
        },
    )
}

private fun binaryenToolchainExecutablePath(platform: ExternalToolchainPlatform): String =
    "bin/${platform.executableName(WASM_OPT_COMMAND)}"

internal abstract class DefaultBinaryenToolchainServiceImpl @Inject internal constructor(
    private val providers: ProviderFactory,
    private val fs: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) : DefaultBinaryenToolchainService {
    private val provisioner by lazy {
        binaryenToolchainProvisioner(BinaryenToolchainSettings.fromParameters(parameters), fs, archiveOperations)
    }

    override fun request(request: BinaryenToolchainRequest): Provider<ProvisionedBinaryenToolchain> = providers.provider {
        val installation = provisioner.provision(request)
        ProvisionedBinaryenToolchain(installation.resolve(binaryenToolchainExecutablePath(request.requiredPlatform())).toPath())
    }
}

/**
 * Registers [PreInstalledBinaryenToolchainService] for the build, configured by [configure].
 */
internal fun Gradle.registerPreInstalledBinaryenToolchainService(
    configure: (PreInstalledBinaryenToolchainService.Parameters) -> Unit = {},
): Provider<out BinaryenToolchainService<*>> =
    registerExternalToolchainService<BinaryenToolchainService<*>, PreInstalledBinaryenToolchainService.Parameters>(
        externalToolchainServiceName(BinaryenToolchainService::class.java),
        PreInstalledBinaryenToolchainServiceImpl::class.java,
    ) { parameters ->
        configure(parameters)
    }

/**
 * Resolves Binaryen tools from the configured distribution directory on the machine running the build.
 */
internal abstract class PreInstalledBinaryenToolchainServiceImpl @Inject internal constructor(
    private val providers: ProviderFactory,
) : PreInstalledBinaryenToolchainService {

    override fun request(request: BinaryenToolchainRequest): Provider<ProvisionedBinaryenToolchain> =
        providers.detectExternalToolchainPlatform().map { platform ->
            val directory = parameters.toolchainDir.get().asFile
            ProvisionedBinaryenToolchain(directory.resolve(binaryenToolchainExecutablePath(platform)).toPath())
        }
}

internal fun Gradle.registerDisabledBinaryenToolchainService(): Provider<out BinaryenToolchainService<*>> =
    sharedServices.registerIfAbsent(
        externalToolchainServiceName(BinaryenToolchainService::class.java),
        DisabledBinaryenToolchainServiceImpl::class.java,
    ) { }

internal abstract class DisabledBinaryenToolchainServiceImpl : DisabledBinaryenToolchainService {

    override fun request(request: BinaryenToolchainRequest): Provider<ProvisionedBinaryenToolchain> =
        throw UnsupportedOperationException("Binaryen toolchain is disabled")
}

/**
 * Whether the Binaryen toolchain is disabled for the build.
 *
 * Realizes the build service, which is cheap - a service only provisions the tool when a request is made.
 */
internal fun Project.isBinaryenToolchainDisabled(): Boolean =
    locateOrRegisterBinaryenToolchainService().get() is DisabledBinaryenToolchainService


internal fun Project.requestDefaultBinaryenToolchain(): Provider<BinaryenToolchainRequest> {
    val objects = objects
    val version = kotlinPropertiesProvider.binaryenDefaultVersion
    return providers.detectExternalToolchainPlatform().map { platform ->
        objects.newInstance<BinaryenToolchainRequest>().also {
            it.version.convention(version)
            it.platform.convention(platform)
        }
    }
}

private fun BinaryenToolchainRequest.requiredVersion(): String =
    (version.orNull ?: error("A Binaryen version must be requested")).also {
        require(it.isNotBlank()) { "Binaryen version must not be blank" }
    }

private fun BinaryenToolchainRequest.requiredPlatform(): ExternalToolchainPlatform =
    platform.orNull ?: error("A Binaryen platform must be requested")

/**
 * Returns the `wasm-opt` executable provisioned by this service, or [legacyExecutable] when the toolchain is disabled.
 *
 * Provisions Binaryen, so it must only be called at execution time.
 */
internal fun BinaryenToolchainService<*>.resolveBinaryenToolchainExecutable(
    legacyExecutable: Provider<String>,
    request: Provider<BinaryenToolchainRequest>,
): String = if (this is DisabledBinaryenToolchainService) {
    legacyExecutable.get()
} else {
    request(request.get()).get().wasmOpt.toString()
}
