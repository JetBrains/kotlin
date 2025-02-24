/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootExtension

open class YarnRootExtension(
    project: Project,
    nodeJsRoot: NodeJsRootExtension,
    yarnSpec: YarnRootEnvSpec,
    objects: ObjectFactory,
    execOps: ExecOperations,
) : BaseYarnRootExtension(
    project = project,
    nodeJsRoot = nodeJsRoot,
    yarnSpec = yarnSpec,
    objects = objects,
    execOps = execOps,
) {
    companion object {
        const val YARN: String = "kotlinYarn"

        operator fun get(project: Project): YarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(YarnPlugin::class.java)
            return rootProject.extensions.getByName(YARN) as YarnRootExtension
        }
    }
}

val Project.yarn: YarnRootExtension
    get() = YarnRootExtension[this]
