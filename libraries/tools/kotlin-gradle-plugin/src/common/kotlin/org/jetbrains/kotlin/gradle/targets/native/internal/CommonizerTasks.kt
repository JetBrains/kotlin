/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.ide.Idea222Api
import org.jetbrains.kotlin.gradle.plugin.ide.ideaImportDependsOn
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask

internal val Project.isCInteropCommonizationEnabled: Boolean get() = PropertiesProvider(this).enableCInteropCommonization

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

internal val Project.commonizeCInteropTask: TaskProvider<CInteropCommonizerTask>?
    get() {
        if (isCInteropCommonizationEnabled) {
            return locateOrRegisterTask(
                "commonizeCInterop",
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
                }
            )
        }
        return null
    }

internal val Project.copyCommonizeCInteropForIdeTask: TaskProvider<CopyCommonizeCInteropForIdeTask>?
    get() {
        val commonizeCInteropTask = commonizeCInteropTask
        if (commonizeCInteropTask != null) {
            return locateOrRegisterTask(
                "copyCommonizeCInteropForIde",
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
                    description = "Copies the output of ${commonizeCInteropTask.get().name} into " +
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
                commonizeTask.dependsOn(this)
                rootProject.commonizeTask.dependsOn(this)
                cleanNativeDistributionCommonizerTask
            },
            configureTask = {
                group = "interop"
                description = "Invokes the commonizer on platform libraries provided by the Kotlin/Native distribution"
            }
        )
    }

internal val Project.cleanNativeDistributionCommonizerTask: TaskProvider<Delete>?
    get() {
        val commonizeNativeDistributionTask = commonizeNativeDistributionTask ?: return null
        return rootProject.locateOrRegisterTask(
            "cleanNativeDistributionCommonization",
            configureTask = {
                group = "interop"
                description = "Deletes all previously commonized klib's from the Kotlin/Native distribution"
                delete(commonizeNativeDistributionTask.map { it.getRootOutputDirectory() })
            }
        )
    }
