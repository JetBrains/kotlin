/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution

object Yarn : NpmApi {
    private fun getDelegate(project: Project): NpmApi =
        if (project.yarn.useWorkspaces) YarnWorkspaces
        else YarnSimple

    override fun setup(project: Project) =
        getDelegate(project.rootProject).setup(project)

    override fun resolveProject(resolvedNpmProject: KotlinCompilationNpmResolution) =
        getDelegate(resolvedNpmProject.project).resolveProject(resolvedNpmProject)

    override fun resolveRootProject(
        rootProject: Project,
        subProjects: Collection<KotlinCompilationNpmResolution>,
        skipExecution: Boolean
    ) = getDelegate(rootProject.project).resolveRootProject(rootProject, subProjects, skipExecution)
}