/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

/**
 * Represents an extension for configuring npm-related tasks and settings in a Gradle project.
 *
 * This class is a part of the Kotlin/JS Gradle Plugin and provides functionality for integrating
 * npm-based dependencies and tooling into a Gradle project.
 *
 * @param project The Gradle [Project] instance to which this extension is applied.
 * @param nodeJsRoot The root extension for configuring Node.js and npm settings.
 */
abstract class NpmExtension internal constructor(
    project: Project,
    nodeJsRoot: NodeJsRootExtension,
    objects: ObjectFactory,
    execOps: ExecOperations,
) : BaseNpmExtension(
    project = project,
    nodeJsRoot = nodeJsRoot,
    objects = objects,
    execOps = execOps,
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
