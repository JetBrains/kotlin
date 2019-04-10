/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson

open class NodeJsExtension(val project: Project) {
    internal val packageJsonHandlers = mutableListOf<PackageJson.() -> Unit>()

    fun packageJson(handler: PackageJson.() -> Unit) {
        packageJsonHandlers.add(handler)
    }

    companion object {
        operator fun get(project: Project): NodeJsExtension {
            NodeJsPlugin[project.rootProject]
            return project.extensions.getByName(NodeJsRootExtension.NODE_JS) as NodeJsExtension
        }
    }
}