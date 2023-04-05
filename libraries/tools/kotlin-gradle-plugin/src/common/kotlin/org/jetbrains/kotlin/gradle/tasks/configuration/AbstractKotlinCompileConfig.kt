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
import org.jetbrains.kotlin.compilerRunner.CompilerSystemPropertiesService
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.dsl.topLevelExtension
import org.jetbrains.kotlin.gradle.incremental.IncrementalModuleInfoBuildService
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.associateWithClosure
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.sources.applyLanguageSettingsToCompilerOptions
import org.jetbrains.kotlin.gradle.report.BuildMetricsService
import org.jetbrains.kotlin.gradle.report.BuildReportsService
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KOTLIN_BUILD_DIR_NAME
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention
import org.jetbrains.kotlin.project.model.LanguageSettings

/**
 * Configuration for the base compile task, [org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile].
 *
 * This contains all data necessary to configure the tasks, and should avoid exposing global state (project, extensions, other tasks)
 * to the task instance as much as possible.
 */
internal abstract class AbstractKotlinCompileConfig<TASK : AbstractKotlinCompile<*>>(
    project: Project,
    val ext: KotlinTopLevelExtension,
    private val languageSettings: Provider<LanguageSettings>
) : TaskConfigAction<TASK>(project) {

    init {
        configureTaskProvider { taskProvider ->
            project.runOnceAfterEvaluated("apply properties and language settings to ${taskProvider.name}") {
                taskProvider.configure {
                    // KaptGenerateStubs will receive value from linked KotlinCompile task
                    if (it is KaptGenerateStubsTask) return@configure

                    applyLanguageSettingsToCompilerOptions(
                        languageSettings.get(),
                        (it as org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>).compilerOptions,
                        // KotlinJsIrTarget and KotlinJsIrTargetConfigurator add additional freeCompilerArgs essentially
                        // always overwriting convention value
                        addFreeCompilerArgsAsConvention = it !is Kotlin2JsCompile
                    )
                }
            }
        }

        val compilerSystemPropertiesService = CompilerSystemPropertiesService.registerIfAbsent(project)
        val buildMetricsService = BuildMetricsService.registerIfAbsent(project)
        val buildReportsService = buildMetricsService?.let { BuildReportsService.registerIfAbsent(project, buildMetricsService) }
        val incrementalModuleInfoProvider =
            IncrementalModuleInfoBuildService.registerIfAbsent(project, objectFactory.providerWithLazyConvention {
                GradleCompilerRunner.buildModulesInfo(project.gradle)
            })
        val buildFinishedListenerService = BuildFinishedListenerService.registerIfAbsent(project)
        configureTask { task ->
            val propertiesProvider = project.kotlinPropertiesProvider

            task.taskBuildCacheableOutputDirectory
                .value(getKotlinBuildDir(task).map { it.dir("cacheable") })
                .disallowChanges()
            task.taskBuildLocalStateDirectory
                .value(getKotlinBuildDir(task).map { it.dir("local-state") })
                .disallowChanges()

            task.localStateDirectories.from(task.taskBuildLocalStateDirectory).disallowChanges()
            buildMetricsService?.also { metricsService ->
                task.buildMetricsService.value(metricsService).disallowChanges()
                buildReportsService?.also { reportsService ->
                    task.buildReportsService.value(reportsService).disallowChanges()
                }
            }
            task.systemPropertiesService.value(compilerSystemPropertiesService).disallowChanges()
            task.incrementalModuleInfoProvider.value(incrementalModuleInfoProvider).disallowChanges()

            propertiesProvider.kotlinDaemonJvmArgs?.let { kotlinDaemonJvmArgs ->
                task.kotlinDaemonJvmArguments.set(providers.provider {
                    kotlinDaemonJvmArgs.split("\\s+".toRegex())
                })
            }
            task.compilerExecutionStrategy.convention(propertiesProvider.kotlinCompilerExecutionStrategy).finalizeValueOnRead()
            task.useDaemonFallbackStrategy.convention(propertiesProvider.kotlinDaemonUseFallbackStrategy).finalizeValueOnRead()
            task.suppressKotlinOptionsFreeArgsModificationWarning
                .convention(propertiesProvider.kotlinOptionsSuppressFreeArgsModificationWarning)
                .finalizeValueOnRead()

            task.preciseCompilationResultsBackup
                .convention(propertiesProvider.preciseCompilationResultsBackup)
                .finalizeValueOnRead()
            task.taskOutputsBackupExcludes.addAll(task.preciseCompilationResultsBackup.map {
                if (it) listOf(task.destinationDirectory.get().asFile, task.taskBuildLocalStateDirectory.get().asFile) else emptyList()
            })
            task.keepIncrementalCompilationCachesInMemory
                .convention(task.preciseCompilationResultsBackup.map { it && propertiesProvider.keepIncrementalCompilationCachesInMemory })
                .finalizeValueOnRead()
            task.taskOutputsBackupExcludes.addAll(task.keepIncrementalCompilationCachesInMemory.map {
                if (it) listOf(task.taskBuildCacheableOutputDirectory.get().asFile) else emptyList()
            })
            task.suppressExperimentalIcOptimizationsWarning
                .convention(propertiesProvider.suppressExperimentalICOptimizationsWarning)
                .finalizeValueOnRead()
            task.buildFinishedListenerService.value(buildFinishedListenerService).disallowChanges()

            task.incremental = false
            task.useModuleDetection.convention(false)
            if (propertiesProvider.useK2 == true) {
                task.compilerOptions.useK2.value(true)
            }
        }
    }

    private fun getKotlinBuildDir(task: TASK): Provider<Directory> =
        task.project.layout.buildDirectory.dir("$KOTLIN_BUILD_DIR_NAME/${task.name}")

    protected fun getClasspathSnapshotDir(task: TASK): Provider<Directory> =
        getKotlinBuildDir(task).map { it.dir("classpath-snapshot") }

    constructor(compilationInfo: KotlinCompilationInfo) : this(
        compilationInfo.project,
        compilationInfo.project.topLevelExtension,
        compilationInfo.project.provider { compilationInfo.languageSettings }
    ) {
        configureTask { task ->
            task.friendPaths.from({ compilationInfo.friendPaths })
            compilationInfo.tcsOrNull?.compilation?.let { compilation ->
                task.friendSourceSets
                    .value(providers.provider { compilation.associateWithClosure.map { it.name } })
                    .disallowChanges()
                task.pluginClasspath.from(
                    compilation.internal.configurations.pluginConfiguration
                )
            }

            @Suppress("DEPRECATION")
            task.ownModuleName.set(project.provider { compilationInfo.moduleName })
            task.sourceSetName.value(providers.provider { compilationInfo.compilationName })
            task.multiPlatformEnabled.value(
                providers.provider {
                    compilationInfo.project.plugins.any {
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