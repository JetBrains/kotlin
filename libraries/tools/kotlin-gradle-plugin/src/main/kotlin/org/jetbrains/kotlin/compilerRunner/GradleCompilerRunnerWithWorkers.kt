/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.gradle.workers.ForkMode
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.gradle.utils.SerializableOptional

internal class GradleCompilerRunnerWithWorkers(
    project: Project,
    private val workersExecutor: WorkerExecutor
) : GradleCompilerRunner(project) {
    override fun compileWithDaemonOrFallback(
        compilerClassName: String,
        compilerArgs: CommonCompilerArguments,
        environment: GradleCompilerEnvironment
    ) {
        val icEnv = environment.incrementalCompilationEnvironment
        val modulesInfo = icEnv?.let { buildModulesInfo(project.gradle) }

        workersExecutor.submit(KotlinCompilerRunnable::class.java) { config ->
            config.isolationMode = IsolationMode.NONE
            config.forkMode = ForkMode.NEVER
            config.params(
                ProjectFilesForCompilation(project),
                environment.compilerFullClasspath,
                compilerClassName,
                ArgumentUtils.convertArgumentsToStringList(compilerArgs).toTypedArray(),
                compilerArgs.verbose,
                SerializableOptional(icEnv),
                SerializableOptional(modulesInfo)
            )
        }
    }

}