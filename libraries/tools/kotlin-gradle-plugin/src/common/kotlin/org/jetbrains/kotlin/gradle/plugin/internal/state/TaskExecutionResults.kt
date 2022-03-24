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

    operator fun set(taskPath: String, result: TaskExecutionResult) {
        results[taskPath] = result
    }

    fun clear() {
        results.clear()
    }
}