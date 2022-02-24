/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.utils.`is`
import kotlin.test.assertEquals
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
        fail("Expected ${this.path} to *not* depend on ${other.path}")
    }
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