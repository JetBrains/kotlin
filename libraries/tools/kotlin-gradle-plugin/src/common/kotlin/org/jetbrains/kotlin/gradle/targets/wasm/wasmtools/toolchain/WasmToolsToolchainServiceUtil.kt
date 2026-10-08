/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.statistics.WasmToolsToolchainServiceMetrics
import org.jetbrains.kotlin.gradle.tasks.wasmtools.UsesWasmToolsToolchainService
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.newInstance

private val serviceClass = WasmToolsToolchainService::class.java
internal val wasmToolsServiceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"

internal fun <P : WasmToolsToolchainService.Parameters, T : WasmToolsToolchainService<P>> registerWasmToolsToolchainServiceIfAbsent(
    gradle: Gradle,
    wasmToolsToolchainClass: Class<T>,
    configureBuildServiceParameters: (P) -> Unit,
): Provider<out WasmToolsToolchainService<out WasmToolsToolchainService.Parameters>> {
    gradle.sharedServices.registrations.findByName(wasmToolsServiceName)?.let {
        @Suppress("UNCHECKED_CAST")
        return it.service as Provider<WasmToolsToolchainService<out WasmToolsToolchainService.Parameters>>
    }

    @Suppress("UNCHECKED_CAST")
    return when (wasmToolsToolchainClass) {
        DefaultWasmToolsToolchainService::class.java -> DefaultWasmToolsToolchainServiceImpl.registerIfAbsent(
            gradle,
            configureBuildServiceParameters as (DefaultWasmToolsToolchainService.Parameters) -> Unit
        )
        PreInstalledWasmToolsToolchainService::class.java -> PreInstalledWasmToolsToolchainServiceImpl.registerIfAbsent(
            gradle,
            configureBuildServiceParameters as (PreInstalledWasmToolsToolchainService.Parameters) -> Unit
        )
        DisabledWasmToolsToolchainService::class.java -> DisabledWasmToolsToolchainServiceImpl.registerIfAbsent(gradle)
        else -> gradle.sharedServices.registerIfAbsent(wasmToolsServiceName, wasmToolsToolchainClass) { spec ->
            configureBuildServiceParameters(spec.parameters)
        }
    }
}

private fun registerIfAbsent(
    project: Project,
): Provider<out WasmToolsToolchainService<out WasmToolsToolchainService.Parameters>> {
    project.gradle.sharedServices.registrations.findByName(wasmToolsServiceName)?.let {
        @Suppress("UNCHECKED_CAST")
        return it.service as Provider<WasmToolsToolchainService<out WasmToolsToolchainService.Parameters>>
    }

    return when (project.kotlinPropertiesProvider.wasmToolsToolchainMode) {
        WasmToolsToolchainMode.DOWNLOAD -> DefaultWasmToolsToolchainServiceImpl.registerIfAbsent(project)
        WasmToolsToolchainMode.PREINSTALLED -> PreInstalledWasmToolsToolchainServiceImpl.registerIfAbsent(project)
        WasmToolsToolchainMode.DISABLE -> DisabledWasmToolsToolchainServiceImpl.registerIfAbsent(project.gradle)
    }
}

/**
 * Registers the [WasmToolsToolchainService] selected for the build, and makes every
 * [UsesWasmToolsToolchainService] task in [project] use it.
 */
internal fun registerWasmToolsToolchainServiceIfAbsent(
    project: Project,
): Provider<out WasmToolsToolchainService<out WasmToolsToolchainService.Parameters>> {
    val serviceProvider = registerIfAbsent(project)

    SingleActionPerProject.run(project, UsesWasmToolsToolchainService::class.java.name) {
        project.tasks.withType<UsesWasmToolsToolchainService>().configureEach { task ->
            task.wasmToolsToolchainService.value(serviceProvider).disallowChanges()
            task.usesService(serviceProvider)
        }
        WasmToolsToolchainServiceMetrics.collectServiceCreated(project, serviceProvider)
    }
    return serviceProvider
}

internal enum class WasmToolsToolchainMode {

    /**
     * wasm-tools distributions are downloaded and installed by [DefaultWasmToolsToolchainServiceImpl].
     */
    DOWNLOAD,

    /**
     * A pre-installed wasm-tools is used by [PreInstalledWasmToolsToolchainService], and nothing is downloaded.
     */
    PREINSTALLED,
    DISABLE,
    ;
}

internal const val DEFAULT_WASM_TOOLS_VERSION = "1.259.0"

internal fun Project.requestDefaultWasmTools(): Provider<WasmToolsRequest> = provider {
    objects.newInstance<WasmToolsRequest>().also {
        it.version.convention(WasmToolsVersion(DEFAULT_WASM_TOOLS_VERSION))
        it.platform.convention(providers.detectWasmToolsPlatform())
    }
}
