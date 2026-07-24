/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION_ERROR")

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.DeprecatedKotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect

internal class BuildNeededDependentTaskWiringProviderG95 : BuildNeededDependentTasksWiringProvider {
    override fun wireSideEffect(): KotlinTargetSideEffect = KotlinTargetSideEffect { target ->
        val project = target.project

        val buildNeeded = project.tasks.named(JavaBasePlugin.BUILD_NEEDED_TASK_NAME)
        val buildDependent = project.tasks.named(JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME)

        val testCompilation = target.compilations.findByName(KotlinCompilation.TEST_COMPILATION_NAME)
        if (testCompilation is DeprecatedKotlinCompilationToRunnableFiles) {
            addDependsOnTaskInOtherProjects(project, buildNeeded, true, testCompilation.runtimeDependencyConfigurationName)
            addDependsOnTaskInOtherProjects(project, buildDependent, false, testCompilation.runtimeDependencyConfigurationName)
        }
    }

    private fun addDependsOnTaskInOtherProjects(
        project: Project,
        taskProvider: TaskProvider<*>,
        useDependedOn: Boolean,
        configurationName: String,
    ) {
        val configuration = project.configurations.getByName(configurationName)
        taskProvider.configure { task ->
            task.dependsOn(configuration.getTaskDependencyFromProjectDependency(useDependedOn, taskProvider.name))
        }
    }


    class Factory : BuildNeededDependentTasksWiringProvider.Factory {
        override fun getInstance() = BuildNeededDependentTaskWiringProviderG95()
    }
}
