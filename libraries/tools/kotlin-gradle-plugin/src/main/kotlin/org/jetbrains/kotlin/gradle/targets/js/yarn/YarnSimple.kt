/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectPackage

object YarnSimple : YarnBasics() {
    override fun resolveProject(resolvedNpmProject: NpmProjectPackage) {
        setup(resolvedNpmProject.project.rootProject)

        val project = resolvedNpmProject.project

        YarnUpToDateCheck(resolvedNpmProject.npmProject).updateIfNeeded {
            yarnExec(project, resolvedNpmProject.npmProject.dir, NpmApi.resolveOperationDescription("yarn for ${project.path}"))
            yarnLockReadTransitiveDependencies(resolvedNpmProject.npmProject.dir, resolvedNpmProject.npmDependencies)
        }
    }

    override fun resolveRootProject(rootProject: Project, subProjects: MutableList<NpmProjectPackage>) = Unit
}