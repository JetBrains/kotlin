/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project

class NodeJsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val root = NodeJsRootPlugin.apply(target.rootProject)

        if (target != target.rootProject) {
            target.extensions.create(NodeJsRootExtension.EXTENSION_NAME, NodeJsExtension::class.java, target)
        }

        root.requireResolver().addProject(target)
    }

    companion object {
        fun apply(project: Project): NodeJsExtension {
            project.plugins.apply(NodeJsPlugin::class.java)
            return project.extensions.getByName(NodeJsRootExtension.EXTENSION_NAME) as NodeJsExtension
        }
    }
}