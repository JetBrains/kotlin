/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinCompileCommonConfig
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateTask

internal class KotlinCommonSourceSetProcessor(
    compilation: KotlinCompilationProjection,
    tasksProvider: KotlinTasksProvider
) : KotlinSourceSetProcessor<KotlinCompileCommon>(
    tasksProvider, taskDescription = "Compiles the kotlin sources in $compilation to Metadata.", kotlinCompilation = compilation
) {
    override fun doTargetSpecificProcessing() {
        project.tasks.named(compilationProjection.compileAllTaskName).dependsOn(kotlinTask)
        // can be missing (e.g. in case of tests)
        if (compilationProjection.isMain) {
            compilationProjection.tcsOrNull?.compilation?.target?.let { target ->
                project.locateTask<Task>(target.artifactsTaskName)?.dependsOn(kotlinTask)
            }
        }

        project.whenEvaluated {
            val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project)
            /* Not supported in KPM yet */
            compilationProjection.tcsOrNull?.compilation?.let { compilation ->
                subpluginEnvironment.addSubpluginOptions(project, compilation)
            }
        }
    }

    override fun doRegisterTask(project: Project, taskName: String): TaskProvider<out KotlinCompileCommon> {
        val configAction = KotlinCompileCommonConfig(compilationProjection)
        applyStandardTaskConfiguration(configAction)
        return tasksProvider.registerKotlinCommonTask(
            project,
            taskName,
            compilationProjection.compilerOptions.options as KotlinMultiplatformCommonCompilerOptions,
            configAction
        )
    }
}