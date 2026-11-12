/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("INVISIBLE_REFERENCE")

package org.jetbrains.kotlin.build.wasmtools

import SystemPropertyClasspathProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsRequest
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsVersion
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain.detectWasmToolsPlatform

/**
 * Provisions [wasm-tools](https://github.com/bytecodealliance/wasm-tools) from the `wasmToolsDistributions()` repository
 * and passes it to tests as the `wasm.tools.path` system property.
 */
abstract class WasmToolsExtension(private val project: Project) {
    val wasmToolsVersion: String
        get() = project.extensions.getByType<VersionCatalogsExtension>()
            .named("libs")
            .findVersion("wasmTools")
            .get().requiredVersion

    @OptIn(ExperimentalWasmDsl::class)
    fun Test.setupWasmTools() {
        val wasmToolsBuildService: Provider<WasmToolsToolchainService<*>> =
            project.gradle.sharedServices.registrations.findByName(org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain.wasmToolsServiceName)
                ?.let {
                    @Suppress("UNCHECKED_CAST")
                    it.service as Provider<WasmToolsToolchainService<*>>
                } ?: error("...")

        usesService(wasmToolsBuildService)

        val request = project.objects.newInstance<WasmToolsRequest>().also {
            it.version.set(WasmToolsVersion(wasmToolsVersion))
            it.platform.set(project.providers.detectWasmToolsPlatform())
        }

        jvmArgumentProviders += project.objects.newInstance<SystemPropertyClasspathProvider>().apply {
            println("HELLO")
            println(wasmToolsBuildService.flatMap {
                it.request(request)
                    .flatMap { it.executable }
            }.get())

            classpath.from(
                wasmToolsBuildService.flatMap {
                    it.request(request)
                        .flatMap { it.executable }
                }
            )
            property.set("wasm.tools.path")
        }
    }
}
