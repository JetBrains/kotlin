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
 * Asserts given [tasks] have not been executed.
 */
fun BuildResult.assertTasksNotExecuted(vararg tasks: String) {
    tasks.forEach {
        assert(!this.tasks.contains(task(it))) {
            printBuildOutput()
            "Task $it was executed and finished with state: ${task(it)?.outcome}"
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
 * @param tasksPaths tasks' paths, for which classpath should be checked with give assertions
 * @param toolName name of build tool
 * @param assertions assertions, with will be applied to each classpath of each given task
 */
fun BuildResult.assertNativeTasksClasspath(
    vararg tasksPaths: String,
    toolName: NativeToolKind = NativeToolKind.KONANC,
    assertions: (List<String>) -> Unit
) = tasksPaths.forEach { taskPath -> assertions(extractNativeCompilerClasspath(getOutputForTask(taskPath), toolName)) }

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
    environmentVariables: EnvironmentalVariables = EnvironmentalVariables()
) {
    build("tasks", "--all", environmentVariables = environmentVariables) {
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
    expectedAbsentTasks: List<String> = emptyList()
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