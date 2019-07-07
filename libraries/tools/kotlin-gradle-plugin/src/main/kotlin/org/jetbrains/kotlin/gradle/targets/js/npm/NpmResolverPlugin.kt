/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

class NpmResolverPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val project = target
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
        nodeJs.npmResolutionManager.requireConfiguringState().addProject(project)
    }

    companion object {
        fun apply(project: Project) {
            project.plugins.apply(NpmResolverPlugin::class.java)
        }
    }
}