/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.toolchain

import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.userKotlinPersistentDir
import java.io.File

/**
 * A built-in way of provisioning a tool: the public [serviceType] a build author selects, for example
 * `DefaultD8ToolchainService`, the internal [implementation] registered for it, and the [parameters] configurer.
 */
internal open class ToolchainServiceVariant(
    val serviceType: Class<out KotlinToolchainService<*>>,
    val implementation: Class<out KotlinToolchainService<*>>,
    val parameters: ToolchainParametersConfigurer,
)

/**
 * Configures the parameters of a [ToolchainServiceVariant].
 */
internal interface ToolchainParametersConfigurer {

    /**
     * Applies the defaults, when the variant is selected in `settings.gradle.kts`,
     * before the parameters are configured by the build author.
     */
    fun applyDefaults(parameters: KotlinToolchainService.Parameters, gradle: Gradle) {}

    /**
     * Configures the parameters from the Gradle [properties] of [project], when the variant is selected by them.
     */
    fun applyGradleProperties(
        parameters: KotlinToolchainService.Parameters,
        project: Project,
        properties: ToolchainGradleProperties,
    ) {
    }
}

internal object NoParametersConfigurer : ToolchainParametersConfigurer

/**
 * Creates the settings [S] of a toolchain service - a simple class listing everything the user can configure -
 * and passes them to the service through its build service parameters [P].
 *
 * Depending on where the service is selected, the settings come from:
 * - the `toolchainManagement { ... }` DSL of the `org.jetbrains.kotlin.ecosystem` settings plugin - the [defaults]
 *   are applied to the parameters, which the build author then configures;
 * - the Gradle properties of the project requesting the service - see [fromGradleProperties].
 *
 * The service itself reads its settings back with [fromParameters].
 */
internal class ToolchainSettingsMapping<P : KotlinToolchainService.Parameters, S : Any>(
    private val defaults: (Gradle) -> S,
    private val fromGradleProperties: (Project, ToolchainGradleProperties) -> S,
    val fromParameters: (P) -> S,
    private val applyConventions: (P, S) -> Unit,
) : ToolchainParametersConfigurer {

    override fun applyDefaults(parameters: KotlinToolchainService.Parameters, gradle: Gradle) {
        applyConventions(parameters.cast(), defaults(gradle))
    }

    override fun applyGradleProperties(
        parameters: KotlinToolchainService.Parameters,
        project: Project,
        properties: ToolchainGradleProperties,
    ) {
        applyConventions(parameters.cast(), fromGradleProperties(project, properties))
    }

    @Suppress("UNCHECKED_CAST")
    private fun KotlinToolchainService.Parameters.cast(): P = this as P
}

/**
 * The [ToolchainSettingsMapping] of a download-based toolchain service having only the common [DownloadedToolchainSettings].
 *
 * @param installationDirName the directory all installations are shared through, relative to `<user.home>/.kotlin`.
 * @param create the constructor of the tool-specific settings class.
 */
internal fun <S : DownloadedToolchainSettings> downloadedToolchainSettings(
    defaultDownloadBaseUrl: String,
    installationDirName: String,
    create: (installationDir: File, downloadBaseUrl: String, offline: Boolean) -> S,
): ToolchainSettingsMapping<DownloadedToolchainParameters, S> = ToolchainSettingsMapping(
    defaults = { gradle ->
        create(defaultInstallationDir(installationDirName), defaultDownloadBaseUrl, gradle.startParameter.isOffline)
    },
    fromGradleProperties = { project, properties ->
        create(
            properties.installationDir(project, installationDirName),
            properties.downloadBaseUrl(project, defaultDownloadBaseUrl),
            project.gradle.startParameter.isOffline,
        )
    },
    fromParameters = { create(it.installationDir.get().asFile, it.downloadBaseUrl.get(), it.offline.get()) },
    applyConventions = { parameters, settings -> parameters.applyConventions(settings) },
)

/**
 * The directory all installations are shared through by default, `<user.home>/.kotlin/<installationDirName>`.
 */
internal fun defaultInstallationDir(installationDirName: String): File =
    File(System.getProperty("user.home")).resolve(".kotlin").resolve(installationDirName)

internal fun DownloadedToolchainParameters.applyConventions(settings: DownloadedToolchainSettings) {
    installationDir.fileValue(settings.installationDir)
    downloadBaseUrl.convention(settings.downloadBaseUrl)
    offline.convention(settings.offline)
}

/**
 * A download-based toolchain service.
 *
 * @param settings creates the settings of the service, see [ToolchainSettingsMapping].
 * @param toolchainFor describes the requested toolchain distribution, given the request and the settings.
 * @param provisionerFor creates the provisioner of the service from its settings.
 */
internal class DownloadedToolchainServiceVariant<P : DownloadedToolchainParameters, S : DownloadedToolchainSettings, T : ExternalToolchainForKotlin>(
    serviceType: Class<out KotlinToolchainService<P>>,
    implementation: Class<out KotlinToolchainService<P>>,
    private val settings: ToolchainSettingsMapping<P, S>,
    private val toolchainFor: (ToolchainRequest, S) -> T,
    private val provisionerFor: (S) -> ExternalToolchainProvisioner<T>,
) : ToolchainServiceVariant(serviceType, implementation, settings) {

    /**
     * Creates the state of a single service instance, reading its settings from [parameters] on the first request.
     */
    fun provisioning(
        objects: ObjectFactory,
        providers: ProviderFactory,
        fs: FileSystemOperations,
        archiveOperations: ArchiveOperations,
        parameters: () -> P,
    ): Provisioning = Provisioning(
        objects,
        providers,
        ToolchainProvisioningServices(fs, archiveOperations, Logging.getLogger(implementation)),
        parameters,
    )

    inner class Provisioning(
        private val objects: ObjectFactory,
        private val providers: ProviderFactory,
        private val services: ToolchainProvisioningServices,
        parameters: () -> P,
    ) {
        private val toolchainSettings by lazy { settings.fromParameters(parameters()) }
        private val provisioner by lazy { provisionerFor(toolchainSettings) }

        /**
         * The logic behind [KotlinToolchainService.request] - provisions the requested distribution lazily,
         * when the returned provider is queried.
         */
        fun request(request: ToolchainRequest): Provider<ToolchainExecutable> = providers.provider {
            provisioner.provisionExecutable(toolchainFor(request, toolchainSettings), services, objects)
        }
    }
}

/**
 * All settings of a pre-installed toolchain service the user can configure.
 *
 * @param executable the command used to run the pre-installed tool.
 */
internal data class PreInstalledToolchainSettings(val executable: String)

/**
 * A toolchain service that uses the tool already installed on the machine.
 *
 * Nothing is provisioned, so the requested version is only reported, and not verified.
 */
internal class PreInstalledToolchainServiceVariant private constructor(
    serviceType: Class<out KotlinToolchainService<PreInstalledToolchainParameters>>,
    implementation: Class<out KotlinToolchainService<PreInstalledToolchainParameters>>,
    private val settings: ToolchainSettingsMapping<PreInstalledToolchainParameters, PreInstalledToolchainSettings>,
    private val defaultVersion: String,
) : ToolchainServiceVariant(serviceType, implementation, settings) {

    /**
     * @param defaultCommand the command used to run the tool when no executable is configured, for example `wasm-opt`.
     * @param defaultVersion the version reported when no version is requested.
     */
    constructor(
        serviceType: Class<out KotlinToolchainService<PreInstalledToolchainParameters>>,
        implementation: Class<out KotlinToolchainService<PreInstalledToolchainParameters>>,
        defaultCommand: String,
        defaultVersion: String,
    ) : this(serviceType, implementation, preInstalledToolchainSettings(defaultCommand), defaultVersion)

    /**
     * The logic behind [KotlinToolchainService.request].
     */
    fun request(
        objects: ObjectFactory,
        providers: ProviderFactory,
        request: ToolchainRequest,
        parameters: PreInstalledToolchainParameters,
    ): Provider<ToolchainExecutable> = providers.provider {
        objects.toolchainExecutable(
            executable = settings.fromParameters(parameters).executable,
            version = request.version.getOrElse(ToolchainVersion(defaultVersion)),
            platform = request.platform.get(),
        )
    }
}

private fun preInstalledToolchainSettings(defaultCommand: String) =
    ToolchainSettingsMapping<PreInstalledToolchainParameters, PreInstalledToolchainSettings>(
        defaults = { PreInstalledToolchainSettings(defaultCommand) },
        fromGradleProperties = { project, properties ->
            PreInstalledToolchainSettings(properties.preInstalledExecutablePath(project).getOrElse(defaultCommand))
        },
        fromParameters = { PreInstalledToolchainSettings(it.executable.get()) },
        applyConventions = { parameters, settings -> parameters.executable.convention(settings.executable) },
    )

internal inline fun <reified I : KotlinToolchainService<*>, reified Impl : I> disabledToolchainService(
): ToolchainServiceVariant = ToolchainServiceVariant(I::class.java, Impl::class.java, NoParametersConfigurer)

internal fun ObjectFactory.toolchainExecutable(
    executable: String,
    version: ToolchainVersion,
    platform: ToolchainPlatform,
): ToolchainExecutable = newInstance<ToolchainExecutable>().apply {
    this.executable.set(executable)
    this.version.set(version)
    this.platform.set(platform)
}

/**
 * The names of the Gradle properties controlling the toolchain of a single tool.
 */
internal class ToolchainGradleProperties(
    private val mode: String,
    private val installationDir: String,
    private val downloadUrl: String,
    private val preInstalledExecutablePath: String,
) {
    fun mode(project: Project): KotlinToolchainMode =
        project.kotlinPropertiesProvider.property(mode).orNull
            ?.let { KotlinToolchainMode.valueOf(it.uppercase()) }
            ?: KotlinToolchainMode.DISABLE

    /**
     * The directory all installations are shared through, `<user.home>/.kotlin/<installationDirName>` by default.
     *
     * The base directory can be customized with the `kotlin.user.home` Gradle property, and defaults to `~/.kotlin`.
     */
    fun installationDir(project: Project, installationDirName: String): File =
        project.kotlinPropertiesProvider.property(installationDir).orNull?.let(::File)
            ?: project.userKotlinPersistentDir.resolve(installationDirName)

    fun downloadBaseUrl(project: Project, defaultDownloadBaseUrl: String): String =
        project.kotlinPropertiesProvider.property(downloadUrl).getOrElse(defaultDownloadBaseUrl)

    fun preInstalledExecutablePath(project: Project): Provider<String> =
        project.kotlinPropertiesProvider.property(preInstalledExecutablePath)
}
