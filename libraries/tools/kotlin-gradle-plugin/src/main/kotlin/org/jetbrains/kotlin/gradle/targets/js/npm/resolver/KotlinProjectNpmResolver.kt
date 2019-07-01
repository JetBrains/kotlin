/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinProjectNpmResolution

internal class KotlinProjectNpmResolver(
    val project: Project,
    val resolver: KotlinNpmResolver
) {
    val byCompilation = mutableMapOf<KotlinJsCompilation, KotlinCompilationNpmResolver>()

    private val taskRequirements = mutableMapOf<RequiresNpmDependencies, Collection<RequiredKotlinJsDependency>>()
    val requiredFromTasksByCompilation = mutableMapOf<KotlinJsCompilation, MutableList<RequiresNpmDependencies>>()
    var hasNodeModulesDependentTasks = false
        private set

    init {
        visit()
    }

    fun visit() {
        project.tasks.toList().forEach { task ->
            if (task.enabled && task is RequiresNpmDependencies) {
                addTaskRequirements(task)
            }
        }

        val kotlin = project.kotlinExtensionOrNull

        if (kotlin != null) {
            when (kotlin) {
                is KotlinSingleTargetExtension -> visitTarget(kotlin.target)
                is KotlinMultiplatformExtension -> kotlin.targets.forEach {
                    visitTarget(it)
                }
            }
        }
    }

    private fun visitTarget(target: KotlinTarget) {
        if (target.platformType == KotlinPlatformType.js) {
            target.compilations.toList().forEach { compilation ->
                if (compilation is KotlinJsCompilation) {
                    // compilation may be KotlinWithJavaTarget for old Kotlin2JsPlugin
                    visitCompilation(compilation)
                }
            }
        }
    }

    private fun visitCompilation(compilation: KotlinJsCompilation) {
        byCompilation[compilation] = KotlinCompilationNpmResolver(this, compilation)
    }

    private fun addTaskRequirements(task: RequiresNpmDependencies) {
        if (!hasNodeModulesDependentTasks && task.nodeModulesRequired) {
            hasNodeModulesDependentTasks = true
        }

        val requirements = task.requiredNpmDependencies.toList()

        taskRequirements[task] = requirements

        requiredFromTasksByCompilation
            .getOrPut(task.compilation) { mutableListOf() }
            .add(task)
    }

    fun toResolved() = KotlinProjectNpmResolution(
        project,
        byCompilation.values.map { it.projectPackage },
        taskRequirements
    )
}