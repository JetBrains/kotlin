/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolved

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies

/**
 * Info about NPM projects inside particular gradle [project].
 */
class KotlinProjectNpmResolution(
    val project: String,
    val npmProjects: List<KotlinCompilationNpmResolution>,
    val taskRequirements: Map<RequiresNpmDependencies, Collection<RequiredKotlinJsDependency>>
) {
    val npmProjectsByNpmDependency: Map<NpmDependency, KotlinCompilationNpmResolution> by lazy {
        mutableMapOf<NpmDependency, KotlinCompilationNpmResolution>().also { result ->
            npmProjects.forEach { npmPackage ->
                npmPackage.externalNpmDependencies.forEach { npmDependency ->
                    result[npmDependency] = npmPackage
                }
            }
        }
    }

    val byCompilation by lazy { npmProjects.associateBy { it.npmProject.compilationName } }

    operator fun get(compilationName: String): KotlinCompilationNpmResolution {
//        check(compilation.target.project.path == project)
        return byCompilation.getValue(compilationName)
    }

    companion object {
        fun empty(project: String) = KotlinProjectNpmResolution(project, listOf(), mapOf())
    }
}