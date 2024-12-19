/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.BaseNpmExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguate

/**
 * An extension for configuring NPM-related tasks and properties specifically for WebAssembly (Wasm) projects.
 * This class is built on top of the base NPM configuration provided by `BaseNpmExtension` and extends it with Wasm-specific logic.
 *
 * This extension integrates with the Gradle plugin system and is automatically configured when applied to the root project.
 * It provides mechanisms for managing NPM dependencies, command execution, and environment configurations for Wasm projects.
 *
 * @property EXTENSION_NAME The name of this extension within the Gradle project configuration, used for retrieving the extension instance.
 */
abstract class WasmNpmExtension internal constructor(
    project: Project,
    nodeJsRoot: WasmNodeJsRootExtension,
) : BaseNpmExtension(
    project,
    nodeJsRoot
) {
    companion object : HasPlatformDisambiguate by WasmPlatformDisambiguate {
        val EXTENSION_NAME: String
            get() = extensionName("kotlinNpm")

        operator fun get(project: Project): WasmNpmExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(NodeJsRootPlugin::class.java)
            return rootProject.extensions.getByName(EXTENSION_NAME) as WasmNpmExtension
        }
    }
}