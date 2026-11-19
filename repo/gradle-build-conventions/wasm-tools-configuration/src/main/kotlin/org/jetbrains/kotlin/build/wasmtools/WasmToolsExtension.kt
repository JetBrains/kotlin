/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.wasmtools

import SystemPropertyClasspathProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsRequest
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsVersion

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
    val provisionerTask: TaskProvider<WasmToolsServiceProvisioner> =
        project.tasks.register(
            "wasmToolsServiceProvisioner",
            WasmToolsServiceProvisioner::class.java
        ) {
            description = "Task to provision the WasmTools toolchain service. Necessary only while there is no proper way to do so. " +
                    "See https://youtrack.jetbrains.com/issue/KT-91863 for more details."

            val wasmToolsRequest = project.objects.newInstance<WasmToolsRequest>().also {
                it.version.convention(WasmToolsVersion(wasmToolsVersion))
                it.platform.convention(project.providers.detectWasmToolsPlatform())
            }

            this.wasmToolsRequest.set(wasmToolsRequest)
        }

    fun Test.setupWasmTools() {
        jvmArgumentProviders += project.objects.newInstance<SystemPropertyClasspathProvider>().apply {
            classpath.from(
                provisionerTask.flatMap {
                    it.executable
                }
            )
            property.set("wasm.tools.path")
        }
    }
}
