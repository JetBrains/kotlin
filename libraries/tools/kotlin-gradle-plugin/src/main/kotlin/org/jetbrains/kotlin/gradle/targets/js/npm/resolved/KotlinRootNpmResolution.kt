/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolved

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.npm.plugins.RootResolverPlugin

internal class KotlinRootNpmResolution(
    val rootProject: Project,
    val projects: Map<Project, KotlinProjectNpmResolution>,
    val plugins: List<RootResolverPlugin>
) {
    operator fun get(project: Project) = projects[project] ?: KotlinProjectNpmResolution.empty(project)
}