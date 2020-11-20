/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import java.io.File

class Yarn : NpmApi {
    private val yarnWorkspaces = YarnWorkspaces()
    private val yarnSimple = YarnSimple()

    private fun getDelegate(project: Project): NpmApi =
        if (project.yarn.useWorkspaces) yarnWorkspaces
        else yarnSimple

    override fun setup(project: Project) =
        getDelegate(project.rootProject).setup(project)

    override fun resolveProject(resolvedNpmProject: KotlinCompilationNpmResolution) =
        getDelegate(resolvedNpmProject.project).resolveProject(resolvedNpmProject)

    override fun preparedFiles(project: Project): Collection<File> =
        getDelegate(project).preparedFiles(project)

    override fun prepareRootProject(
        rootProject: Project,
        subProjects: Collection<KotlinCompilationNpmResolution>,
        resolutions: Map<String, String>
    ) = getDelegate(rootProject.project)
        .prepareRootProject(
            rootProject,
            subProjects,
            resolutions
        )

    override fun resolveRootProject(
        rootProject: Project,
        npmProjects: Collection<KotlinCompilationNpmResolution>,
        skipExecution: Boolean,
        cliArgs: List<String>
    ) {
        getDelegate(rootProject.project)
            .resolveRootProject(
                rootProject,
                npmProjects,
                skipExecution,
                cliArgs
            )
    }

    override fun resolveDependency(
        npmResolution: KotlinCompilationNpmResolution,
        dependency: NpmDependency,
        transitive: Boolean
    ) = getDelegate(npmResolution.project)
        .resolveDependency(
            npmResolution,
            dependency,
            transitive
        )
}