/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KaptExtensionConfig
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.disableClassloaderCacheForProcessors
import org.jetbrains.kotlin.gradle.internal.KaptCombinedTask
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.internal.buildKaptSubpluginOptions
import org.jetbrains.kotlin.gradle.internal.kapt.KaptProperties
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinAndroidPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions

internal class KaptCombinedConfig(
    project: Project,
    compilation: KotlinCompilation<*>,
    val ext: KaptExtensionConfig,
) : BaseKotlinCompileConfig<KaptCombinedTask>(KotlinCompilationInfo(compilation)) {

    // from generate stubs task
    init {
        configureFromExtension(project.extensions.getByType(KaptExtension::class.java))

        configureTask { kaptGenerateStubsTask ->
            // Syncing compiler options from related KotlinJvmCompile task
            @Suppress("DEPRECATION") val jvmCompilerOptions = compilation.compilerOptions.options as KotlinJvmCompilerOptions
            syncOptionsFromCompileTask(jvmCompilerOptions, kaptGenerateStubsTask)
        }
    }

    internal fun syncOptionsFromCompileTask(
        taskCompilerOptions: KotlinJvmCompilerOptions,
        kaptGenerateStubsTask: KaptCombinedTask,
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

    // from apt task
    init {
        configureTaskProvider { taskProvider ->
            val kaptClasspathSnapshot = KaptConfig.getKaptClasspathSnapshot(project, taskProvider)
            taskProvider.configure { task ->
                task.verbose.set(KaptTask.queryKaptVerboseProperty(project))

                task.isIncremental = KaptProperties.isIncrementalKapt(project).get()
                task.useBuildCache = ext.useBuildCache

                task.includeCompileClasspath.set(
                    project.provider<Boolean> { ext.includeCompileClasspath }.orElse(KaptProperties.isIncludeCompileClasspath(project))
                )
                task.classpathStructure.from(kaptClasspathSnapshot)

//                task.localStateDirectories.from({ task.incAptCache.orNull })
                task.onlyIf {
                    it as KaptCombinedTask
                    it.includeCompileClasspath.get() || !it.kaptClasspath.isEmpty
                }

                task.compiledSources
                    .from(
                        { task.kotlinCompileDestinationDirectory },
                        { task.javaOutputDir.takeIf { it.isPresent } }
                    )
                    .disallowChanges()

//                val kaptSources = objectFactory.fileCollection()
//                    .from(task.javaSources, task.stubsDir)
//                    .asFileTree
//                    .matching { it.include("**/*.java") }
//                    .filter {
//                        it.exists() &&
//                                !isAncestor(task.destinationDir.get().asFile, it) &&
//                                !isAncestor(task.classesDir.get().asFile, it)
//                    }
//                task.javaSourcesForApt.from(kaptSources).disallowChanges()

                task.addJdkClassesToClasspath.set(
                    project.providers.provider {
                        project.plugins.none { it is AbstractKotlinAndroidPluginWrapper }
                    }
                )
//                task.kaptJars.from(project.configurations.getByName(Kapt3GradleSubplugin.KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME))
                task.mapDiagnosticLocations = ext.mapDiagnosticLocations
                task.stubGenerationScheme.convention(ext.stubGenerationScheme)
//
                if (ext is KaptExtension) {
                    task.annotationProcessorFqNames.set(providers.provider {
                        ext.processors.split(',').filter { it.isNotEmpty() }
                    })
                }
                task.disableClassloaderCacheForProcessors = project.disableClassloaderCacheForProcessors()
//                task.classLoadersCacheSize = KaptProperties.getClassloadersCacheSize(project).get()
                task.javacOptions.set(KaptConfig.getJavaOptions(ext, providers, task.defaultJavaSourceCompatibility))
            }
        }
    }


    private fun configureFromExtension(kaptExtension: KaptExtensionConfig) {
        configureTask { task ->
            task.verbose.set(KaptTask.queryKaptVerboseProperty(project))
            if (kaptExtension is KaptExtension) {
                task.pluginOptions.add(buildOptions(kaptExtension, task))
            }
        }
    }

    private fun buildOptions(kaptExtension: KaptExtension, task: KaptCombinedTask): Provider<CompilerPluginOptions> {
        val javacOptions = project.provider { kaptExtension.getJavacOptions() }
        return project.provider {
            val compilerPluginOptions = CompilerPluginOptions()
            buildKaptSubpluginOptions(
                kaptExtension,
                project,
                javacOptions.get(),
                aptMode = "stubsAndApt",
                generatedSourcesDir = objectFactory.fileCollection().from(task.destinationDirectory.asFile),
                generatedClassesDir = objectFactory.fileCollection().from(task.classesDir.asFile),
                incrementalDataDir = objectFactory.fileCollection().from(task.destinationDirectory.asFile),
                includeCompileClasspath = isIncludeCompileClasspath(kaptExtension),
                kaptStubsDir = objectFactory.fileCollection().from(task.stubsDir.asFile)
            ).forEach {
                compilerPluginOptions.addPluginArgument(Kapt3GradleSubplugin.KAPT_SUBPLUGIN_ID, it)
            }
            return@provider compilerPluginOptions
        }
    }

    private fun isIncludeCompileClasspath(kaptExtension: KaptExtensionConfig) =
        kaptExtension.includeCompileClasspath ?: KaptProperties.isIncludeCompileClasspath(project).get()

}
