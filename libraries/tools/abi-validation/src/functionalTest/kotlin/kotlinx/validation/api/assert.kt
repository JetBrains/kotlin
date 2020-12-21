/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.assertEquals

/**
 * Helper `fun` for asserting a [TaskOutcome] to be equal to [TaskOutcome.SUCCESS]
 */
internal fun BuildResult.assertTaskSuccess(task: String) {
    assertTaskOutcome(TaskOutcome.SUCCESS, task)
}

/**
 * Helper `fun` for asserting a [TaskOutcome] to be equal to [TaskOutcome.FAILED]
 */
internal fun BuildResult.assertTaskFailure(task: String) {
    assertTaskOutcome(TaskOutcome.FAILED, task)
}

private fun BuildResult.assertTaskOutcome(taskOutcome: TaskOutcome, taskName: String) {
    assertEquals(taskOutcome, task(taskName)?.outcome)
}