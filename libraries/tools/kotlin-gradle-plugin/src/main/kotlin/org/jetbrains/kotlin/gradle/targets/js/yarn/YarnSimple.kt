/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJsonUpToDateCheck
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import java.io.File

class YarnSimple : YarnBasics() {
    override fun resolveProject(resolvedNpmProject: KotlinCompilationNpmResolution) {
        setup(resolvedNpmProject.project.rootProject)

        val project = resolvedNpmProject.project

        PackageJsonUpToDateCheck(resolvedNpmProject.npmProject).updateIfNeeded {
            yarnExec(
                project,
                resolvedNpmProject.npmProject.dir,
                NpmApi.resolveOperationDescription("yarn for ${project.path}"),
                emptyList()
            )
            yarnLockReadTransitiveDependencies(resolvedNpmProject.npmProject.dir, resolvedNpmProject.externalNpmDependencies)
        }
    }

    override fun preparedFiles(project: Project): Collection<File> =
        emptyList()

    override fun prepareRootProject(
        rootProject: Project,
        subProjects: Collection<KotlinCompilationNpmResolution>,
        resolutions: Map<String, String>
    ) = Unit

    override fun resolveRootProject(
        rootProject: Project,
        npmProjects: Collection<KotlinCompilationNpmResolution>,
        skipExecution: Boolean,
        cliArgs: List<String>
    ) = Unit
}