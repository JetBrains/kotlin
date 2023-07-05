/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.compilerRunner.maybeCreateCommonizerClasspathConfiguration
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.ide.Idea222Api
import org.jetbrains.kotlin.gradle.plugin.ide.ideaImportDependsOn
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import java.io.File
import javax.inject.Inject

internal suspend fun Project.cInteropCommonizationEnabled(): Boolean {
    KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript.await()
    return kotlinPropertiesProvider.enableCInteropCommonization
        ?: kotlinPropertiesProvider.enableCInteropCommonizationSetByExternalPlugin
        ?: false
}

internal val Project.isIntransitiveMetadataConfigurationEnabled: Boolean
    get() = PropertiesProvider(this).enableIntransitiveMetadataConfiguration

internal val Project.isOptimisticNumberCommonizationEnabled: Boolean
    get() = PropertiesProvider(this).mppEnableOptimisticNumberCommonization

internal val Project.isPlatformIntegerCommonizationEnabled: Boolean
    get() = PropertiesProvider(this).mppEnablePlatformIntegerCommonization

internal val Project.commonizeTask: TaskProvider<Task>
    get() = locateOrRegisterTask(
        "commonize",
        invokeWhenRegistered = {
            @OptIn(Idea222Api::class)
            ideaImportDependsOn(this)

            /* 'runCommonizer' is called by older IDEs during import */
            @Suppress("deprecation")
            runCommonizerTask.dependsOn(this)
        },
        configureTask = {
            group = "interop"
            description = "Aggregator task for all c-interop & Native distribution commonizer tasks"
        }
    )

/**
 * Keeping this task/task name for IDE compatibility which is invoking 'runCommonizer' during sync
 */
@Deprecated("Use 'commonizeTask' instead. Keeping the task for IDE compatibility", replaceWith = ReplaceWith("commonizeTask"))
internal val Project.runCommonizerTask: TaskProvider<Task>
    get() = locateOrRegisterTask(
        "runCommonizer",
        configureTask = {
            group = "interop"
            description = "[Deprecated: Use 'commonize' instead]"
        }
    )

private const val commonizeCInteropTaskName = "commonizeCInterop"

internal suspend fun Project.commonizeCInteropTask(): TaskProvider<CInteropCommonizerTask>? {
    if (cInteropCommonizationEnabled()) {
        return locateOrRegisterTask(
            commonizeCInteropTaskName,
            invokeWhenRegistered = {
                val task = this
                commonizeTask.dependsOn(this)
                whenEvaluated {
                    commonizeNativeDistributionTask?.let(task::dependsOn)
                }
            },
            configureTask = {
                group = "interop"
                description = "Invokes the commonizer on c-interop bindings of the project"

                kotlinPluginVersion.set(getKotlinPluginVersion())
                commonizerClasspath.from(project.maybeCreateCommonizerClasspathConfiguration())
                customJvmArgs.set(PropertiesProvider(project).commonizerJvmArgs)
            }
        )
    }
    return null
}

internal suspend fun Project.copyCommonizeCInteropForIdeTask(): TaskProvider<CopyCommonizeCInteropForIdeTask>? {
    val commonizeCInteropTask = commonizeCInteropTask()
    if (commonizeCInteropTask != null) {
        return locateOrRegisterTask(
            "copyCommonizeCInteropForIde",
            args = listOf(commonizeCInteropTask),
            invokeWhenRegistered = {
                @OptIn(Idea222Api::class)
                ideaImportDependsOn(this)

                /* Older IDEs will still call 'runCommonizer' -> 'commonize'  tasks */
                if (isInIdeaSync) {
                    commonizeTask.dependsOn(this)
                }
            },
            configureTask = {
                group = "interop"
                description = "Copies the output of $commonizeCInteropTaskName into " +
                        "the root projects .gradle folder for the IDE"
            }
        )
    }
    return null
}

internal val Project.commonizeNativeDistributionTask: TaskProvider<NativeDistributionCommonizerTask>?
    get() {
        if (!isAllowCommonizer()) return null
        return rootProject.locateOrRegisterTask(
            "commonizeNativeDistribution",
            invokeWhenRegistered = {
                /**
                 * https://github.com/gradle/gradle/issues/13252
                 * https://github.com/gradle/gradle/issues/20145
                 * https://youtrack.jetbrains.com/issue/KT-51583
                 */
                if (rootProject.plugins.findPlugin("jvm-ecosystem") == null) {
                    rootProject.plugins.apply("jvm-ecosystem")
                }

                commonizeTask.dependsOn(this)
                rootProject.commonizeTask.dependsOn(this)
                cleanNativeDistributionCommonizerTask
            },
            configureTask = {
                group = "interop"
                description = "Invokes the commonizer on platform libraries provided by the Kotlin/Native distribution"

                kotlinPluginVersion.set(getKotlinPluginVersion())
                commonizerClasspath.from(rootProject.maybeCreateCommonizerClasspathConfiguration())
                customJvmArgs.set(PropertiesProvider(rootProject).commonizerJvmArgs)
            }
        )
    }

internal val Project.cleanNativeDistributionCommonizerTask: TaskProvider<CleanNativeDistributionCommonizerTask>?
    get() {
        val commonizeNativeDistributionTask = commonizeNativeDistributionTask ?: return null
        return rootProject.locateOrRegisterTask(
            "cleanNativeDistributionCommonization",
            configureTask = {
                group = "interop"
                description = "Deletes all previously commonized klib's from the Kotlin/Native distribution"

                commonizerDirectory.set(commonizeNativeDistributionTask.map { it.rootOutputDirectory })
            }
        )
    }

@DisableCachingByDefault
internal abstract class CleanNativeDistributionCommonizerTask : DefaultTask() {
    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:OutputDirectory
    abstract val commonizerDirectory: Property<File>

    @TaskAction
    fun action() {
        NativeDistributionCommonizerLock(commonizerDirectory.get()).withLock { lockFile ->
            val files = commonizerDirectory.get().listFiles().orEmpty().toSet() - lockFile
            fileSystemOperations.delete {
                it.delete(files)
            }
        }
    }
}
