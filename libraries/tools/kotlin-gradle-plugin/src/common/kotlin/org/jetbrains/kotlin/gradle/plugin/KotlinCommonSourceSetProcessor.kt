/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.CompilerMultiplatformCommonOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.isMainCompilationData
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinCompileCommonConfig
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateTask

internal class KotlinCommonSourceSetProcessor(
    compilation: KotlinCompilationData<*>,
    tasksProvider: KotlinTasksProvider
) : KotlinSourceSetProcessor<KotlinCompileCommon>(
    tasksProvider, taskDescription = "Compiles the kotlin sources in $compilation to Metadata.", kotlinCompilation = compilation
) {
    override fun doTargetSpecificProcessing() {
        project.tasks.named(kotlinCompilation.compileAllTaskName).dependsOn(kotlinTask)
        // can be missing (e.g. in case of tests)
        if ((kotlinCompilation as? AbstractKotlinCompilation<*>)?.isMainCompilationData() == true) {
            project.locateTask<Task>(kotlinCompilation.target.artifactsTaskName)?.dependsOn(kotlinTask)
        }

        if (kotlinCompilation is KotlinCompilation<*>) {
            project.whenEvaluated {
                val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project)
                subpluginEnvironment.addSubpluginOptions(project, kotlinCompilation)
            }
        }
    }

    override fun doRegisterTask(project: Project, taskName: String): TaskProvider<out KotlinCompileCommon> {
        val configAction = KotlinCompileCommonConfig(kotlinCompilation)
        applyStandardTaskConfiguration(configAction)
        return tasksProvider.registerKotlinCommonTask(
            project,
            taskName,
            kotlinCompilation.compilerOptions.options as CompilerMultiplatformCommonOptions,
            configAction
        )
    }
}