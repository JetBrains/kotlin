/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.toolchains

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
import org.jetbrains.kotlin.gradle.dsl.toolchain.*
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.*
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames
import org.jetbrains.kotlin.gradle.targets.toolchain.*
import org.jetbrains.kotlin.gradle.tasks.toolchain.UsesBinaryenToolchainService
import java.io.File
import javax.inject.Inject

/**
 * The default Binaryen version provisioned by the Binaryen toolchain.
 *
 * Kept in sync with the default of the Binaryen environment spec.
 */
internal const val DEFAULT_BINARYEN_VERSION: String = "133"

private const val BINARYEN = "Binaryen"

private const val WASM_OPT_COMMAND = "wasm-opt"

/**
 * See https://github.com/WebAssembly/binaryen/releases for the available archives.
 */
private const val BINARYEN_DOWNLOAD_BASE_URL = "https://github.com/WebAssembly/binaryen/releases/download"

private const val BINARYEN_INSTALLATION_DIR_NAME = "toolchains/binaryen"

private val binaryenGradleProperties = ToolchainGradleProperties(
    mode = PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_MODE,
    installationDir = PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_INSTALLATION_DIR,
    downloadUrl = PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_DOWNLOAD_URL,
    preInstalledExecutablePath = PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_PREINSTALLED_EXECUTABLE_PATH,
)

private val binaryenTasks = ToolchainTaskBinding(UsesBinaryenToolchainService::class.java) { it.binaryenToolchainService }

internal val binaryenServiceName: String = toolchainServiceName(BinaryenToolchainService::class.java)

/**
 * Makes the Binaryen toolchain service available to the project, and makes its tasks use it.
 */
internal val BinaryenToolchainExtension = KotlinProjectSetupAction {
    registerBinaryenToolchainServiceIfAbsent(project)
}

/**
 * Returns the Binaryen toolchain service of the build, and makes every [UsesBinaryenToolchainService] task
 * in [project] use it.
 *
 * The service may already be registered - by the `org.jetbrains.kotlin.ecosystem` settings plugin, which always wins,
 * or by another project. Otherwise, it is registered with the settings from the Gradle properties of [project].
 */
internal fun registerBinaryenToolchainServiceIfAbsent(project: Project): Provider<out BinaryenToolchainService<*>> {
    val service = project.gradle.findToolchainService(binaryenServiceName)
        ?: project.registerBinaryenToolchainServiceFromGradleProperties()
    binaryenTasks.bind(project, service)
    return service
}

private fun Project.registerBinaryenToolchainServiceFromGradleProperties(): Provider<out BinaryenToolchainService<*>> =
    when (binaryenGradleProperties.mode(this)) {
        KotlinToolchainMode.DOWNLOAD ->
            gradle.registerDefaultBinaryenToolchainService(BinaryenToolchainSettings.fromGradleProperties(this))
        KotlinToolchainMode.PREINSTALLED ->
            gradle.registerPreInstalledBinaryenToolchainService(
                PreInstalledToolchainSettings(binaryenGradleProperties.preInstalledExecutablePath(this).getOrElse(WASM_OPT_COMMAND))
            )
        KotlinToolchainMode.DISABLE ->
            gradle.registerDisabledBinaryenToolchainService()
    }

/**
 * Registers the Binaryen toolchain service selected with [serviceClass] in `settings.gradle.kts`.
 *
 * [serviceClass] is either one of the built-in Binaryen services, which is mapped to its implementation here,
 * or a custom implementation class provided by the build author.
 */
internal fun Gradle.registerBinaryenToolchainService(
    serviceClass: Class<out KotlinToolchainService<*>>,
    configure: (KotlinToolchainService.Parameters) -> Unit,
): Provider<out BinaryenToolchainService<*>> {
    findToolchainService<BinaryenToolchainService<*>>(binaryenServiceName)?.let { return it }

    return when (serviceClass) {
        DefaultBinaryenToolchainService::class.java ->
            registerDefaultBinaryenToolchainService(BinaryenToolchainSettings.defaults(this), configure)
        PreInstalledBinaryenToolchainService::class.java ->
            registerPreInstalledBinaryenToolchainService(PreInstalledToolchainSettings(WASM_OPT_COMMAND), configure)
        DisabledBinaryenToolchainService::class.java ->
            registerDisabledBinaryenToolchainService()
        else ->
            registerToolchainService(binaryenServiceName, serviceClass, configure)
    }
}

/**
 * The requested Binaryen distribution, and how the Binaryen project publishes it.
 */
internal data class KotlinBinaryenToolchain(
    override val version: ToolchainVersion,
    override val platform: ToolchainPlatform,
) : ExternalToolchainForKotlin {

    override val toolName: String get() = BINARYEN

    override val installationName: String get() = "binaryen-version_${version.version}-$classifier"

    override val executablePath: String get() = "bin/${platform.executableName(WASM_OPT_COMMAND)}"

    val archivePath: String get() = "version_${version.version}/$installationName.tar.gz"

    val archiveRootDirName: String get() = "binaryen-version_${version.version}"

    /**
     * The platform part of the archive name, for example `x86_64-linux` or `arm64-macos`.
     */
    private val classifier: String
        get() {
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

/**
 * All settings of the download-based Binaryen toolchain service the user can configure.
 */
internal data class BinaryenToolchainSettings(
    override val installationDir: File,
    override val downloadBaseUrl: String,
    override val offline: Boolean,
) : DownloadedToolchainSettings {

    companion object {
        /**
         * The settings of the service selected in `settings.gradle.kts`, before the build author configures them.
         */
        fun defaults(gradle: Gradle): BinaryenToolchainSettings = BinaryenToolchainSettings(
            installationDir = defaultInstallationDir(BINARYEN_INSTALLATION_DIR_NAME),
            downloadBaseUrl = BINARYEN_DOWNLOAD_BASE_URL,
            offline = gradle.startParameter.isOffline,
        )

        /**
         * The settings of the service requested by [project], configured by its Gradle properties.
         */
        fun fromGradleProperties(project: Project): BinaryenToolchainSettings = BinaryenToolchainSettings(
            installationDir = binaryenGradleProperties.installationDir(project, BINARYEN_INSTALLATION_DIR_NAME),
            downloadBaseUrl = binaryenGradleProperties.downloadBaseUrl(project, BINARYEN_DOWNLOAD_BASE_URL),
            offline = project.gradle.startParameter.isOffline,
        )

        fun fromParameters(parameters: DownloadedToolchainParameters): BinaryenToolchainSettings = BinaryenToolchainSettings(
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
    configure: (DownloadedToolchainParameters) -> Unit = {},
): Provider<out BinaryenToolchainService<*>> =
    registerToolchainService<BinaryenToolchainService<*>, DownloadedToolchainParameters>(
        binaryenServiceName,
        DefaultBinaryenToolchainServiceImpl::class.java,
    ) { parameters ->
        parameters.applyConventions(settings)
        configure(parameters)
    }

/**
 * Assembles the provisioner of [DefaultBinaryenToolchainService] from its [settings].
 *
 * It is called by the service itself, because a build service is instantiated by Gradle,
 * and only receives its parameters, which the build author may change in `settings.gradle.kts`.
 */
private fun binaryenProvisioner(settings: BinaryenToolchainSettings) = DefaultExternalProvisioner(
    archiveDownloader = DefaultExternalToolchainDownloader { settings.downloadUrl(it.archivePath) },
    extractor = DefaultExternalToolchainExtractor<KotlinBinaryenToolchain>(
        archiveExtractor = TarGzToolchainExtractor,
        archiveRootDir = { it.archiveRootDirName },
    ),
    installationsDir = settings.installationDir,
    offline = settings.offline,
)

internal abstract class DefaultBinaryenToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    fs: FileSystemOperations,
    archiveOperations: ArchiveOperations,
) : DefaultBinaryenToolchainService {

    private val services = ToolchainProvisioningServices(
        fs,
        archiveOperations,
        Logging.getLogger(DefaultBinaryenToolchainServiceImpl::class.java),
    )

    private val provisioner by lazy { binaryenProvisioner(BinaryenToolchainSettings.fromParameters(parameters)) }

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> = providers.provider {
        val toolchain = KotlinBinaryenToolchain(request.requiredVersion(BINARYEN), request.requiredPlatform(BINARYEN))
        provisioner.provisionExecutable(toolchain, services, objects)
    }
}

/**
 * Registers [PreInstalledBinaryenToolchainService] for the build, configured by [settings], and then by [configure].
 */
internal fun Gradle.registerPreInstalledBinaryenToolchainService(
    settings: PreInstalledToolchainSettings,
    configure: (PreInstalledToolchainParameters) -> Unit = {},
): Provider<out BinaryenToolchainService<*>> =
    registerToolchainService<BinaryenToolchainService<*>, PreInstalledToolchainParameters>(
        binaryenServiceName,
        PreInstalledBinaryenToolchainServiceImpl::class.java,
    ) { parameters ->
        parameters.executable.convention(settings.executable)
        configure(parameters)
    }

/**
 * Uses the Binaryen already installed on the machine. Nothing is provisioned, so the requested version is only reported.
 */
internal abstract class PreInstalledBinaryenToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) : PreInstalledBinaryenToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> = providers.provider {
        objects.toolchainExecutable(
            executable = parameters.executable.get(),
            version = request.version.getOrElse(ToolchainVersion(DEFAULT_BINARYEN_VERSION)),
            platform = request.requiredPlatform(BINARYEN),
        )
    }
}

internal fun Gradle.registerDisabledBinaryenToolchainService(): Provider<out BinaryenToolchainService<*>> =
    registerToolchainService<BinaryenToolchainService<*>, KotlinToolchainService.Parameters>(
        binaryenServiceName,
        DisabledBinaryenToolchainServiceImpl::class.java,
    ) { }

internal abstract class DisabledBinaryenToolchainServiceImpl : DisabledBinaryenToolchainService {

    override fun request(request: ToolchainRequest): Provider<ToolchainExecutable> =
        throw UnsupportedOperationException("Binaryen toolchain is disabled")
}

/**
 * Whether the Binaryen toolchain is disabled for the build.
 *
 * Realizes the build service, which is cheap - a service only provisions the tool when a request is made.
 */
internal fun Project.isBinaryenToolchainDisabled(): Boolean =
    registerBinaryenToolchainServiceIfAbsent(this).get() is DisabledBinaryenToolchainServiceImpl

internal fun Project.requestDefaultBinaryen(): Provider<ToolchainRequest> =
    defaultToolchainRequest(this, DEFAULT_BINARYEN_VERSION)

/**
 * Returns the `wasm-opt` executable provisioned by this service, or [legacyExecutable] when the toolchain is disabled.
 *
 * Provisions Binaryen, so it must only be called at execution time.
 */
internal fun BinaryenToolchainService<*>.resolveBinaryenExecutable(
    legacyExecutable: Provider<String>,
    request: Provider<ToolchainRequest>,
): String = resolveToolchainExecutable(BINARYEN, this is DisabledBinaryenToolchainServiceImpl, legacyExecutable, request)

/**
 * The `toolchainManagement { binaryen { ... } }` DSL.
 *
 * @suppress
 */
@InternalKotlinGradlePluginApi
class BinaryenToolchainManagementDslImpl(gradle: Gradle) :
    BinaryenToolchainManagementDsl,
    ToolchainManagementDsl by ToolchainManagementDslImpl(
        dslName = "binaryen",
        defaultServiceType = DefaultBinaryenToolchainService::class.java,
        disabledServiceType = DisabledBinaryenToolchainService::class.java,
        register = { serviceClass, configure -> gradle.registerBinaryenToolchainService(serviceClass, configure) },
    )
