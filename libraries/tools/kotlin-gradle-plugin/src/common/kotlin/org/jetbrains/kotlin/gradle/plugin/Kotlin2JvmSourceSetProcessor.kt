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
import org.jetbrains.kotlin.gradle.dsl.CompilerJvmOptions
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinCompileConfig
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.whenKaptEnabled

internal class Kotlin2JvmSourceSetProcessor(
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: KotlinCompilationData<*>
) : KotlinSourceSetProcessor<KotlinCompile>(
    tasksProvider, "Compiles the $kotlinCompilation.", kotlinCompilation
) {
    override fun doRegisterTask(project: Project, taskName: String): TaskProvider<out KotlinCompile> {
        val configAction = KotlinCompileConfig(kotlinCompilation)
        applyStandardTaskConfiguration(configAction)
        return tasksProvider.registerKotlinJVMTask(
            project,
            taskName,
            kotlinCompilation.compilerOptions.options as CompilerJvmOptions,
            configAction
        )
    }

    override fun doTargetSpecificProcessing() {
        project.whenKaptEnabled {
            Kapt3GradleSubplugin.createAptConfigurationIfNeeded(project, kotlinCompilation.compilationPurpose)
        }

        ScriptingGradleSubplugin.configureForSourceSet(project, kotlinCompilation.compilationPurpose)

        project.whenEvaluated {
            val subpluginEnvironment = SubpluginEnvironment.loadSubplugins(project)

            if (kotlinCompilation is KotlinCompilation<*>) // FIXME support compiler plugins with PM20
                subpluginEnvironment.addSubpluginOptions(project, kotlinCompilation)

            javaSourceSet?.let { java ->
                val javaTask = project.tasks.withType<AbstractCompile>().named(java.compileJavaTaskName)
                javaTask.configure { javaCompile ->
                    javaCompile.classpath += project.files(kotlinTask.flatMap { it.destinationDirectory })
                }
                kotlinTask.configure { kotlinCompile ->
                    kotlinCompile.javaOutputDir.set(javaTask.flatMap { it.destinationDirectory })
                }
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