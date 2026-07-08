/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.web.npm.CommonNpmResolverPlugin
import org.jetbrains.kotlin.gradle.targets.web.npm.NpmResolverPluginApplier
import org.jetbrains.kotlin.gradle.utils.withType
import org.jetbrains.kotlin.platform.wasm.WasmTarget

/**
 * Class for the WebAssembly NPM Resolver Plugin.
 *
 * This plugin is used to configure and apply NPM dependency resolution logic specifically
 * for WebAssembly (Wasm) projects. It integrates with Gradle to ensure the proper handling
 * of NPM dependencies required for Wasm-based builds and tasks.
 */
abstract class WasmNpmResolverPlugin internal constructor() : CommonNpmResolverPlugin {
    override fun apply(project: Project) {
        val wasmNodeJsRootPlugin = WasmNodeJsRootPlugin.apply(project.rootProject)

        val applier = NpmResolverPluginApplier(
            { wasmNodeJsRootPlugin },
            { WasmNodeJsPlugin.apply(project) },
            /**
             * For wasm we don't need to add NPM dependencies
             * We install all npm dependencies in [org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinToolingSetupTask]
             */
            { false },
        )
        applier.apply(project)

        applier.configureNpmDependencyTaskInputs(project) { task ->
            when (task.compilation.wasmTarget) {
                WasmTarget.JS -> true
                WasmTarget.WASI -> {
                    // wasi doesn't use npm dependencies
                    false
                }
                null -> {
                    // Kotlin/JS targets are handled by NpmResolverPluginApplier
                    false
                }
            }
        }

        project.tasks.withType<RequiresNpmDependenciesTask>().configureEach { task ->
            task.mustRunAfter(wasmNodeJsRootPlugin.toolingInstallTaskProvider)
        }
    }

    companion object {
        fun apply(project: Project) {
            project.plugins.apply(WasmNpmResolverPlugin::class.java)
        }
    }
}
