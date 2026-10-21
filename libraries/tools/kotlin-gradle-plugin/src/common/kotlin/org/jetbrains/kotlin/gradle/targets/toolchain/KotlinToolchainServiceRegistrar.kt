/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.toolchain

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.DownloadedToolchainParameters
import org.jetbrains.kotlin.gradle.dsl.toolchain.KotlinToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.PreInstalledToolchainParameters
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainRequest
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainVersion
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.userKotlinPersistentDir
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import java.io.File

/**
 * How a tool is provisioned for the build.
 */
internal enum class KotlinToolchainMode {

    /**
     * The tool distribution is downloaded and installed by the download-based toolchain service.
     */
    DOWNLOAD,

    /**
     * A tool already installed on the machine is used, and nothing is downloaded.
     */
    PREINSTALLED,

    /**
     * The toolchain service is not used - the tool is provisioned per project by its environment spec
     * and setup task, which is the behaviour of the Kotlin Gradle Plugin before the toolchain services.
     */
    DISABLE,
    ;
}

/**
 * The Gradle properties controlling the toolchain of a single tool.
 */
internal class KotlinToolchainProperties(
    val mode: Provider<String>,
    val installationDir: Provider<String>,
    val downloadUrl: Provider<String>,
    val preInstalledExecutablePath: Provider<String>,
)

/**
 * Registers the [KotlinToolchainService] of a single tool as a shared build service, and wires it into tasks.
 *
 * All the per-tool differences are passed in, so that D8, Binaryen, Wasmtime and SWC share
 * the very same registration, selection and task-wiring logic.
 *
 * @param S the tool-specific [KotlinToolchainService] subtype, for example `D8ToolchainService`.
 */
internal class KotlinToolchainServiceRegistrar<S : KotlinToolchainService<*>>(
    serviceInterface: Class<S>,
    private val downloadedServiceInterface: Class<out S>,
    private val downloadedServiceImpl: Class<out S>,
    private val preInstalledServiceInterface: Class<out S>,
    private val preInstalledServiceImpl: Class<out S>,
    private val disabledServiceInterface: Class<out S>,
    private val disabledServiceImpl: Class<out S>,
    private val defaultDownloadBaseUrl: String,
    private val defaultVersion: String,
    private val defaultPreInstalledCommand: String,
    /** The directory all installations are shared through, relative to `<user.home>/.kotlin`. */
    private val installationDirName: String,
    private val properties: (PropertiesProvider) -> KotlinToolchainProperties,
) {

    /**
     * The name the service is registered under.
     *
     * It includes the class loader hash, so that several Kotlin Gradle Plugin versions loaded in the same build
     * do not clash on the same registration.
     */
    val serviceName: String = "${serviceInterface.name}_${serviceInterface.classLoader.hashCode()}"

    @Suppress("UNCHECKED_CAST")
    private fun findRegistered(gradle: Gradle): Provider<out S>? =
        gradle.sharedServices.registrations.findByName(serviceName)?.service as Provider<out S>?

    /**
     * Registers the service selected in `settings.gradle.kts` by the `org.jetbrains.kotlin.ecosystem` plugin.
     *
     * [serviceClass] is one of the built-in service interfaces of the tool, which are mapped to their
     * implementations here, or a custom implementation class provided by the user.
     */
    fun registerIfAbsent(
        gradle: Gradle,
        serviceClass: Class<out KotlinToolchainService<*>>,
        configureBuildServiceParameters: (KotlinToolchainService.Parameters) -> Unit,
    ): Provider<out S> {
        findRegistered(gradle)?.let { return it }

        return when (serviceClass) {
            downloadedServiceInterface -> registerDownloaded(gradle, configureBuildServiceParameters)
            preInstalledServiceInterface -> registerPreInstalled(gradle, configureBuildServiceParameters)
            disabledServiceInterface -> registerDisabled(gradle)
            else -> {
                @Suppress("UNCHECKED_CAST")
                registerService(gradle, serviceClass as Class<out S>, configureBuildServiceParameters)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun registerService(
        gradle: Gradle,
        implementationClass: Class<out S>,
        configure: (KotlinToolchainService.Parameters) -> Unit,
    ): Provider<out S> = gradle.sharedServices.registerIfAbsent(
        serviceName,
        implementationClass as Class<BuildService<BuildServiceParameters>>,
    ) { spec ->
        configure(spec.parameters as KotlinToolchainService.Parameters)
    } as Provider<out S>

    /**
     * Registers the service selected by the Gradle properties of [project], unless a service is already registered
     * for the build - a service registered by the settings plugin always wins.
     */
    fun registerIfAbsent(project: Project): Provider<out S> {
        findRegistered(project.gradle)?.let { return it }

        val projectProperties = properties(project.kotlinPropertiesProvider)
        return when (projectProperties.mode()) {
            KotlinToolchainMode.DOWNLOAD -> registerDownloaded(project.gradle) { parameters ->
                parameters.installationDir.fileProvider(project.installationDir(projectProperties))
                parameters.downloadBaseUrl.set(projectProperties.downloadUrl.orElse(defaultDownloadBaseUrl))
            }
            KotlinToolchainMode.PREINSTALLED -> registerPreInstalled(project.gradle) { parameters ->
                parameters.executable.set(
                    projectProperties.preInstalledExecutablePath.orElse(defaultPreInstalledCommand)
                )
            }
            KotlinToolchainMode.DISABLE -> registerDisabled(project.gradle)
        }
    }

    /**
     * Registers the service selected for the build, and makes every task of [taskType] in [project] use it.
     */
    fun <T : Task> registerAndWireTasks(
        project: Project,
        taskType: Class<T>,
        setService: (T, Provider<out S>) -> Unit,
    ): Provider<out S> {
        val serviceProvider = registerIfAbsent(project)

        SingleActionPerProject.run(project, taskType.name) {
            project.tasks.withType(taskType).configureEach { task ->
                setService(task, serviceProvider)
                task.usesService(serviceProvider)
            }
        }
        return serviceProvider
    }

    /**
     * The default request for this tool - the default version on the platform of the machine running the build.
     */
    fun defaultRequest(project: Project): Provider<ToolchainRequest> = project.toolchainRequest(defaultVersion)

    fun isDisabled(service: KotlinToolchainService<*>): Boolean = disabledServiceImpl.isInstance(service)

    /**
     * Whether the toolchain of this tool is disabled for the build.
     *
     * Realizes the build service, which is cheap - a service only provisions the tool when a request is made.
     */
    fun isDisabled(project: Project): Boolean = isDisabled(registerIfAbsent(project).get())

    private fun registerDownloaded(
        gradle: Gradle,
        configure: (DownloadedToolchainParameters) -> Unit,
    ): Provider<out S> = registerService(gradle, downloadedServiceImpl) {
        val parameters = it as DownloadedToolchainParameters
        parameters.downloadBaseUrl.convention(defaultDownloadBaseUrl)
        parameters.installationDir.fileValue(
            File(System.getProperty("user.home")).resolve(".kotlin").resolve(installationDirName)
        )
        parameters.offline.convention(gradle.startParameter.isOffline)
        configure(parameters)
    }

    private fun registerPreInstalled(
        gradle: Gradle,
        configure: (PreInstalledToolchainParameters) -> Unit,
    ): Provider<out S> = registerService(gradle, preInstalledServiceImpl) {
        val parameters = it as PreInstalledToolchainParameters
        parameters.executable.convention(defaultPreInstalledCommand)
        configure(parameters)
    }

    private fun registerDisabled(gradle: Gradle): Provider<out S> =
        registerService(gradle, disabledServiceImpl) {}

    /**
     * The directory all installations are shared through, `<user.home>/.kotlin/toolchains/<tool>`.
     *
     * The base directory can be customized with the `kotlin.user.home` Gradle property, and defaults to `~/.kotlin`.
     */
    private fun Project.installationDir(projectProperties: KotlinToolchainProperties): Provider<File> {
        if (projectProperties.installationDir.isPresent) {
            return projectProperties.installationDir.map { File(it) }
        }
        return providers.provider { userKotlinPersistentDir.resolve(installationDirName) }
    }
}

private fun KotlinToolchainProperties.mode(): KotlinToolchainMode =
    mode.orNull?.let { KotlinToolchainMode.valueOf(it.toUpperCaseAsciiOnly()) } ?: KotlinToolchainMode.DISABLE

/**
 * A request for [defaultVersion] of a tool on the platform of the machine running the build.
 *
 * The returned provider intentionally captures only the object factory and the version, and not the project,
 * so that it stays compatible with the configuration cache.
 */
private fun Project.toolchainRequest(defaultVersion: String): Provider<ToolchainRequest> {
    val objects = objects
    return providers.detectToolchainPlatform().map { platform ->
        objects.newInstance<ToolchainRequest>().also {
            it.version.convention(ToolchainVersion(defaultVersion))
            it.platform.convention(platform)
        }
    }
}
