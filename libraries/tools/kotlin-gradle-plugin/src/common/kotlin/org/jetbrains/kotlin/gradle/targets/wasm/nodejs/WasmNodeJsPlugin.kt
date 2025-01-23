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

open class WasmNodeJsPlugin : CommonNodeJsPlugin {
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