/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal.state

import org.jetbrains.kotlin.gradle.report.TaskExecutionResult
import java.util.concurrent.ConcurrentHashMap

internal object TaskExecutionResults {
    private val results = ConcurrentHashMap<String, TaskExecutionResult>()

    operator fun get(taskPath: String): TaskExecutionResult? =
        results[taskPath]

    /**
     * Merges [result] into any value already present for [taskPath] instead of overwriting it, since more than one
     * writer can legitimately contribute a [TaskExecutionResult] for the same task path - e.g. a task action's own
     * result (written by [org.jetbrains.kotlin.compilerRunner.addBuildMetricsForTaskAction]) and the result of a
     * Gradle worker it submitted work to without awaiting, which can complete either before or after the task
     * action returns. [ConcurrentHashMap.merge] keeps this thread-safe without extra locking.
     */
    operator fun set(taskPath: String, result: TaskExecutionResult) {
        results.merge(taskPath, result) { old, new -> old.merge(new) }
    }

    fun clear() {
        results.clear()
    }
}