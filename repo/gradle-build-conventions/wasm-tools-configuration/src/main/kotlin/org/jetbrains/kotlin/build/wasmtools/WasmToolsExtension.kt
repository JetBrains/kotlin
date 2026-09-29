/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.wasmtools

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.typeOf

/**
 * The name of the system property (and Gradle property) through which the `wasm-tools` executable is passed on.
 */
const val WASM_TOOLS_PATH_PROPERTY: String = "wasm.tools.path"

abstract class WasmToolsExtension(
    private val project: Project,
    private val setupTaskProvider: TaskProvider<*>,
    /** Absolute path of the `wasm-tools` executable of the provisioned distribution. */
    val wasmToolsExecutablePath: Provider<String>,
) {
    init {
        project.extra[WASM_TOOLS_PATH_PROPERTY] = wasmToolsExecutablePath
    }

    /**
     * Makes the provisioned `wasm-tools` available to a test JVM as the `wasm.tools.path` system property.
     */
    fun Test.setupWasmTools() {
        dependsOn(setupTaskProvider)

        val wasmToolsExecutablePath = wasmToolsExecutablePath

        inputs.property("propertyName", WASM_TOOLS_PATH_PROPERTY)
        inputs.property("destinationPath", wasmToolsExecutablePath)

        doFirst {
            systemProperty(WASM_TOOLS_PATH_PROPERTY, wasmToolsExecutablePath.get())
        }
    }

    /**
     * Makes the provisioned `wasm-tools` available to any other task; the returned provider must only be queried at
     * execution time.
     */
    fun org.gradle.api.Task.useWasmTools(): Provider<String> {
        dependsOn(setupTaskProvider)
        return wasmToolsExecutablePath
    }

    companion object {
        internal fun version(project: Project): String =
            project.extensions.getByType(typeOf<VersionCatalogsExtension>())
                .named("libs")
                .findVersion("wasmTools")
                .get().requiredVersion
    }
}
