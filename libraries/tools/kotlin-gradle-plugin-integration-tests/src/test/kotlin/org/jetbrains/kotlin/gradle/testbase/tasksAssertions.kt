/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * Asserts given tasks are not present in the build task graph.
 *
 * (Note: 'not in task graph' has a different meaning to 'not executed'.
 * Tasks with outcomes [TaskOutcome.SKIPPED] and [TaskOutcome.UP_TO_DATE] will be in the task graph, but
 * are not considered 'executed').
 */
fun BuildResult.assertTasksAreNotInTaskGraph(vararg taskPaths: String) {
    val presentTasks = taskPaths.mapNotNull { task(it) }
    assert(presentTasks.isEmpty()) {
        printBuildOutput()
        val allTaskPaths = taskPaths.joinToString(prefix = "[", postfix = "]")
        """
        |Tasks $allTaskPaths shouldn't be present in the task graph, but found ${presentTasks.joinToString { "${it.path} (${it.outcome})" }}
        |
        |All task states:
        |${getActualTasksAsString()}
        """.trimMargin()
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
 * Asserts given [taskPaths] have [TaskOutcome.SUCCESS] execution state.
 */
fun BuildResult.assertTasksExecuted(vararg taskPaths: String) {
    assertTasksHaveOutcome(TaskOutcome.SUCCESS, taskPaths.asList())
}

/**
 * Asserts any of [taskPaths] has [TaskOutcome.SUCCESS] execution state.
 */
fun BuildResult.assertAnyTaskHasBeenExecuted(taskPaths: Set<String>) {
    val taskOutcomes = taskPaths.associateWith { taskPath -> task(taskPath)?.outcome }

    assert(
        taskOutcomes.values.any { it == TaskOutcome.SUCCESS }
    ) {
        printBuildOutput()
        "Expected at least one Task of $taskPaths had outcome 'SUCCESS', but none did. Actual outcomes: $taskOutcomes"
    }
}

/**
 * Asserts given [taskPaths] have [TaskOutcome.SUCCESS] execution state.
 */
fun BuildResult.assertTasksExecuted(taskPaths: Collection<String>) {
    assertTasksExecuted(*taskPaths.toTypedArray())
}

/**
 * Asserts given [taskPaths] have [TaskOutcome.FAILED] execution state.
 */
fun BuildResult.assertTasksFailed(vararg taskPaths: String) {
    assertTasksHaveOutcome(TaskOutcome.FAILED, taskPaths.asList())
}

/**
 * Asserts given [taskPaths] have [TaskOutcome.UP_TO_DATE] execution state.
 */
fun BuildResult.assertTasksUpToDate(vararg taskPaths: String) {
    assertTasksHaveOutcome(TaskOutcome.UP_TO_DATE, taskPaths.asList())
}

/**
 * Asserts given [taskPaths] have [TaskOutcome.UP_TO_DATE] execution state.
 */
fun BuildResult.assertTasksUpToDate(taskPaths: Collection<String>) {
    assertTasksUpToDate(*taskPaths.toTypedArray())
}

/**
 * Asserts given [taskPaths] have [TaskOutcome.SKIPPED] execution state.
 */
fun BuildResult.assertTasksSkipped(vararg taskPaths: String) {
    assertTasksHaveOutcome(TaskOutcome.SKIPPED, taskPaths.asList())
}

/**
 * Asserts given [taskPaths] have [TaskOutcome.FROM_CACHE] execution state.
 */
fun BuildResult.assertTasksFromCache(vararg taskPaths: String) {
    assertTasksHaveOutcome(TaskOutcome.FROM_CACHE, taskPaths.asList())
}

/**
 * Asserts given [taskPaths] have [TaskOutcome.NO_SOURCE] execution state.
 */
fun BuildResult.assertTasksNoSource(vararg taskPaths: String) {
    assertTasksHaveOutcome(TaskOutcome.NO_SOURCE, taskPaths.asList())
}

/**
 * Asserts given [taskPaths] have [expected] execution state.
 */
private fun BuildResult.assertTasksHaveOutcome(expected: TaskOutcome, taskPaths: Collection<String>) {
    taskPaths.forEach { taskPath ->
        val task = task(taskPath)
        assert(task?.outcome == expected) {
            printBuildOutput()
            """
            |Expected Task $taskPath had state:${expected}, but was ${task?.outcome ?: "not executed"}
            |
            |Actual task states:
            |${getActualTasksAsString()}
            """.trimMargin()
        }
    }
}

/**
 * Assert new cache entry was created for given [taskPaths].
 */
fun BuildResult.assertTasksPackedToCache(vararg taskPaths: String) {
    taskPaths.forEach {
        assertOutputContains("Stored cache entry for task '$it' with cache key ")
    }
}

/**
 * Builds test project with 'tasks --all' arguments and then
 * asserts that [registeredTasks] of the given tasks have been registered
 * and tasks from the [notRegisteredTasks] list have not been registered.
 *
 * @param registeredTasks The names of the tasks that should have been registered,
 *                          it could contain task paths as well, but without the first semicolon.
 * @param notRegisteredTasks An optional list of task names that should not have been registered,
 *                           it could contain task paths as well, but without the first semicolon.
 * @param environmentVariables environmental variables for build process
 * @throws AssertionError if any of the registered tasks do not match the expected task names,
 * or if any of the not-registered tasks were actually registered.
 */
@OptIn(EnvironmentalVariablesOverride::class)
fun TestProject.buildAndAssertAllTasks(
    registeredTasks: List<String> = emptyList(),
    notRegisteredTasks: List<String> = emptyList(),
    buildOptions: BuildOptions = this.buildOptions,
    environmentVariables: EnvironmentalVariables = EnvironmentalVariables(),
) {
    build("tasks", "--all", buildOptions = buildOptions, environmentVariables = environmentVariables) {
        assertTasksInBuildOutput(registeredTasks, notRegisteredTasks)
    }
}

/**
 * Inspects the output of the 'tasks' command and asserts that the specified
 * tasks are either present or absent in the output.
 *
 * @param expectedPresentTasks The names of the tasks that should be present in the output,
 *                              it could contain task paths as well, but without the first semicolon.
 * @param expectedAbsentTasks The names of the tasks that should be absent from the output,
 *                              it could contain task paths as well, but without the first semicolon.
 * @throws AssertionError if any of the expected present tasks are not present in the output,
 * or if any of the expected absent tasks are present in the output.
 */
fun BuildResult.assertTasksInBuildOutput(
    expectedPresentTasks: List<String> = emptyList(),
    expectedAbsentTasks: List<String> = emptyList(),
) {
    val registeredTasks = getAllTasksFromTheOutput()
    expectedPresentTasks.forEach {
        assert(registeredTasks.contains(it)) {
            printBuildOutput()
            "Expected $it task is not registered in $registeredTasks"
        }
    }
    expectedAbsentTasks.forEach {
        assert(!registeredTasks.contains(it)) {
            printBuildOutput()
            "$it task should not be registered in $registeredTasks"
        }
    }
}

/**
 * Returns printable list of task paths that are in the task graph.
 */
private fun BuildResult.getActualTasksAsString(): String {
    return tasks.joinToString("\n") { "${it.path} - ${it.outcome}" }
}

/**
 * Method parses the output of a 'tasks --all' build
 * and returns a list of all the tasks mentioned in it.
 *
 * @return A list of all the tasks mentioned in the build 'tasks -all' output
 * @throws IllegalStateException if the build output could not be parsed.
 */
private fun BuildResult.getAllTasksFromTheOutput(): List<String> {

    val taskPattern = Regex("^([:\\w]+) - (.*)$")
    val tasks = mutableListOf<String>()

    output.lines().forEach { line ->
        if (line.matches(taskPattern)) {
            tasks.add(taskPattern.find(line)!!.groupValues[1])
        }
    }

    return tasks
}
