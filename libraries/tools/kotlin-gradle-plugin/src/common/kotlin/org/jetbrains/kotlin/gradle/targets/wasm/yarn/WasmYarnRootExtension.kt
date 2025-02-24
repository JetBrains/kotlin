/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.yarn

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootExtension

abstract class WasmYarnRootExtension internal constructor(
    project: Project,
    nodeJsRoot: WasmNodeJsRootExtension,
    yarnSpec: WasmYarnRootEnvSpec,
    objects: ObjectFactory,
    execOps: ExecOperations,
) : BaseYarnRootExtension(
    project = project,
    nodeJsRoot = nodeJsRoot,
    yarnSpec = yarnSpec,
    objects = objects,
    execOps = execOps,
) {
    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        val YARN: String
            get() = extensionName("kotlinYarn")

        operator fun get(project: Project): WasmYarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(YarnPlugin::class.java)
            return rootProject.extensions.getByName(YARN) as WasmYarnRootExtension
        }
    }
}
