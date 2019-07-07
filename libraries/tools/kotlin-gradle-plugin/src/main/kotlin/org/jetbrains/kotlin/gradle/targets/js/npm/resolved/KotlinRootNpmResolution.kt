/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolved

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager

class KotlinRootNpmResolution(
    val rootProject: Project,
    private val projects: Map<Project, KotlinProjectNpmResolution>
) {
    operator fun get(project: Project) = projects[project] ?: KotlinProjectNpmResolution.empty(project)
}