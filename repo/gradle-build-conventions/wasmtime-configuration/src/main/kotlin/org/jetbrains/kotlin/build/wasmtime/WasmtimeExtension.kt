/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.build.wasmtime

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.typeOf
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimeEnvSpec

abstract class WasmtimeExtension(
    private val project: Project,
    private val wasmtimeEnvSpec: WasmtimeEnvSpec,
) {
    val wasmtimeVersion: String
        get() = project.extensions.getByType(typeOf<VersionCatalogsExtension>())
            .named("libs")
            .findVersion("wasmtime")
            .get().requiredVersion

    val wasmtimeExecutablePath: Provider<String> = wasmtimeEnvSpec.executable.also {
        project.extra["wasm.engine.path.Wasmtime"] = it
    }

    fun Test.setupWasmtime() {
        with(wasmtimeEnvSpec) {
            dependsOn(project.wasmtimeSetupTaskProvider)
        }

        val wasmtimeExecutablePath = wasmtimeExecutablePath

        inputs.property("propertyName", "wasm.engine.path.Wasmtime")
        inputs.property("destinationPath", wasmtimeExecutablePath)

        doFirst {
            systemProperty("wasm.engine.path.Wasmtime", wasmtimeExecutablePath.get())
        }
    }
}
