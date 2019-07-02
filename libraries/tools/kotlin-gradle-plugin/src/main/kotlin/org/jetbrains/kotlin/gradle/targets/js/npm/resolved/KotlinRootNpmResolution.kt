/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolved

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

class KotlinRootNpmResolution(
    val rootProject: Project,
    private val projects: Map<Project, KotlinProjectNpmResolution>
) : NodeJsRootExtension.ResolutionStateData {
    operator fun get(project: Project) = projects[project] ?: KotlinProjectNpmResolution.empty(project)

    override val compilations: Collection<KotlinJsCompilation>
        get() = projects.values.flatMap { it.npmProjects.map { it.npmProject.compilation } }
}