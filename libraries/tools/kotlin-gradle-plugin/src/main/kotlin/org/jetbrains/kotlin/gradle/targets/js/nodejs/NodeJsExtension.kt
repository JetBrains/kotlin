/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson

open class NodeJsExtension(val project: Project, root: NodeJsRootExtension?) {
    @Suppress("LeakingThis")
    val root: NodeJsRootExtension = root ?: this as NodeJsRootExtension

    internal val packageJsonHandlers = mutableListOf<PackageJson.() -> Unit>()

    val versions = NpmVersions()

    @Suppress("unused")
    fun packageJson(handler: PackageJson.() -> Unit) {
        packageJsonHandlers.add(handler)
    }
}

val Project.nodeJs: NodeJsExtension
    get() {
        val project = this
        project.plugins.apply(NodeJsPlugin::class.java)
        return project.extensions.getByName(NodeJsRootExtension.EXTENSION_NAME) as NodeJsExtension
    }