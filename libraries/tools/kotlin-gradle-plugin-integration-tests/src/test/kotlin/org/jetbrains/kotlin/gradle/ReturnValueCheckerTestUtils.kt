/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.jetbrains.kotlin.gradle.testbase.getOutputForTask
import org.jetbrains.kotlin.gradle.testbase.printBuildOutput

/**
 * Asserts that the output produced by the task at [taskPath] contains [expected].
 *
 * Unlike the whole-build `assertOutputContains`, this is scoped to a single task, which is required to attribute a
 * compiler diagnostic (e.g. the return value checker's "Unused return value" warning) to a specific compilation.
 */
internal fun BuildResult.assertTaskOutputContains(taskPath: String, expected: String) {
    val taskOutput = getOutputForTask(taskPath, LogLevel.DEBUG)
    assert(taskOutput.contains(expected)) {
        printBuildOutput()
        "$taskPath output does not contain '$expected'."
    }
}

/**
 * Asserts that the output produced by the task at [taskPath] does not contain [unexpected].
 */
internal fun BuildResult.assertTaskOutputDoesNotContain(taskPath: String, unexpected: String) {
    val taskOutput = getOutputForTask(taskPath, LogLevel.DEBUG)
    assert(!taskOutput.contains(unexpected)) {
        printBuildOutput()
        "$taskPath output unexpectedly contains '$unexpected'."
    }
}
