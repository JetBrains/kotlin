/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Intended to track tasks instantiated at execution time and fail the build if there's any.
 * Task instantiation at execution quite often is a problem.
 * It may lead to cases like the one in KT-71328 when calculation of some lazy provider checks `TaskContainer.names`, leading to CME.
 */
abstract class TaskInstantiationTrackingBuildService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    var startedExecution = false
    val tasksExecutedAtExecutionTime = mutableListOf<String>()

    override fun close() {
        require(tasksExecutedAtExecutionTime.isEmpty()) {
            "The following tasks were instantiated at execution time: $tasksExecutedAtExecutionTime"
        }
    }

    companion object {
        fun trackInstantiationInProject(project: Project) {
            val trackingService = project.gradle.sharedServices.registerIfAbsent(
                "trackingService",
                TaskInstantiationTrackingBuildService::class.java
            ) {}

            project.tasks.configureEach { task ->
                task.usesService(trackingService)
                if (trackingService.get().startedExecution) {
                    trackingService.get().tasksExecutedAtExecutionTime.add(task.path)
                }
                task.doFirst {
                    trackingService.get().startedExecution = true
                }
            }
        }
    }
}