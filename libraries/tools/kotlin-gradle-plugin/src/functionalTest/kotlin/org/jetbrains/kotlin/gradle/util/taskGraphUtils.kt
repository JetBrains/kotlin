/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.execution.plan.ExecutionPlanFactory
import org.gradle.execution.plan.FinalizedExecutionPlan

/**
 * This utility should be used when task graph is checked in KGP
 * Most likely project will require Repositories to resolve dependencies
 * This will not execute tasks though.
 */
fun Project.populateTaskGraph(vararg entryTasks: Task) {
    require(this is ProjectInternal)
    val final = calculateFinalTaskGraph(*entryTasks)
    gradle.taskGraph.populate(final)
}

/**
 * This is more accurate than [Task.getTaskDependencies] as it works with the final task graph
 * rather than direct dependsOn edges.
 */
fun Project.allTaskDependencies(task: Task): List<Task> {
    require(this is ProjectInternal)
    val final = calculateFinalTaskGraph(task)
    return (final.contents.tasks - task).toList()
}

private fun ProjectInternal.calculateFinalTaskGraph(
    vararg entryTasks: Task
): FinalizedExecutionPlan {
    evaluate()

    val executionPlanFactory = gradle.services.get(ExecutionPlanFactory::class.java)
    val plan = executionPlanFactory.createPlan()

    plan.addEntryTasks(entryTasks.toList())

    // this unwraps the plan from entryTasks
    plan.determineExecutionPlan()
    val final = plan.finalizePlan()
    return final
}
