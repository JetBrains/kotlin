/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.KAPT_SUBPLUGIN_ID
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.isIncludeCompileClasspath
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.internal.KotlinJvmCompilerArgumentsContributor
import org.jetbrains.kotlin.gradle.internal.buildKaptSubpluginOptions
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompilerArgumentsProvider
import java.util.concurrent.Callable

internal class KaptGenerateStubsConfig : BaseKotlinCompileConfig<KaptGenerateStubsTask> {

    constructor(compilation: KotlinCompilationData<*>, kotlinTaskProvider: TaskProvider<KotlinCompile>) : super(compilation) {
        configureFromExtension(project.extensions.getByType(KaptExtension::class.java))
        configureTask { task ->
            val kotlinCompileTask = kotlinTaskProvider.get()
            task.useModuleDetection.value(kotlinCompileTask.useModuleDetection).disallowChanges()
            task.moduleName.value(kotlinCompileTask.moduleName).disallowChanges()
            task.libraries.from({ kotlinCompileTask.libraries })
            task.compileKotlinArgumentsContributor.set(providers.provider { kotlinCompileTask.compilerArgumentsContributor })
            task.pluginOptions.addAll(kotlinCompileTask.pluginOptions)
        }
    }

    constructor(project: Project, ext: KotlinTopLevelExtension, kaptExtension: KaptExtension) : super(project, ext) {
        configureFromExtension(kaptExtension)
        configureTask { task ->
            task.compileKotlinArgumentsContributor.set(
                providers.provider {
                    KotlinJvmCompilerArgumentsContributor(KotlinJvmCompilerArgumentsProvider(task))
                }
            )
        }
    }

    private fun configureFromExtension(kaptExtension: KaptExtension) {
        configureTask { task ->
            task.verbose.set(KaptTask.queryKaptVerboseProperty(project))
            task.pluginOptions.add(buildOptions(kaptExtension, task))

            if (!isIncludeCompileClasspath(kaptExtension)) {
                task.onlyIf {
                    !(it as KaptGenerateStubsTask).kaptClasspath.isEmpty
                }
            }
        }
    }

    private fun isIncludeCompileClasspath(kaptExtension: KaptExtension) = kaptExtension.includeCompileClasspath ?: project.isIncludeCompileClasspath()

    private fun buildOptions(kaptExtension: KaptExtension, task: KaptGenerateStubsTask): Provider<CompilerPluginOptions> {
        val javacOptions = project.provider { kaptExtension.getJavacOptions() }
        return project.provider {
            val compilerPluginOptions = CompilerPluginOptions()
            buildKaptSubpluginOptions(
                kaptExtension,
                project,
                javacOptions.get(),
                aptMode = "stubs",
                generatedSourcesDir = objectFactory.fileCollection().from(task.destinationDirectory.asFile),
                generatedClassesDir = objectFactory.fileCollection().from(task.destinationDirectory.asFile),
                incrementalDataDir = objectFactory.fileCollection().from(task.destinationDirectory.asFile),
                includeCompileClasspath = isIncludeCompileClasspath(kaptExtension),
                kaptStubsDir = objectFactory.fileCollection().from(task.stubsDir.asFile)
            ).forEach {
                compilerPluginOptions.addPluginArgument(KAPT_SUBPLUGIN_ID, it)
            }
            return@provider compilerPluginOptions
        }
    }
}