/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.CompilerJsOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.configuration.Kotlin2JsCompileConfig
import java.io.File

internal class Kotlin2JsSourceSetProcessor(
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: KotlinCompilationProjection
) : KotlinSourceSetProcessor<Kotlin2JsCompile>(
    tasksProvider,
    taskDescription = "Compiles the Kotlin sources in $kotlinCompilation to JavaScript.",
    kotlinCompilation = kotlinCompilation
) {
    override fun doRegisterTask(project: Project, taskName: String): TaskProvider<out Kotlin2JsCompile> {
        val configAction = Kotlin2JsCompileConfig(compilationProjection)
        applyStandardTaskConfiguration(configAction)
        return tasksProvider.registerKotlinJSTask(
            project,
            taskName,
            compilationProjection.compilerOptions.options as CompilerJsOptions,
            configAction
        )
    }

    override fun doTargetSpecificProcessing() {
        project.tasks.named(compilationProjection.compileAllTaskName).configure {
            it.dependsOn(kotlinTask)
        }

        compilationProjection.tcsOrNull?.compilation?.run { this as? KotlinWithJavaCompilation<*, *> }?.javaSourceSet?.clearJavaSrcDirs()

        project.whenEvaluated {
            val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project)

            /* Not supported in KPM */
            compilationProjection.tcsOrNull?.compilation?.let { compilation ->
                subpluginEnvironment.addSubpluginOptions(project, compilation)
            }
        }
    }
}

private fun SourceSet.clearJavaSrcDirs() {
    java.setSrcDirs(emptyList<File>())
}
