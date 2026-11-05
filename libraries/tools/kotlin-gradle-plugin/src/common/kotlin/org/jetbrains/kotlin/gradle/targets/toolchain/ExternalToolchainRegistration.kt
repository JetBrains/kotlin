/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.toolchain

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.KotlinToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainRequest
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainVersion
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.newInstance

/**
 * How a tool is provisioned for the build, selected by the `<tool>ToolchainMode` Gradle property.
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
 * Makes the tasks of [taskType] use the toolchain service, through the [serviceProperty] of the task.
 */
internal class ToolchainTaskBinding<S : KotlinToolchainService<*>, T : Task>(
    private val taskType: Class<T>,
    private val serviceProperty: (T) -> Property<S>,
) {
    fun bind(project: Project, service: Provider<out S>) {
        SingleActionPerProject.run(project, taskType.name) {
            project.tasks.withType(taskType).configureEach { task ->
                serviceProperty(task).value(service).disallowChanges()
                task.usesService(service)
            }
        }
    }
}

/**
 * Declares the toolchain of a single tool, for example D8, provisioned for the whole build by one shared
 * [KotlinToolchainService] of type [S].
 *
 * @param name the name of the tool used in messages, for example `D8`. Its lowercase form is the DSL block name.
 * @param properties the Gradle properties selecting and configuring a service variant per project.
 * @param tasks makes the tasks that run the tool use the service.
 * @param downloaded the built-in service downloading the tool with its provisioner.
 * @param preInstalled the built-in service using the tool already installed on the machine.
 * @param disabled the built-in service disabling the toolchain.
 */
internal inline fun <reified S : KotlinToolchainService<*>> registerExternalToolchain(
    name: String,
    defaultVersion: String,
    properties: ToolchainGradleProperties,
    tasks: ToolchainTaskBinding<S, *>,
    downloaded: DownloadedToolchainServiceVariant<*, *, *>,
    preInstalled: PreInstalledToolchainServiceVariant,
    disabled: ToolchainServiceVariant,
): ExternalToolchainRegistration<S> = ExternalToolchainRegistration(
    toolName = name,
    serviceType = S::class.java,
    defaultVersion = defaultVersion,
    properties = properties,
    tasks = tasks,
    downloaded = downloaded,
    preInstalled = preInstalled,
    disabled = disabled,
)

/**
 * The toolchain of a single tool, declared with [registerExternalToolchain].
 *
 * All tools share the very same registration, selection and task-wiring logic, implemented here:
 * - the built-in service variants - [downloaded], [preInstalled] and [disabled];
 * - the Gradle [properties] selecting and configuring a variant per project;
 * - the [tasks] binding, making the tasks that run the tool use the service.
 *
 * The service selected in `settings.gradle.kts` by the `org.jetbrains.kotlin.ecosystem` plugin always wins
 * over the one selected by the Gradle properties.
 *
 * @param S the tool-specific [KotlinToolchainService] subtype, for example `D8ToolchainService`.
 */
internal class ExternalToolchainRegistration<S : KotlinToolchainService<*>>(
    val toolName: String,
    serviceType: Class<S>,
    private val defaultVersion: String,
    private val properties: ToolchainGradleProperties,
    private val tasks: ToolchainTaskBinding<S, *>,
    val downloaded: ToolchainServiceVariant,
    val preInstalled: ToolchainServiceVariant,
    val disabled: ToolchainServiceVariant,
) {

    /**
     * The name of the `toolchainManagement { <dslName> { ... } }` block.
     */
    val dslName: String = toolName.lowercase()

    /**
     * The name the service is registered under, see [toolchainServiceName].
     */
    val serviceName: String = toolchainServiceName(serviceType)

    private val variants = listOf(downloaded, preInstalled, disabled)

    /**
     * Registers the service selected in `settings.gradle.kts`.
     *
     * [serviceClass] is the [ToolchainServiceVariant.serviceType] of a built-in variant, which is mapped to its
     * implementation here, or a custom implementation class provided by the build author.
     */
    fun registerIfAbsent(
        gradle: Gradle,
        serviceClass: Class<out KotlinToolchainService<*>>,
        configure: (KotlinToolchainService.Parameters) -> Unit,
    ): Provider<out S> {
        findRegistered(gradle)?.let { return it }

        val variant = variants.find { it.serviceType == serviceClass }
            ?: return register(gradle, serviceClass, configure)

        return register(gradle, variant.implementation) { parameters ->
            variant.parameters.applyDefaults(parameters, gradle)
            configure(parameters)
        }
    }

    /**
     * Registers the service selected by the Gradle properties of [project], unless a service is already registered
     * for the build, and makes the tasks of [project] that run the tool use it.
     */
    fun registerIfAbsent(project: Project): Provider<out S> {
        val service = findRegistered(project.gradle) ?: registerFromGradleProperties(project)
        tasks.bind(project, service)
        return service
    }

    /**
     * The default request for this tool - the default version on the platform of the machine running the build.
     *
     * The returned provider intentionally captures only the object factory and the version, and neither the project
     * nor this toolchain, so that it stays compatible with the configuration cache.
     */
    fun defaultRequest(project: Project): Provider<ToolchainRequest> = defaultToolchainRequest(project, defaultVersion)

    fun isDisabled(service: KotlinToolchainService<*>): Boolean = disabled.implementation.isInstance(service)

    /**
     * Whether the toolchain of this tool is disabled for the build.
     *
     * Realizes the build service, which is cheap - a service only provisions the tool when a request is made.
     */
    fun isDisabled(project: Project): Boolean = isDisabled(registerIfAbsent(project).get())

    /**
     * Returns the executable provisioned by [service], or [legacyExecutable] when the toolchain is disabled.
     *
     * Provisions the tool, so it must only be called at execution time.
     */
    fun resolveExecutable(
        service: KotlinToolchainService<*>,
        legacyExecutable: Provider<String>,
        request: Provider<ToolchainRequest>,
    ): String = service.resolveToolchainExecutable(toolName, isDisabled(service), legacyExecutable, request)

    private fun registerFromGradleProperties(project: Project): Provider<out S> {
        val variant = when (properties.mode(project)) {
            KotlinToolchainMode.DOWNLOAD -> downloaded
            KotlinToolchainMode.PREINSTALLED -> preInstalled
            KotlinToolchainMode.DISABLE -> disabled
        }
        return register(project.gradle, variant.implementation) { parameters ->
            variant.parameters.applyGradleProperties(parameters, project, properties)
        }
    }

    private fun findRegistered(gradle: Gradle): Provider<out S>? = gradle.findToolchainService(serviceName)

    private fun register(
        gradle: Gradle,
        implementation: Class<out KotlinToolchainService<*>>,
        configure: (KotlinToolchainService.Parameters) -> Unit,
    ): Provider<out S> = gradle.registerToolchainService(serviceName, implementation, configure)
}

/**
 * The name the toolchain service of [serviceType] is registered under.
 *
 * It includes the class loader hash, so that several Kotlin Gradle Plugin versions loaded in the same build
 * do not clash on the same registration.
 */
internal fun toolchainServiceName(serviceType: Class<out KotlinToolchainService<*>>): String =
    "${serviceType.name}_${serviceType.classLoader.hashCode()}"

/**
 * Returns the toolchain service registered for the build under [serviceName], if any.
 */
@Suppress("UNCHECKED_CAST")
internal fun <S : KotlinToolchainService<*>> Gradle.findToolchainService(serviceName: String): Provider<out S>? =
    sharedServices.registrations.findByName(serviceName)?.service as Provider<out S>?

/**
 * Registers [implementation] as the toolchain service of the build under [serviceName], unless one is already registered.
 */
@Suppress("UNCHECKED_CAST")
internal fun <S : KotlinToolchainService<*>, P : KotlinToolchainService.Parameters> Gradle.registerToolchainService(
    serviceName: String,
    implementation: Class<out KotlinToolchainService<*>>,
    configure: (P) -> Unit,
): Provider<out S> = sharedServices.registerIfAbsent(
    serviceName,
    implementation as Class<BuildService<BuildServiceParameters>>,
) { spec ->
    configure(spec.parameters as P)
} as Provider<out S>

/**
 * The default request for a tool - [defaultVersion] on the platform of the machine running the build.
 *
 * The returned provider intentionally captures only the object factory and the version, and not the project,
 * so that it stays compatible with the configuration cache.
 */
internal fun defaultToolchainRequest(project: Project, defaultVersion: String): Provider<ToolchainRequest> {
    val objects = project.objects
    val version = ToolchainVersion(defaultVersion)
    return project.providers.detectToolchainPlatform().map { platform ->
        objects.newInstance<ToolchainRequest>().also {
            it.version.convention(version)
            it.platform.convention(platform)
        }
    }
}

/**
 * Returns the executable provisioned by this service, or [legacyExecutable] when the toolchain is [disabled].
 *
 * Provisions the tool, so it must only be called at execution time.
 */
internal fun KotlinToolchainService<*>.resolveToolchainExecutable(
    toolName: String,
    disabled: Boolean,
    legacyExecutable: Provider<String>,
    request: Provider<ToolchainRequest>,
): String = if (disabled) {
    legacyExecutable.get()
} else {
    request(request.get()).get().executable.orNull ?: error("$toolName executable should be provisioned")
}
