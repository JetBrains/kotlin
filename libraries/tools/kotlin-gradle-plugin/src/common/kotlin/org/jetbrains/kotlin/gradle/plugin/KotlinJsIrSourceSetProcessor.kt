/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.configuration.Kotlin2JsCompileConfig
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinJsIrLinkConfig
import org.jetbrains.kotlin.gradle.utils.filesProvider

internal class KotlinJsIrSourceSetProcessor(
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: KotlinCompilationInfo,
) : KotlinSourceSetProcessor<Kotlin2JsCompile>(
    tasksProvider, taskDescription = "Compiles the Kotlin sources in $kotlinCompilation to JavaScript.",
    kotlinCompilation = kotlinCompilation
) {
    override fun doRegisterTask(project: Project, taskName: String): TaskProvider<out Kotlin2JsCompile> {
        val configAction = Kotlin2JsCompileConfig(compilationInfo)
        applyStandardTaskConfiguration(configAction)
        return tasksProvider.registerKotlinJSTask(
            project,
            taskName,
            compilationInfo.compilerOptions.options as KotlinJsCompilerOptions,
            configAction
        )
    }

    override fun doTargetSpecificProcessing() {
        project.tasks.named(compilationInfo.compileAllTaskName).configure {
            it.dependsOn(kotlinTask)
        }

        val compilation = compilationInfo.tcs.compilation as KotlinJsIrCompilation

        compilation.binaries
            .withType(JsIrBinary::class.java)
            .all { binary ->
                val configAction = KotlinJsIrLinkConfig(binary)
                configAction.configureTask {
                    it.description = taskDescription
                    it.libraries.from(project.filesProvider { compilation.runtimeDependencyFiles })
                }
                configAction.configureTask { task ->
                    val targetCompilerOptions = (compilation.target as KotlinJsIrTarget).compilerOptions
                    KotlinJsCompilerOptionsHelper.syncOptionsAsConvention(
                        targetCompilerOptions,
                        task.compilerOptions
                    )

                    // Restoring already configured module name
                    task.compilerOptions.moduleName.convention(
                        project.provider { compilation.npmProject.name }
                    )

                    task.modeProperty.set(binary.mode)
                    task.dependsOn(kotlinTask)
                }

                tasksProvider.registerKotlinJsIrTask(project, binary.linkTaskName, configAction)
            }

        project.whenEvaluated {
            val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project)
            /* Not supported in KPM, yet */
            compilationInfo.tcs.compilation.let { compilation ->
                subpluginEnvironment.addSubpluginOptions(project, compilation)

            }
        }
    }
}