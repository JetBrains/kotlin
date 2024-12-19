/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.web.npm.CommonNpmResolverPlugin
import org.jetbrains.kotlin.gradle.targets.web.npm.NpmResolverPluginApplier

/**
 * Class for the WebAssembly NPM Resolver Plugin.
 *
 * This plugin is used to configure and apply NPM dependency resolution logic specifically
 * for WebAssembly (Wasm) projects. It integrates with Gradle to ensure the proper handling
 * of NPM dependencies required for Wasm-based builds and tasks.
 */
abstract class WasmNpmResolverPlugin internal constructor() : CommonNpmResolverPlugin {
    override fun apply(project: Project) {
        NpmResolverPluginApplier(
            { WasmNodeJsRootPlugin.Companion.apply(project.rootProject) },
            { WasmNodeJsPlugin.Companion.apply(project) },
        ).apply(project)
    }

    companion object {
        fun apply(project: Project) {
            project.plugins.apply(WasmNpmResolverPlugin::class.java)
        }
    }
}