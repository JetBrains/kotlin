/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalNodeJsToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.*
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerBuild
import org.jetbrains.kotlin.gradle.plugin.statistics.NodeJsToolchainServiceMetrics
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.web.nodejs.nodeJsEnvSpec
import org.jetbrains.kotlin.gradle.tasks.nodejs.UsesNodeJsToolchainService
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.toolchains.ExternalToolchainArchiveDownloader
import org.jetbrains.kotlin.gradle.toolchains.ExternalToolchainGradleProperties
import org.jetbrains.kotlin.gradle.toolchains.ExternalToolchainMode
import org.jetbrains.kotlin.gradle.toolchains.ExternalToolchainProvisioner
import org.jetbrains.kotlin.gradle.toolchains.defaultExternalToolchainInstallationDir
import org.jetbrains.kotlin.gradle.toolchains.externalToolchainServiceName
import org.jetbrains.kotlin.gradle.toolchains.findExternalToolchainService
import org.jetbrains.kotlin.gradle.toolchains.registerExternalToolchainService
import org.jetbrains.kotlin.gradle.utils.copySubDirectoryContent
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File
import javax.inject.Inject

/**
 * The base URL of the official Node.js distributions.
 */
private const val OFFICIAL_NODE_JS_DOWNLOAD_BASE_URL = "https://nodejs.org/dist"

private const val NODE_JS_INSTALLATION_DIR_NAME = "toolchains/nodejs"

/**
 * The command used to run a pre-installed Node.js when no executable is configured.
 */
private const val DEFAULT_PRE_INSTALLED_NODE_JS_COMMAND = "node"

private val nodeJsToolchainGradleProperties = ExternalToolchainGradleProperties(
    mode = PropertyNames.KOTLIN_JS_NODEJS_TOOLCHAIN_MODE,
    installationDir = PropertyNames.KOTLIN_JS_NODEJS_TOOLCHAIN_INSTALLATION_DIR,
    downloadUrl = PropertyNames.KOTLIN_JS_NODEJS_TOOLCHAIN_DOWNLOAD_URL,
)

/**
 * Makes every [UsesNodeJsToolchainService] task of the project use the [NodeJsToolchainService] selected for the build.
 */
internal val NodeJsToolchainExtension = KotlinProjectSetupAction {
    val service = locateOrRegisterNodeJsToolchainService()
    tasks.withType<UsesNodeJsToolchainService>().configureEach { task ->
        task.nodeJsToolchainService.value(service).disallowChanges()
        task.usesService(service)
    }
    NodeJsToolchainServiceMetrics.collectServiceCreated(this, service)
}

/**
 * Returns the [NodeJsToolchainService] selected for the build in `settings.gradle.kts`,
 * or registers the one selected by the Gradle properties of this project.
 */
internal fun Project.locateOrRegisterNodeJsToolchainService(): Provider<out NodeJsToolchainService<*>> =
    gradle.findExternalToolchainService(NodeJsToolchainService::class.java)
        ?: registerNodeJsToolchainServiceFromGradleProperties()

private fun Project.registerNodeJsToolchainServiceFromGradleProperties(): Provider<out NodeJsToolchainService<*>> {
    val mode = nodeJsToolchainGradleProperties.mode(this)
    if (mode != ExternalToolchainMode.DISABLE) {
        reportDiagnosticOncePerBuild(
            KotlinToolingDiagnostics.ExperimentalFeatureWarning(
                "Node.js toolchain",
                "https://kotl.in/experimental-nodejs-toolchain",
            ),
            key = "NodeJsToolchain"
        )
    }

    return when (mode) {
        ExternalToolchainMode.DOWNLOAD ->
            gradle.registerDefaultNodeJsToolchainService(
                NodeJsToolchainSettings.fromGradleProperties(this)
            )
        ExternalToolchainMode.PREINSTALLED ->
            gradle.registerPreInstalledNodeJsToolchainService { parameters ->
                parameters.nodeJsExecutable.set(
                    kotlinPropertiesProvider.property(PropertyNames.KOTLIN_JS_NODEJS_TOOLCHAIN_PREINSTALLED_EXECUTABLE_PATH)
                        .orElse(DEFAULT_PRE_INSTALLED_NODE_JS_COMMAND)
                )
            }
        ExternalToolchainMode.DISABLE ->
            gradle.registerDisabledNodeJsToolchainService()
    }
}

/**
 * Registers the Node.js toolchain service selected with [serviceClass] in `settings.gradle.kts`.
 *
 * [serviceClass] is either one of the built-in Node.js services, which is mapped to its implementation here,
 * or a custom implementation class provided by the build author.
 */
@Suppress("UNCHECKED_CAST")
internal fun Gradle.registerNodeJsToolchainService(
    serviceClass: Class<out NodeJsToolchainService<*>>,
    configure: (NodeJsToolchainService.Parameters) -> Unit,
): Provider<out NodeJsToolchainService<*>> {
    findExternalToolchainService(NodeJsToolchainService::class.java)?.let { return it }

    return when (serviceClass) {
        DefaultNodeJsToolchainService::class.java ->
            registerDefaultNodeJsToolchainService(NodeJsToolchainSettings.defaults(this), configure)
        PreInstalledNodeJsToolchainService::class.java ->
            registerPreInstalledNodeJsToolchainService(configure)
        DisabledNodeJsToolchainService::class.java ->
            registerDisabledNodeJsToolchainService()
        else ->
            sharedServices.registerIfAbsent(
                externalToolchainServiceName(NodeJsToolchainService::class.java),
                serviceClass as Class<NodeJsToolchainService<NodeJsToolchainService.Parameters>>,
            ) { spec ->
                configure(spec.parameters)
            }
    }
}

/**
 * All settings of the download-based Node.js toolchain service the user can configure.
 */
internal data class NodeJsToolchainSettings(
    val installationDir: File,
    val downloadBaseUrl: String,
    val offline: Boolean,
) {

    companion object {
        /**
         * The settings of the service selected in `settings.gradle.kts`, before the build author configures them.
         */
        fun defaults(gradle: Gradle): NodeJsToolchainSettings = NodeJsToolchainSettings(
            installationDir = defaultExternalToolchainInstallationDir(NODE_JS_INSTALLATION_DIR_NAME),
            downloadBaseUrl = OFFICIAL_NODE_JS_DOWNLOAD_BASE_URL,
            offline = gradle.startParameter.isOffline,
        )

        /**
         * The settings of the service requested by [project], configured by its Gradle properties.
         */
        fun fromGradleProperties(project: Project): NodeJsToolchainSettings = NodeJsToolchainSettings(
            installationDir = nodeJsToolchainGradleProperties.installationDir(project, NODE_JS_INSTALLATION_DIR_NAME),
            downloadBaseUrl = nodeJsToolchainGradleProperties.downloadBaseUrl(project, OFFICIAL_NODE_JS_DOWNLOAD_BASE_URL),
            offline = project.gradle.startParameter.isOffline,
        )

        fun fromParameters(parameters: DefaultNodeJsToolchainService.Parameters): NodeJsToolchainSettings = NodeJsToolchainSettings(
            installationDir = parameters.installationDir.get().asFile,
            downloadBaseUrl = parameters.downloadBaseUrl.get(),
            offline = parameters.offline.get(),
        )
    }
}

/**
 * Registers [DefaultNodeJsToolchainService] for the build, configured by [settings], and then by [configure].
 */
internal fun Gradle.registerDefaultNodeJsToolchainService(
    settings: NodeJsToolchainSettings,
    configure: (DefaultNodeJsToolchainService.Parameters) -> Unit = {},
): Provider<out NodeJsToolchainService<*>> =
    registerExternalToolchainService<NodeJsToolchainService<*>, DefaultNodeJsToolchainService.Parameters>(
        externalToolchainServiceName(NodeJsToolchainService::class.java),
        DefaultNodeJsToolchainServiceImpl::class.java,
    ) { parameters ->
        parameters.installationDir.fileValue(settings.installationDir)
        parameters.downloadBaseUrl.convention(settings.downloadBaseUrl)
        parameters.offline.convention(settings.offline)
        configure(parameters)
    }

/**
 * Assembles the provisioner of [DefaultNodeJsToolchainService] from its [settings].
 *
 * It is called by the service itself, because a build service is instantiated by Gradle,
 * and only receives its parameters, which the build author may change in `settings.gradle.kts`.
 */
private fun nodeJsToolchainProvisioner(
    settings: NodeJsToolchainSettings,
    fs: FileSystemOperations,
    archiveOperations: ArchiveOperations,
    logger: Logger,
): ExternalToolchainProvisioner<NodeJsRequest> = ExternalToolchainProvisioner(
    toolchainName = "Node.js",
    installationsDir = settings.installationDir,
    installationName = { request -> nodeJsDistributionName(request.requiredVersion(), request.requiredPlatform()) },
    requiredFiles = { request -> listOf(nodeJsExecutablePath(request.requiredPlatform())) },
    archiveDownloader = ExternalToolchainArchiveDownloader(
        offline = settings.offline,
        urlForRequest = { request ->
            nodeJsArchiveUrl(settings.downloadBaseUrl, request.requiredVersion(), request.requiredPlatform())
        },
    ),
    unpackArchive = { request, archive, destination ->
        val platform = request.requiredPlatform()
        // The provisioner downloads into a file without the archive extension, so the compression is set explicitly.
        val archiveTree = if (platform.isWindows) {
            archiveOperations.zipTree(archive)
        } else {
            archiveOperations.tarTree(archiveOperations.gzip(archive))
        }
        fs.copySubDirectoryContent(
            archiveTree,
            nodeJsDistributionName(request.requiredVersion(), platform),
            destination,
        )
        prepareNodeJsInstallation(logger, destination, platform.isWindows)
    },
)

/**
 * The URL of the official Node.js distribution archive,
 * for example `https://nodejs.org/dist/v24.22.1/node-v24.22.1-linux-x64.tar.gz`.
 */
internal fun nodeJsArchiveUrl(downloadBaseUrl: String, version: NodeJsVersion, platform: BuildPlatform): String =
    "${downloadBaseUrl.trimEnd('/')}/v${version.normalized}/" +
            "${nodeJsDistributionName(version, platform)}.${nodeJsArchiveExtension(platform)}"

/**
 * The default [NodeJsToolchainService] used by the Kotlin Gradle Plugin.
 *
 * Downloads the requested Node.js distribution and installs it into a shared, machine-wide directory,
 * so that a distribution is downloaded once and then reused by all builds on the machine.
 */
internal abstract class DefaultNodeJsToolchainServiceImpl @Inject internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    private val fs: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) : DefaultNodeJsToolchainService {

    private val logger = Logging.getLogger(DefaultNodeJsToolchainServiceImpl::class.java)

    private val provisioner by lazy {
        nodeJsToolchainProvisioner(NodeJsToolchainSettings.fromParameters(parameters), fs, archiveOperations, logger)
    }

    override fun request(nodeJsRequest: NodeJsRequest): Provider<NodeJsExecutable> = providers.provider {
        val version = nodeJsRequest.requiredVersion()
        val platform = nodeJsRequest.requiredPlatform()
        logger.reportDiagnosticWhenNodeJsVersionUnsupported(version)

        val installation = provisioner.provision(nodeJsRequest)

        objects.newInstance<NodeJsExecutable>().apply {
            executable.set(nodeJsExecutableFile(installation, platform).absolutePath)
            this.version.set(version)
            this.platform.set(platform)
        }
    }
}

/**
 * Registers [PreInstalledNodeJsToolchainService] for the build, configured by [configure].
 */
internal fun Gradle.registerPreInstalledNodeJsToolchainService(
    configure: (PreInstalledNodeJsToolchainService.Parameters) -> Unit = {},
): Provider<out NodeJsToolchainService<*>> =
    registerExternalToolchainService<NodeJsToolchainService<*>, PreInstalledNodeJsToolchainService.Parameters>(
        externalToolchainServiceName(NodeJsToolchainService::class.java),
        PreInstalledNodeJsToolchainServiceImpl::class.java,
    ) { parameters ->
        parameters.nodeJsExecutable.convention(DEFAULT_PRE_INSTALLED_NODE_JS_COMMAND)
        configure(parameters)
    }

internal fun Gradle.registerDisabledNodeJsToolchainService(): Provider<out NodeJsToolchainService<*>> =
    sharedServices.registerIfAbsent(
        externalToolchainServiceName(NodeJsToolchainService::class.java),
        DisabledNodeJsToolchainServiceImpl::class.java,
    )

internal abstract class DisabledNodeJsToolchainServiceImpl : DisabledNodeJsToolchainService {

    override fun request(nodeJsRequest: NodeJsRequest): Provider<NodeJsExecutable> =
        throw UnsupportedOperationException("Node.js toolchain is disabled")
}

/**
 * The oldest Node.js version the Kotlin Gradle Plugin is tested against.
 *
 * Requesting an older version is not forbidden, but produces a warning.
 */
private const val MINIMAL_SUPPORTED_NODE_JS_MAJOR_VERSION = 18

internal fun Logger.reportDiagnosticWhenNodeJsVersionUnsupported(installedVersion: NodeJsVersion) {
    val installedMajorVersion = installedVersion.majorVersion
    if (installedMajorVersion != null && installedMajorVersion < MINIMAL_SUPPORTED_NODE_JS_MAJOR_VERSION) {
        warn(
            "Node.js ${installedVersion.normalized} is not supported by the Kotlin Gradle Plugin. " +
                    "The minimal supported version is $MINIMAL_SUPPORTED_NODE_JS_MAJOR_VERSION. " +
                    "Please use Node.js $MINIMAL_SUPPORTED_NODE_JS_MAJOR_VERSION or a newer version."
        )
    }
}

/**
 * The path of the Node.js executable relative to the root of an installed distribution.
 */
internal fun nodeJsExecutablePath(platform: BuildPlatform): String =
    if (platform.isWindows) "node.exe" else "bin/node"

/**
 * The Node.js executable inside an installed distribution.
 */
internal fun nodeJsExecutableFile(installationDir: File, platform: BuildPlatform): File =
    installationDir.resolve(nodeJsExecutablePath(platform))

/**
 * The Node.js executable of the old (non-toolchain) setup. It is empty when a toolchain service provisions Node.js.
 *
 * Intended to be a task input, so the task is not tied to the old executable when the toolchain is enabled.
 * Use [resolveNodeJsExecutable] to get the executable to run.
 */
@Suppress("DEPRECATION")
internal fun Provider<NodeJsToolchainService<*>>.legacyNodeJsExecutable(
    objects: ObjectFactory,
    compilation: KotlinJsIrCompilation,
): Provider<String> = flatMap { service ->
    if (service.isNodeJsToolchainDisabled()) {
        objects.property(compilation.nodeJsEnvSpec).flatMap { it.executable }
    } else objects.property()
}

/**
 * Returns the Node.js executable provisioned by this service, or [legacyNodeExecutable] when the toolchain is disabled.
 *
 * Provisions Node.js, so it must only be called at execution time.
 */
internal fun NodeJsToolchainService<*>.resolveNodeJsExecutable(
    legacyNodeExecutable: Provider<String>,
    nodeJsRequest: Provider<NodeJsRequest>,
): String = if (isNodeJsToolchainDisabled()) {
    legacyNodeExecutable.get()
} else {
    requestNodeJsExecutable(nodeJsRequest)
}

/**
 * Returns the Node.js executable provisioned by this service, or provisioned by [npmProject] when the toolchain is disabled.
 *
 * Provisions Node.js, so it must only be called at execution time.
 */
@Suppress("DEPRECATION")
internal fun NodeJsToolchainService<*>.resolveNodeJsExecutable(
    npmProject: NpmProject,
    nodeJsRequest: Provider<NodeJsRequest>,
): String = if (isNodeJsToolchainDisabled()) {
    npmProject.nodeExecutable
} else {
    requestNodeJsExecutable(nodeJsRequest)
}

private fun NodeJsToolchainService<*>.requestNodeJsExecutable(nodeJsRequest: Provider<NodeJsRequest>): String =
    request(nodeJsRequest.get()).get().executable.orNull ?: error("Node js executable should be provisioned")

internal fun NodeJsToolchainService<*>.isNodeJsToolchainDisabled(): Boolean = this is DisabledNodeJsToolchainService

internal fun Project.requestDefaultNodeJs(): Provider<NodeJsRequest> {
    val objects = objects
    val version = kotlinPropertiesProvider.nodejsDefaultVersion
    return providers.detectBuildPlatform().map { platform ->
        objects.newInstance<NodeJsRequest>().also {
            it.version.convention(version)
            it.platform.convention(platform)
        }
    }
}

private fun NodeJsRequest.requiredVersion(): NodeJsVersion =
    version.orNull ?: error("A Node.js version must be requested")

private fun NodeJsRequest.requiredPlatform(): BuildPlatform =
    platform.orNull ?: error("A Node.js platform must be requested")
