/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies

class TasksRequirements {
    val byTask = mutableMapOf<RequiresNpmDependencies, Set<RequiredKotlinJsDependency>>()
    private val byCompilation = mutableMapOf<KotlinJsCompilation, MutableList<RequiresNpmDependencies>>()

    fun getTaskRequirements(compilation: KotlinJsCompilation): Collection<RequiresNpmDependencies> =
        byCompilation[compilation] ?: listOf()

    fun addTaskRequirements(task: RequiresNpmDependencies) {
        val requirements = task.requiredNpmDependencies

        byTask[task] = requirements

        byCompilation
            .getOrPut(task.compilation) { mutableListOf() }
            .add(task)
    }
}