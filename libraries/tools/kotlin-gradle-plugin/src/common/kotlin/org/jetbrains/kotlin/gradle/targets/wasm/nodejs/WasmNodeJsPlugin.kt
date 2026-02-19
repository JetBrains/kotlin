/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.nodejs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.nodejs.CommonNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.web.nodejs.NodeJsPluginApplier
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware

/**
 * Represents a Node.js plugin for WebAssembly (Wasm) projects in a Gradle-based build environment.
 *
 * This plugin provides specific configurations and setup required for enabling Node.js support
 * in projects targeting Wasm. The WasmNodeJsPlugin interacts with the project's extensions
 * and tasks by applying appropriate Node.js environment specifications and ensuring the correct
 * version of Node.js is installed and used.
 *
 * The plugin extends the `CommonNodeJsPlugin` interface, which provides a shared structure for
 * plugins related to Node.js environments, and leverages the `NodeJsPluginApplier` class to
 * centralize the application process.
 *
 */
abstract class WasmNodeJsPlugin internal constructor(): CommonNodeJsPlugin {
    override fun apply(target: Project) {
        NodeJsPluginApplier(
            platformDisambiguate = WasmPlatformDisambiguator,
            nodeJsEnvSpecKlass = WasmNodeJsEnvSpec::class,
            nodeJsEnvSpecName = WasmNodeJsEnvSpec.EXTENSION_NAME,
            nodeJsRootApply = { WasmNodeJsRootPlugin.Companion.apply(it) }
        ).apply(target)
    }

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        fun apply(project: Project): WasmNodeJsEnvSpec {
            project.plugins.apply(WasmNodeJsPlugin::class.java)
            return project.extensions.getByName(WasmNodeJsEnvSpec.EXTENSION_NAME) as WasmNodeJsEnvSpec
        }

        val Project.kotlinNodeJsEnvSpec: WasmNodeJsEnvSpec
            get() = extensions.getByName(WasmNodeJsEnvSpec.EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()
    }
}