/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolved

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies

/**
 * Info about NPM projects inside particular gradle [project].
 */
class KotlinProjectNpmResolution(
    val project: Project,
    val npmProjects: List<NpmProjectPackage>,
    val taskRequirements: Map<RequiresNpmDependencies, Collection<RequiredKotlinJsDependency>>
) {
    val npmProjectsByCompilation: Map<KotlinJsCompilation, NpmProjectPackage> = npmProjects.associateBy { it.npmProject.compilation }
    val npmProjectsByNpmDependency: Map<NpmDependency, NpmProjectPackage> =
        mutableMapOf<NpmDependency, NpmProjectPackage>().also { result ->
            npmProjects.forEach { npmPackage ->
                npmPackage.npmDependencies.forEach { npmDependency ->
                    result[npmDependency] = npmPackage
                }
            }
        }

    companion object {
        fun empty(project: Project) = KotlinProjectNpmResolution(project, listOf(), mapOf())
    }
}