/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

open class NpmExtension(
    project: Project,
    nodeJsRoot: NodeJsRootExtension,
) : AbstractNpmExtension(
    project,
    nodeJsRoot
) {
    companion object {
        const val EXTENSION_NAME: String = "kotlinNpm"

        operator fun get(project: Project): NpmExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(NodeJsRootPlugin::class.java)
            return rootProject.extensions.getByName(EXTENSION_NAME) as NpmExtension
        }
    }
}