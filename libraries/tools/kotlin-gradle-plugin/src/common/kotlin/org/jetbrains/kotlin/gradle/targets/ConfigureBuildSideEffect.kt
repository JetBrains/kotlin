/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets

import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.*

@Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
internal val ConfigureBuildSideEffect = KotlinTargetSideEffect { target ->
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
