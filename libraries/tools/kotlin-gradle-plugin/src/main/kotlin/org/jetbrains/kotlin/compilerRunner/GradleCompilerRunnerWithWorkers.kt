/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.gradle.workers.ForkMode
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor

internal class GradleCompilerRunnerWithWorkers(
    project: Project,
    private val workersExecutor: WorkerExecutor
) : GradleCompilerRunner(project) {
    override fun runCompilerAsync(workArgs: GradleKotlinCompilerWorkArguments) {
        workersExecutor.submit(GradleKotlinCompilerWork::class.java) { config ->
            config.isolationMode = IsolationMode.NONE
            config.forkMode = ForkMode.NEVER
            config.params(workArgs)
        }
    }

}