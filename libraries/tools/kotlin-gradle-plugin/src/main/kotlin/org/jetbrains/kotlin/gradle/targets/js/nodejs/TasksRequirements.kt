/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies

class TasksRequirements {
    private val _byTask = mutableMapOf<RequiresNpmDependencies, Set<RequiredKotlinJsDependency>>()
    private val byCompilation = mutableMapOf<KotlinJsCompilation, MutableSet<NpmDependency>>()

    val byTask: Map<RequiresNpmDependencies, Set<RequiredKotlinJsDependency>>
        get() = _byTask

    fun getCompilationNpmRequirements(compilation: KotlinJsCompilation): Set<NpmDependency> =
        byCompilation[compilation]
            ?: setOf()

    fun addTaskRequirements(task: RequiresNpmDependencies) {
        val requirements = task.requiredNpmDependencies

        _byTask[task] = requirements

        val requiredNpmDependencies = requirements
            .asSequence()
            .map { it.createDependency(task.compilation.target.project) }
            .filterIsInstance<NpmDependency>()
            .toMutableSet()

        val compilation = task.compilation
        if (compilation in byCompilation) {
            byCompilation[compilation]!!.addAll(requiredNpmDependencies)
        } else {
            byCompilation[compilation] = requiredNpmDependencies
        }
    }
}