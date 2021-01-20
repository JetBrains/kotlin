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

class TasksRequirements {
    private val _byTask = mutableMapOf<RequiresNpmDependencies, Set<RequiredKotlinJsDependency>>()
    private val byCompilation = mutableMapOf<String, MutableSet<NpmDependencyDeclaration>>()

    val byTask: Map<RequiresNpmDependencies, Set<RequiredKotlinJsDependency>>
        get() = _byTask

    internal fun getCompilationNpmRequirements(compilationName: String): Set<NpmDependencyDeclaration> =
        byCompilation[compilationName]
            ?: setOf()

    fun addTaskRequirements(task: RequiresNpmDependencies) {
        val requirements = task.requiredNpmDependencies

        _byTask[task] = requirements

        val requiredNpmDependencies = requirements
            .asSequence()
            .map { it.createDependency(task.compilation.target.project) }
            .filterIsInstance<NpmDependency>()
            .toMutableSet()

        val compilation = task.compilation.name
        if (compilation in byCompilation) {
            byCompilation[compilation]!!.addAll(requiredNpmDependencies.map { it.toDeclaration() })
        } else {
            byCompilation[compilation] = requiredNpmDependencies.map { it.toDeclaration() }.toMutableSet()
        }
    }
}