/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import kotlin.test.fail

fun Task.isDependsOn(other: Task): Boolean {
    return other in this.taskDependencies.getDependencies(null)
}

fun Task.assertDependsOn(other: Task) {
    if (!isDependsOn(other)) {
        fail("Expected ${this.path} to depend on ${other.path}")
    }
}

fun Task.assertNotDependsOn(other: Task) {
    if (isDependsOn(other)) {
        fail("Expected ${this.path} not to depend on ${other.path}")
    }
}

fun Task.assertNoCircularTaskDependencies() {
    data class TaskAndDependants(
        val task: Task,
        val dependants: List<Task>,
    )

    val visited = hashMapOf<String, List<String>>()
    val queue = ArrayDeque(
        taskDependencies.getDependencies(this).map { TaskAndDependants(it, listOf(this)) }
    )

    while (queue.isNotEmpty()) {
        val (task, taskDependencies) = queue.removeFirst()
        visited.put(task.path, taskDependencies.map { it.path })

        val dependencies = task.taskDependencies.getDependencies(task)
        queue.addAll(dependencies.map { TaskAndDependants(it, taskDependencies + task) })
    }

    val taskWithCircularDependecy = visited.hasCycle()
    if (taskWithCircularDependecy != null) fail("Task $name has circular dependency on $taskWithCircularDependecy")
}

// Uses Depth-First Search algorithm to detect circular dependencies in the directed tasks graph
private fun HashMap<String, List<String>>.hasCycle(): String? {
    val visited = hashSetOf<String>()
    val inStack = hashSetOf<String>() // Tracks nodes in the current path (DFS stack)

    for (node in keys) {
        val failedNode = dfs(node, visited, inStack)
        if (failedNode != null) return failedNode // Cycle detected
    }

    return null // No cycle found
}

private fun HashMap<String, List<String>>.dfs(
    node: String,
    visited: MutableSet<String>,
    inStack: MutableSet<String>,
): String? {
    if (inStack.contains(node)) return node // Cycle detected
    if (visited.contains(node)) return null // Node already processed, no cycle here

    // Mark the current node as visited and add to inStack
    visited.add(node)
    inStack.add(node)

    // Recursively visit neighbors
    for (neighbor in getOrDefault(node, emptyList<String>())) {
        val failedNode = dfs(neighbor, visited, inStack)
        if (failedNode != null) return failedNode
    }

    // Remove node from current stack (backtrack)
    inStack.remove(node)
    return null
}


fun Task.assertTaskDependenciesEquals(dependencies: Set<Task>) {
    assertEquals(
        dependencies, this.taskDependencies.getDependencies(null),
        "Expected given set of taskDependencies for task ${this.path}"
    )
}

fun Project.assertContainsTaskWithName(taskName: String): Task {
    this.getKotlinPluginVersion()
    return project.tasks.findByName(taskName)
        ?: fail("Expected task with name $taskName in project ${this.path}")
}

fun Project.assertContainsNoTaskWithName(taskName: String) {
    if (taskName in tasks.names) {
        fail("Expected *no* task with name $taskName in project ${this.path}")
    }
}

inline fun <reified T : Task> Project.assertContainsTaskInstance(taskName: String): T {
    assertContainsTaskWithName(taskName)
    val task = tasks.getByName(taskName)
    return assertIsInstance<T>(task)
}

fun Project.assertContainsDependencies(configurationName: String, vararg dependencyNotations: Any, exhaustive: Boolean = false) {
    val configuration = configurations.getByName(configurationName)
    val expectedDependencies = dependencyNotations.map { dependencies.create(it) }.toMutableSet()
    val unexpectedDependencies = mutableSetOf<Dependency>()

    for (dependency in configuration.allDependencies) {
        if (!expectedDependencies.remove(dependency)) {
            unexpectedDependencies.add(dependency)
        }
    }

    if (exhaustive) {
        assertTrue(
            unexpectedDependencies.isEmpty(),
            "Unexpected dependencies '$unexpectedDependencies' found in configuration '$configurationName'"
        )
    }

    assertTrue(
        expectedDependencies.isEmpty(),
        "Expected dependencies '$expectedDependencies' not found in configuration '$configurationName'"
    )
}

fun Project.assertNotContainsDependencies(configurationName: String, vararg dependencyNotations: Any) {
    val configuration = configurations.getByName(configurationName)
    val unexpectedDependencies = dependencyNotations.map { dependencies.create(it) }.toMutableSet()

    val foundUnexpectedDependencies = mutableSetOf<Dependency>()

    for (dependency in configuration.allDependencies) {
        if (dependency in unexpectedDependencies) {
            foundUnexpectedDependencies.add(dependency)
        }
    }

    assertTrue(
        foundUnexpectedDependencies.isEmpty(),
        "Unexpected dependencies '$foundUnexpectedDependencies' found in configuration '$configurationName'"
    )
}

inline fun <reified T> assertIsInstance(value: Any?): T {
    if (value is T) return value
    fail("Expected $value to implement ${T::class.java}")
}

/**
 * Assert that given consumable configuration [configurationName] depends on [expectedTaskNames] tasks
 */
fun Project.assertConfigurationsHaveTaskDependencies(
    configurationName: String,
    vararg expectedTaskNames: String,
) {
    val actualNames = configurations
        .getByName(configurationName)
        .outgoing
        .artifacts
        .buildDependencies.getDependencies(null)
        .map { it.path }

    assertEquals(expectedTaskNames.toSet(), actualNames.toSet(), "Unexpected task dependencies for $configurationName")
}

/** Assert that [actual] contains substring [expected] */
fun assertContains(
    expected: String,
    actual: String,
    ignoreCase: Boolean = false,
) {
    if (!actual.contains(expected, ignoreCase = ignoreCase)) {
        fail("expected:<string contains '$expected' (ignoreCase:$ignoreCase)> but was:<$actual>")
    }
}

/** Assert that [actual] does _not_ contain substring [expected] */
fun assertNotContains(
    expected: String,
    actual: String,
    ignoreCase: Boolean = false,
) {
    if (actual.contains(expected, ignoreCase = ignoreCase)) {
        fail("expected:<string does not contain '$expected' (ignoreCase:$ignoreCase)> but was:<$actual>")
    }
}

inline fun <reified T : Throwable> assertFailsWithChainedCause(block: () -> Unit): T {
    val throwable = assertFails(block)
    var cause: Throwable? = throwable
    while (cause != null) {
        if (cause is T) return cause
        cause = cause.cause
    }
    fail("Expected to fail with ${T::class.java.name} but failed with ${throwable::class.java.name}")
}