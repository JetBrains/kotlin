/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.configuration.KaptGenerateStubsConfig
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinCompileConfig
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.whenKaptEnabled
import java.util.concurrent.Callable

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

        project.launchInStage(AfterEvaluateBuildscript) {
            val subpluginEnvironment = SubpluginEnvironment.loadSubplugins(project)
            /* Not supported in KPM yet */
            compilationInfo.tcs.compilation.let { compilation ->
                subpluginEnvironment.addSubpluginOptions(project, compilation)
            }

            javaSourceSet.await()?.let { java ->
                val javaTask = project.tasks.withType<AbstractCompile>().named(java.compileJavaTaskName)
                javaTask.configure { javaCompile ->
                    val kotlinClasses = project.files(kotlinTask.flatMap { it.destinationDirectory })
                    javaCompile.classpath += kotlinClasses
                    // Only a modular compilation gets the provider, whose class would otherwise make the up-to-date check and
                    // cache key of every compileJava depend on the class loader of KGP. The check is Gradle's for a module.
                    // During IntelliJ sync, the arguments are imported as javac options for IntelliJ builds (IDEA-395306). No
                    // patch is needed there, since the Kotlin and Java classes are compiled to one directory.
                    if (javaCompile is JavaCompile && java.java.srcDirs.any { it.resolve("module-info.java").isFile } &&
                        !project.isInIdeaSync.get()
                    ) {
                        // Resolved lazily, since 'setSource' replaces the collection behind 'source'
                        val javaSources = project.files(Callable { javaCompile.source }).asFileTree
                        javaCompile.options.compilerArgumentProviders.add(
                            KotlinClassesPatchModuleArgumentProvider(
                                javaSources,
                                kotlinClasses,
                                javaCompile.destinationDirectory,
                                javaCompile.options,
                            )
                        )
                    }
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
        }
    }
}