/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * Asserts given tasks are not present in the build task graph
 */
fun BuildResult.assertTasksAreNotInTaskGraph(vararg tasks: String) {
    val presentTasks = tasks.filter { task(it) != null }
    assert(presentTasks.isEmpty()) {
        printBuildOutput()
        "Tasks ${tasks.joinToString(prefix = "[", postfix = "]")} shouldn't be present in the task graph, but $presentTasks were present"
    }
}

/**
 * Returns all the affected during the build tasks, whose [org.gradle.api.Task.getPath] satisfies the [pattern]
 */
fun BuildResult.findTasksByPattern(pattern: Regex): Set<String> {
    return tasks.map { it.path }.filter { taskPath ->
        pattern.matches(taskPath)
    }.toSet()
}

/**
 * Asserts given [tasks] have 'SUCCESS' execution state.
 */
fun BuildResult.assertTasksExecuted(vararg tasks: String) {
    tasks.forEach { task ->
        assert(task(task)?.outcome == TaskOutcome.SUCCESS) {
            printBuildOutput()
            "Task $task didn't have 'SUCCESS' state: ${task(task)?.outcome}"
        }
    }
}

/**
 * Asserts given [tasks] have 'SUCCESS' execution state.
 */
fun BuildResult.assertTasksExecuted(tasks: Collection<String>) {
    assertTasksExecuted(*tasks.toTypedArray())
}

/**
 * Asserts given [tasks] have 'FAILED' execution state.
 */
fun BuildResult.assertTasksFailed(vararg tasks: String) {
    tasks.forEach { task ->
        assert(task(task)?.outcome == TaskOutcome.FAILED) {
            printBuildOutput()
            "Task $task didn't have 'FAILED' state: ${task(task)?.outcome}"
        }
    }
}

/**
 * Asserts given [tasks] have 'UP-TO-DATE' execution state.
 */
fun BuildResult.assertTasksUpToDate(vararg tasks: String) {
    tasks.forEach { task ->
        assert(task(task)?.outcome == TaskOutcome.UP_TO_DATE) {
            printBuildOutput()
            "Task $task didn't have 'UP-TO-DATE' state: ${task(task)?.outcome}"
        }
    }
}

/**
 * Asserts given [tasks] have 'UP-TO-DATE' execution state.
 */
fun BuildResult.assertTasksUpToDate(tasks: Collection<String>) {
    assertTasksUpToDate(*tasks.toTypedArray())
}

/**
 * Asserts given [tasks] have 'SKIPPED' execution state.
 */
fun BuildResult.assertTasksSkipped(vararg tasks: String) {
    tasks.forEach { task ->
        assert(task(task)?.outcome == TaskOutcome.SKIPPED) {
            printBuildOutput()
            "Task $task didn't have 'SKIPPED' state: ${task(task)?.outcome}"
        }
    }
}

/**
 * Asserts given [tasks] have 'FROM_CACHE' execution state.
 */
fun BuildResult.assertTasksFromCache(vararg tasks: String) {
    tasks.forEach { task ->
        assert(task(task)?.outcome == TaskOutcome.FROM_CACHE) {
            printBuildOutput()
            "Task $task didn't have 'FROM-CACHE' state: ${task(task)?.outcome}"
        }
    }
}

/**
 * Asserts given [tasks] have 'NO_SOURCE' execution state.
 */
fun BuildResult.assertTasksNoSource(vararg tasks: String) {
    tasks.forEach { task ->
        assert(task(task)?.outcome == TaskOutcome.NO_SOURCE) {
            printBuildOutput()
            "Task $task didn't have 'NO_SOURCE' state: ${task(task)?.outcome}"
        }
    }
}

/**
 * Assert new cache entry was created for given [tasks].
 */
fun BuildResult.assertTasksPackedToCache(vararg tasks: String) {
    tasks.forEach {
        assertOutputContains("Stored cache entry for task '$it' with cache key ")
    }
}

/**
 * Asserts classpath of the given K/N compiler tool for given tasks' paths.
 *
 * Note: Log level of output must be set to [LogLevel.DEBUG].
 *
 * @param tasksPaths names of the tasks, which classpath should be checked with give assertions
 * @param toolName name of build tool
 * @param assertions assertions, with will be applied to each classpath of each given task
 */
fun BuildResult.assertNativeTasksClasspath(
    vararg tasksPaths: String,
    toolName: NativeToolKind = NativeToolKind.KONANC,
    assertions: (List<String>) -> Unit
) = tasksPaths.forEach { taskPath -> assertions(extractNativeCompilerClasspath(getOutputForTask(taskPath), toolName)) }
