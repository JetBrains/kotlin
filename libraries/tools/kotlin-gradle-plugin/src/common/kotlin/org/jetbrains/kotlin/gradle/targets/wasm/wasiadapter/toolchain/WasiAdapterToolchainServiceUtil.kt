/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.wasm.wasiadapter.toolchain

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.statistics.WasiAdapterToolchainServiceMetrics
import org.jetbrains.kotlin.gradle.tasks.wasiadapter.UsesWasiAdapterToolchainService
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.newInstance

private val serviceClass = WasiAdapterToolchainService::class.java
internal val wasiAdapterServiceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"

internal fun <P : WasiAdapterToolchainService.Parameters, T : WasiAdapterToolchainService<P>> registerWasiAdapterToolchainServiceIfAbsent(
    gradle: Gradle,
    wasiAdapterToolchainClass: Class<T>,
    configureBuildServiceParameters: (P) -> Unit,
): Provider<out WasiAdapterToolchainService<out WasiAdapterToolchainService.Parameters>> {
    gradle.sharedServices.registrations.findByName(wasiAdapterServiceName)?.let {
        @Suppress("UNCHECKED_CAST")
        return it.service as Provider<WasiAdapterToolchainService<out WasiAdapterToolchainService.Parameters>>
    }

    @Suppress("UNCHECKED_CAST")
    return when {
        wasiAdapterToolchainClass == DefaultWasiAdapterToolchainService::class.java || wasiAdapterToolchainClass == DefaultWasiAdapterToolchainServiceImpl::class.java ->
            DefaultWasiAdapterToolchainServiceImpl.registerIfAbsent(
                gradle,
                configureBuildServiceParameters as (DefaultWasiAdapterToolchainService.Parameters) -> Unit
            )
        wasiAdapterToolchainClass == PreInstalledWasiAdapterToolchainService::class.java || wasiAdapterToolchainClass == PreInstalledWasiAdapterToolchainServiceImpl::class.java ->
            PreInstalledWasiAdapterToolchainServiceImpl.registerIfAbsent(
                gradle,
                configureBuildServiceParameters as (PreInstalledWasiAdapterToolchainService.Parameters) -> Unit
            )
        wasiAdapterToolchainClass == DisabledWasiAdapterToolchainService::class.java || wasiAdapterToolchainClass == DisabledWasiAdapterToolchainServiceImpl::class.java ->
            DisabledWasiAdapterToolchainServiceImpl.registerIfAbsent(gradle)
        else -> gradle.sharedServices.registerIfAbsent(wasiAdapterServiceName, wasiAdapterToolchainClass) { spec ->
            configureBuildServiceParameters(spec.parameters)
        }
    }
}

private fun registerIfAbsent(
    project: Project,
): Provider<out WasiAdapterToolchainService<out WasiAdapterToolchainService.Parameters>> {
    project.gradle.sharedServices.registrations.findByName(wasiAdapterServiceName)?.let {
        @Suppress("UNCHECKED_CAST")
        return it.service as Provider<WasiAdapterToolchainService<out WasiAdapterToolchainService.Parameters>>
    }

    return when (project.kotlinPropertiesProvider.wasiAdapterToolchainMode) {
        WasiAdapterToolchainMode.DOWNLOAD -> DefaultWasiAdapterToolchainServiceImpl.registerIfAbsent(project)
        WasiAdapterToolchainMode.PREINSTALLED -> PreInstalledWasiAdapterToolchainServiceImpl.registerIfAbsent(project)
        WasiAdapterToolchainMode.DISABLE -> DisabledWasiAdapterToolchainServiceImpl.registerIfAbsent(project.gradle)
    }
}

/**
 * Registers the [WasiAdapterToolchainService] selected for the build, and makes every
 * [UsesWasiAdapterToolchainService] task in [project] use it.
 */
internal fun registerWasiAdapterToolchainServiceIfAbsent(
    project: Project,
) {
    val serviceProvider = registerIfAbsent(project)

    SingleActionPerProject.run(project, UsesWasiAdapterToolchainService::class.java.name) {
        project.tasks.withType<UsesWasiAdapterToolchainService>().configureEach { task ->
            task.wasiAdapterToolchainService.value(serviceProvider).disallowChanges()
            task.usesService(serviceProvider)
        }
        WasiAdapterToolchainServiceMetrics.collectServiceCreated(project, serviceProvider)
    }
}

internal fun wasiAdapterToolchainServiceProvider(
    project: Project,
): Provider<out WasiAdapterToolchainService<out WasiAdapterToolchainService.Parameters>> = registerIfAbsent(project)

internal enum class WasiAdapterToolchainMode {
    DOWNLOAD,
    PREINSTALLED,
    DISABLE,
}

// Keep in sync with WasmtimePlugin's version.convention("49.0.1")
internal const val DEFAULT_WASI_ADAPTER_VERSION = "49.0.1"

internal fun Project.requestDefaultWasiAdapter(): WasiAdapterRequest =
    objects.newInstance<WasiAdapterRequest>().also {
        it.version.convention(WasiAdapterVersion(DEFAULT_WASI_ADAPTER_VERSION))
        it.flavor.convention(WasiAdapterFlavor.COMMAND)
    }
