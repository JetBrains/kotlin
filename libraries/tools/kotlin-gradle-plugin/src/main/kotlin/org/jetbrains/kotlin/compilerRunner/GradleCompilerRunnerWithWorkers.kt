/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Task
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.tasks.GradleCompileTask

internal class GradleCompilerRunnerWithWorkers(
    taskProvider: TaskProvider<out GradleCompileTask>,
    private val workersExecutor: WorkerExecutor
) : GradleCompilerRunner(taskProvider) {

    override fun runCompilerAsync(workArgs: GradleKotlinCompilerWorkArguments) {
        loggerProvider.get().kotlinDebug { "Starting Kotlin compiler work from task '${pathProvider.get()}'" }
        // todo: write tests with Workers enabled;
        workersExecutor.submit(GradleKotlinCompilerWork::class.java) { config ->
            config.isolationMode = IsolationMode.NONE
            config.params(workArgs)
        }
    }

}