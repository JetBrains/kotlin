/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KaptExtensionConfig
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.internal.*
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.KAPT_SUBPLUGIN_ID
import org.jetbrains.kotlin.gradle.internal.kapt.KaptProperties
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.whenKaptEnabled

internal class KaptGenerateStubsConfig : BaseKotlinCompileConfig<KaptGenerateStubsTask> {

    constructor(
        compilation: KotlinCompilation<*>
    ) : super(KotlinCompilationInfo(compilation)) {
        configureFromExtension(project.extensions.getByType(KaptExtension::class.java))

        configureTask { kaptGenerateStubsTask ->
            // Syncing compiler options from related KotlinJvmCompile task
            @Suppress("DEPRECATION") val jvmCompilerOptions = compilation.compilerOptions.options as KotlinJvmCompilerOptions
            syncOptionsFromCompileTask(jvmCompilerOptions, kaptGenerateStubsTask)
        }
    }

    constructor(
        project: Project,
        explicitApiMode: Provider<ExplicitApiMode>,
        kaptExtension: KaptExtensionConfig
    ) : super(project, explicitApiMode) {
        configureFromExtension(kaptExtension)
    }

    private fun configureFromExtension(kaptExtension: KaptExtensionConfig) {
        configureTask { task ->
            task.verbose.set(KaptTask.queryKaptVerboseProperty(project))
            if (kaptExtension is KaptExtension) {
                task.pluginOptions.add(buildOptions(kaptExtension, task))
            }
            task.useK2Kapt.value(KaptProperties.isUseK2(project)).finalizeValueOnRead()

            if (!isIncludeCompileClasspath(kaptExtension)) {
                task.onlyIf {
                    !(it as KaptGenerateStubsTask).kaptClasspath.isEmpty
                }
            }
        }
    }

    private fun isIncludeCompileClasspath(kaptExtension: KaptExtensionConfig) =
        kaptExtension.includeCompileClasspath ?: KaptProperties.isIncludeCompileClasspath(project).get()

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

    companion object {
        internal fun wireJavaAndKotlinOutputs(
            project: Project,
            javaCompileTask: TaskProvider<out AbstractCompile>,
            kotlinCompileTask: TaskProvider<out KotlinJvmCompile>
        ) {
            project.whenKaptEnabled {
                val kaptGenerateStubsTaskName = getKaptTaskName(kotlinCompileTask.name, KAPT_GENERATE_STUBS_PREFIX)
                project.tasks.withType<KaptGenerateStubsTask>().configureEach { task ->
                    if (task.name == kaptGenerateStubsTaskName) {
                        task.javaOutputDir.set(javaCompileTask.flatMap { it.destinationDirectory })
                        task.kotlinCompileDestinationDirectory.set(kotlinCompileTask.flatMap { it.destinationDirectory })
                    }
                }
            }
        }

        internal fun configureLibraries(
            project: Project,
            kotlinCompileTask: TaskProvider<out KotlinJvmCompile>,
            vararg paths: Any
        ) {
            project.whenKaptEnabled {
                val kaptGenerateStubsTaskName = getKaptTaskName(kotlinCompileTask.name, KAPT_GENERATE_STUBS_PREFIX)
                project.tasks.withType<KaptGenerateStubsTask>().configureEach { task ->
                    if (task.name == kaptGenerateStubsTaskName) {
                        task.libraries.from(paths)
                    }
                }
            }
        }

        internal fun configureUseModuleDetection(
            project: Project,
            kotlinCompileTask: TaskProvider<out KotlinJvmCompile>,
            config: Property<Boolean>.() -> Unit
        ) {
            project.whenKaptEnabled {
                val kaptGenerateStubsTaskName = getKaptTaskName(kotlinCompileTask.name, KAPT_GENERATE_STUBS_PREFIX)
                project.tasks.withType<KaptGenerateStubsTask>().configureEach { task ->
                    if (task.name == kaptGenerateStubsTaskName) {
                        config(task.useModuleDetection)
                    }
                }
            }
        }

        internal fun syncOptionsFromCompileTask(
            taskCompilerOptions: KotlinJvmCompilerOptions,
            kaptGenerateStubsTask: KaptGenerateStubsTask,
        ) {
            // Syncing compiler options from related KotlinJvmCompile task
            KotlinJvmCompilerOptionsHelper.syncOptionsAsConvention(
                from = taskCompilerOptions,
                into = kaptGenerateStubsTask.compilerOptions
            )

            // This task should not sync any freeCompilerArgs from relevant KotlinCompile task
            // when someone explicitly configures any value for this task as well.
            // Here we reset any configured value and say that use KotlinCompile freeCompilerArgs as convention
            kaptGenerateStubsTask.compilerOptions.freeCompilerArgs.value(null as Iterable<String>?)
            kaptGenerateStubsTask.compilerOptions.freeCompilerArgs.convention(taskCompilerOptions.freeCompilerArgs)
        }
    }
}
