/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.spec

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator

@ExperimentalWasmDsl
abstract class SpecPlugin internal constructor() : Plugin<Project> {

    override fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        project.plugins.apply(BasePlugin::class.java)

        project.extensions.create(
            SpecEnvSpec.EXTENSION_NAME,
            SpecEnvSpec::class.java,
        )
    }

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        const val TASKS_GROUP_NAME: String = "specBLABLABLA"

        internal fun applyWithSpecEnv(project: Project): SpecEnvSpec {
            project.plugins.apply(SpecPlugin::class.java)
            return project.extensions.getByName(
                SpecEnvSpec.EXTENSION_NAME
            ) as SpecEnvSpec
        }
    }
}