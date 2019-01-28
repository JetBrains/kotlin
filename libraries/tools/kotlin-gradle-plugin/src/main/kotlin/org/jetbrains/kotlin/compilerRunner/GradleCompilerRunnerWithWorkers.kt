/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Task
import org.gradle.workers.ForkMode
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.logging.kotlinDebug

internal class GradleCompilerRunnerWithWorkers(
    task: Task,
    private val workersExecutor: WorkerExecutor
) : GradleCompilerRunner(task) {
    override fun runCompilerAsync(workArgs: GradleKotlinCompilerWorkArguments) {
        task.logger.kotlinDebug { "Starting Kotlin compiler work from task '${task.path}'" }
        // todo: write tests with Workers enabled;
        workersExecutor.submit(GradleKotlinCompilerWork::class.java) { config ->
            config.isolationMode = IsolationMode.NONE
            config.params(workArgs)
        }
    }

}