/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.nodejs

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.web.nodejs.CommonNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.web.nodejs.NodeJsRootPluginApplier
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware

/**
 * Represents the root plugin class for configuring and applying Node.js-related functionality specifically tailored for WebAssembly (Wasm) projects.
 * This plugin is responsible for setting up Node.js as a runtime or development environment for Wasm projects, leveraging specific tools
 * and extensions required for Wasm builds.
 *
 * This plugin extends from the `CommonNodeJsRootPlugin` interface, providing Wasm-specific customizations for Node.js integration.
 *
 * This class manages:
 * - Applying specific Node.js configuration tailored for Wasm projects.
 * - Registering Wasm-specific Node.js root extensions, like `WasmNodeJsRootExtension` and `WasmNpmExtension`.
 * - Configuring dependencies and lockfile management for Wasm project setups.
 *
 */
abstract class WasmNodeJsRootPlugin internal constructor() : CommonNodeJsRootPlugin {

    override fun apply(target: Project) {
        val rootDirectoryName = WasmPlatformDisambiguate.platformDisambiguate
        NodeJsRootPluginApplier(
            platformDisambiguate = WasmPlatformDisambiguate,
            nodeJsRootKlass = WasmNodeJsRootExtension::class,
            nodeJsRootName = WasmNodeJsRootExtension.EXTENSION_NAME,
            npmKlass = WasmNpmExtension::class,
            npmName = WasmNpmExtension.Companion.EXTENSION_NAME,
            rootDirectoryName = rootDirectoryName,
            lockFileDirectory = { it.dir(LockCopyTask.Companion.KOTLIN_JS_STORE).dir(rootDirectoryName) },
            singleNodeJsPluginApply = { WasmNodeJsPlugin.apply(it) },
            yarnPlugin = WasmYarnPlugin::class,
            platformType = KotlinPlatformType.wasm,
        ).apply(target)
    }

    companion object : HasPlatformDisambiguate by WasmPlatformDisambiguate {
        fun apply(rootProject: Project): WasmNodeJsRootExtension {
            check(rootProject == rootProject.rootProject)
            rootProject.plugins.apply(WasmNodeJsRootPlugin::class.java)
            return rootProject.extensions.getByName(WasmNodeJsRootExtension.EXTENSION_NAME) as WasmNodeJsRootExtension
        }

        val Project.kotlinNodeJsRootExtension: WasmNodeJsRootExtension
            get() = extensions.getByName(WasmNodeJsRootExtension.EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

        val Project.kotlinNpmResolutionManager: Provider<KotlinNpmResolutionManager>
            get() {
                return project.gradle.sharedServices.registerIfAbsent(
                    extensionName(KotlinNpmResolutionManager::class.java.name),
                    KotlinNpmResolutionManager::class.java
                ) {
                    error("Must be already registered")
                }
            }
    }
}