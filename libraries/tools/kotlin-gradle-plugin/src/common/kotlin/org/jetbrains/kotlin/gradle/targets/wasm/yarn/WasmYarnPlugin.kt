/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPluginApplier
import org.jetbrains.kotlin.gradle.targets.web.yarn.CommonYarnPlugin

/**
 * An abstract class representing the WebAssembly specific implementation of the Yarn plugin.
 * The `WasmYarnPlugin` applies configurations, tasks, and extensions necessary for managing
 * Yarn as a package manager within Wasm-targeted projects using Gradle.
 */
abstract class WasmYarnPlugin internal constructor() : CommonYarnPlugin {
    override fun apply(target: Project) {

        YarnPluginApplier(
            platformDisambiguate = WasmPlatformDisambiguator,
            yarnRootKlass = WasmYarnRootExtension::class,
            yarnRootName = WasmYarnRootExtension.YARN,
            yarnEnvSpecKlass = WasmYarnRootEnvSpec::class,
            yarnEnvSpecName = WasmYarnRootEnvSpec.YARN,
            nodeJsRootApply = { WasmNodeJsRootPlugin.apply(it) },
            nodeJsRootExtension = { it.kotlinNodeJsRootExtension },
            nodeJsEnvSpec = { it.kotlinNodeJsEnvSpec },
            lockFileDirectory = { it.resolve(LockCopyTask.KOTLIN_JS_STORE).resolve(WasmPlatformDisambiguator.platformDisambiguator) },
        ).apply(target)
    }

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        fun apply(project: Project): WasmYarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(WasmYarnPlugin::class.java)
            return rootProject.extensions.getByName(WasmYarnRootExtension.YARN) as WasmYarnRootExtension
        }
    }
}
