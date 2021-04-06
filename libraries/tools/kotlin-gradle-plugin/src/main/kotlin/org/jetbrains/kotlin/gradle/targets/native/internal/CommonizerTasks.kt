/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask

internal val Project.isCInteropCommonizationEnabled: Boolean get() = PropertiesProvider(this).enableCInteropCommonization

internal val Project.isHierarchicalCommonizationEnabled: Boolean get() = PropertiesProvider(this).enableHierarchicalCommonization

internal val Project.commonizeTask: TaskProvider<Task>
    get() = locateOrRegisterTask(
        "commonize",
        invokeWhenRegistered = { @Suppress("deprecation") runCommonizerTask.dependsOn(this) },
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
        invokeWhenRegistered = { dependsOn(commonizeTask) },
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
                    commonizeTask.dependsOn(this)
                    commonizeNativeDistributionHierarchicallyTask?.let(this::dependsOn)
                    commonizeNativeDistributionTask?.let(this::dependsOn)
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
        if (isCInteropCommonizationEnabled) {
            return locateOrRegisterTask(
                "copyCommonizeCInteropForIde",
                invokeWhenRegistered = { if (isInIdeaSync) commonizeTask.dependsOn(this) },
                configureTask = {
                    group = "interop"
                    description = "Copies the output of ${commonizeCInteropTask?.get()?.name} into " +
                            "the root projects .gradle folder for the IDE"
                }
            )
        }
        return null
    }

internal val Project.commonizeNativeDistributionTask: TaskProvider<NativeDistributionCommonizerTask>?
    get() {
        if (isHierarchicalCommonizationEnabled) return null
        return locateOrRegisterTask(
            "commonizeNativeDistribution",
            invokeWhenRegistered = { commonizeTask.dependsOn(this) },
            configureTask = {
                group = "interop"
                description = "Invokes the commonizer on the platform libraries provided by the Kotlin/Native distribution"
            }
        )
    }

internal val Project.commonizeNativeDistributionHierarchicallyTask: TaskProvider<HierarchicalNativeDistributionCommonizerTask>?
    get() {
        if (!isHierarchicalCommonizationEnabled) return null
        return locateOrRegisterTask(
            "commonizeNativeDistribution",
            invokeWhenRegistered = { commonizeTask.dependsOn(this) },
            configureTask = {
                group = "interop"
                description = "Invokes the commonizer on platform libraries provided by the Kotlin/Native distribution"
            }
        )
    }

private inline fun <reified T : Task> Project.locateOrRegisterTask(
    name: String,
    args: List<Any> = emptyList(),
    invokeWhenRegistered: (TaskProvider<T>.() -> Unit) = {},
    noinline configureTask: (T.() -> Unit)? = null
): TaskProvider<T> {
    locateTask<T>(name)?.let { return it }
    return registerTask(name, args, configureTask).also(invokeWhenRegistered)
}
