/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.AbstractNpmExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguate

open class WasmNpmExtension(
    project: Project,
    nodeJsRoot: WasmNodeJsRootExtension,
) : AbstractNpmExtension(
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