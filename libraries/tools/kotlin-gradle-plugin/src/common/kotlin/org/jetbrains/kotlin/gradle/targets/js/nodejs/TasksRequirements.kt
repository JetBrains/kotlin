/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyDeclaration
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.toDeclaration
import java.io.Serializable

class TasksRequirements : Serializable {
    private val _byTask = mutableMapOf<String, Set<RequiredKotlinJsDependency>>()
    private val byCompilation = mutableMapOf<String, MutableSet<NpmDependencyDeclaration>>()

    val byTask: Map<String, Set<RequiredKotlinJsDependency>>
        get() = _byTask

    internal fun getCompilationNpmRequirements(projectPath: String, compilationName: String): Set<NpmDependencyDeclaration> =
        byCompilation["$projectPath:$compilationName"]
            ?: setOf()

    fun addTaskRequirements(task: RequiresNpmDependencies) {
        val requirements = task.requiredNpmDependencies

        _byTask[task.getPath()] = requirements

        val requiredNpmDependencies = requirements
            .asSequence()
            .map { it.createDependency(task.compilation.target.project.objects) }
            .filterIsInstance<NpmDependency>()
            .toMutableSet()

        val projectPath = task.compilation.target.project.path
        val compilationPath = "$projectPath:${task.compilation.disambiguatedName}"
        if (compilationPath in byCompilation) {
            byCompilation[compilationPath]!!.addAll(requiredNpmDependencies.map { it.toDeclaration() })
        } else {
            byCompilation[compilationPath] = requiredNpmDependencies.map { it.toDeclaration() }.toMutableSet()
        }
    }
}