/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

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
            val occurrences = output.lineSequence().filter { it.contains("> Task $task") }
            System.err.println("ZZZ: task results ${occurrences.joinToString(separator = "\n")}")
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
