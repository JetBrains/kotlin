/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.tasks.GradleCompileTaskProvider
import java.io.File

/**
 * Uses Gradle worker api to run kotlin compilation.
 */
internal class GradleCompilerRunnerWithWorkers(
    taskProvider: GradleCompileTaskProvider,
    jdkToolsJar: File?,
    private val workerExecutor: WorkerExecutor
) : GradleCompilerRunner(taskProvider, jdkToolsJar) {
    override fun runCompilerAsync(workArgs: GradleKotlinCompilerWorkArguments) {
        loggerProvider.kotlinDebug { "Starting Kotlin compiler work from task '${pathProvider}'" }

        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(GradleKotlinCompilerWorkAction::class.java) {
            it.compilerWorkArguments.set(workArgs)
        }
    }

    internal abstract class GradleKotlinCompilerWorkAction
        : WorkAction<GradleKotlinCompilerWorkParameters> {
        override fun execute() {
            GradleKotlinCompilerWork(
                parameters.compilerWorkArguments.get()
            ).run()
        }
    }

    internal interface GradleKotlinCompilerWorkParameters : WorkParameters {
        val compilerWorkArguments: Property<GradleKotlinCompilerWorkArguments>
    }
}