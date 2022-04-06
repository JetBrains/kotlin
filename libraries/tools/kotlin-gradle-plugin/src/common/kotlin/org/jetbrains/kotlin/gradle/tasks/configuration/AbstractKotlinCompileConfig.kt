/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.dsl.topLevelExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.associateWithClosure
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.sources.applyLanguageSettingsToKotlinOptions
import org.jetbrains.kotlin.gradle.report.BuildMetricsReporterService
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KOTLIN_BUILD_DIR_NAME
import org.jetbrains.kotlin.project.model.LanguageSettings

/**
 * Configuration for the base compile task, [org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile].
 *
 * This contains all data necessary to configure the tasks, and should avoid exposing global state (project, extensions, other tasks)
 * to the task instance as much as possible.
 */
internal abstract class AbstractKotlinCompileConfig<TASK : AbstractKotlinCompile<*>>(
    project: Project,
    private val ext: KotlinTopLevelExtension,
    private val languageSettings: Provider<LanguageSettings>
) : TaskConfigAction<TASK>(project) {

    init {
        configureTaskProvider { taskProvider ->
            project.runOnceAfterEvaluated("apply properties and language settings to ${taskProvider.name}") {
                taskProvider.configure {
                    applyLanguageSettingsToKotlinOptions(
                        languageSettings.get(), (it as org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>).kotlinOptions
                    )
                }
            }
        }

        configureTask { task ->
            val propertiesProvider = project.kotlinPropertiesProvider

            task.taskBuildCacheableOutputDirectory
                .value(getKotlinBuildDir(task).map { it.dir("cacheable") })
                .disallowChanges()
            task.taskBuildLocalStateDirectory
                .value(getKotlinBuildDir(task).map { it.dir("local-state") })
                .disallowChanges()

            task.localStateDirectories.from(task.taskBuildLocalStateDirectory).disallowChanges()
            BuildMetricsReporterService.registerIfAbsent(project)?.let {
                task.buildMetricsReporterService.value(it)
            }

            propertiesProvider.kotlinDaemonJvmArgs?.let { kotlinDaemonJvmArgs ->
                task.kotlinDaemonJvmArguments.set(providers.provider {
                    kotlinDaemonJvmArgs.split("\\s+".toRegex())
                })
            }
            task.compilerExecutionStrategy.value(propertiesProvider.kotlinCompilerExecutionStrategy)

            task.incremental = false
            task.useModuleDetection.convention(false)
        }
    }

    private fun getKotlinBuildDir(task: TASK): Provider<Directory> =
        task.project.layout.buildDirectory.dir("$KOTLIN_BUILD_DIR_NAME/${task.name}")

    protected fun getClasspathSnapshotDir(task: TASK): Provider<Directory> =
        getKotlinBuildDir(task).map { it.dir("classpath-snapshot") }

    constructor(compilation: KotlinCompilationData<*>) : this(
        compilation.project, compilation.project.topLevelExtension, compilation.project.provider { compilation.languageSettings }
    ) {
        configureTask { task ->
            task.friendPaths.from({ compilation.friendPaths })
            if (compilation is KotlinCompilation<*>) {
                task.friendSourceSets
                    .value(providers.provider { compilation.associateWithClosure.map { it.name } })
                    .disallowChanges()
                task.pluginClasspath.from(
                    compilation.project.configurations.getByName(compilation.pluginConfigurationName)
                )
            }
            task.moduleName.set(providers.provider { compilation.moduleName })
            task.ownModuleName.set(project.provider { compilation.ownModuleName })
            task.sourceSetName.value(providers.provider { compilation.compilationPurpose })
            task.multiPlatformEnabled.value(
                providers.provider {
                    compilation.project.plugins.any {
                        it is KotlinPlatformPluginBase ||
                                it is AbstractKotlinMultiplatformPluginWrapper ||
                                it is AbstractKotlinPm20PluginWrapper
                    }
                }
            )
        }
    }
}

internal abstract class TaskConfigAction<TASK : Task>(protected val project: Project) {

    protected val objectFactory: ObjectFactory = project.objects
    protected val providers: ProviderFactory = project.providers
    protected val propertiesProvider = project.kotlinPropertiesProvider

    private var executed = false

    // Collect all task configurations, and run them later (in order they were added).
    private val taskConfigActions = ArrayDeque<(TaskProvider<TASK>) -> Unit>()

    fun configureTaskProvider(configAction: (TaskProvider<TASK>) -> Unit) {
        check(!executed) {
            "Task has already been configured. Configuration actions should be added to this object before `this.execute` method runs."
        }
        taskConfigActions.addLast(configAction)
    }

    fun configureTask(configAction: (TASK) -> Unit) {
        configureTaskProvider { taskProvider ->
            taskProvider.configure(configAction)
        }
    }

    fun execute(taskProvider: TaskProvider<TASK>) {
        executed = true
        taskConfigActions.forEach {
            it(taskProvider)
        }
    }
}