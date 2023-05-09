/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.configuration.KaptGenerateStubsConfig
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinCompileConfig
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.whenKaptEnabled

internal class Kotlin2JvmSourceSetProcessor(
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: KotlinCompilationInfo
) : KotlinSourceSetProcessor<KotlinCompile>(
    tasksProvider, "Compiles the $kotlinCompilation.", kotlinCompilation
) {
    override fun doRegisterTask(project: Project, taskName: String): TaskProvider<out KotlinCompile> {
        val configAction = KotlinCompileConfig(compilationInfo)
        applyStandardTaskConfiguration(configAction)
        return tasksProvider.registerKotlinJVMTask(
            project,
            taskName,
            compilationInfo.compilerOptions.options as KotlinJvmCompilerOptions,
            configAction
        ).also { kotlinTask ->
            // Configuring here to not interfere with 'kotlin-android' plugin configuration for 'libraries' input
            KaptGenerateStubsConfig.configureLibraries(
                project,
                kotlinTask,
                { compilationInfo.compileDependencyFiles }
            )
        }
    }

    override fun doTargetSpecificProcessing() {
        project.whenKaptEnabled {
            Kapt3GradleSubplugin.createAptConfigurationIfNeeded(project, compilationInfo.compilationName)
        }

        ScriptingGradleSubplugin.configureForSourceSet(project, compilationInfo.compilationName)

        project.whenEvaluated {
            val subpluginEnvironment = SubpluginEnvironment.loadSubplugins(project)
            /* Not supported in KPM yet */
            compilationInfo.tcsOrNull?.compilation?.let { compilation ->
                subpluginEnvironment.addSubpluginOptions(project, compilation)
            }

            javaSourceSet?.let { java ->
                val javaTask = project.tasks.withType<AbstractCompile>().named(java.compileJavaTaskName)
                javaTask.configure { javaCompile ->
                    javaCompile.classpath += project.files(kotlinTask.flatMap { it.destinationDirectory })
                }
                kotlinTask.configure { kotlinCompile ->
                    kotlinCompile.javaOutputDir.set(javaTask.flatMap { it.destinationDirectory })
                }

                KaptGenerateStubsConfig.wireJavaAndKotlinOutputs(
                    project,
                    javaTask,
                    kotlinTask
                )
            }

            if (sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME) {
                project.pluginManager.withPlugin("java-library") {
                    registerKotlinOutputForJavaLibrary(kotlinTask.flatMap { it.destinationDirectory })
                }
            }
        }
    }

    private fun registerKotlinOutputForJavaLibrary(outputDir: Provider<Directory>) {
        val configuration = project.configurations.getByName("apiElements")
        configuration.outgoing.variants.getByName("classes").artifact(outputDir) {
            it.type = "java-classes-directory"
        }
    }
}