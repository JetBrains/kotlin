/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.CompilationErrorException
import org.jetbrains.kotlin.gradle.tasks.FailedCompilationException
import org.jetbrains.kotlin.gradle.tasks.OOMErrorException
import org.jetbrains.kotlin.gradle.tasks.TaskOutputsBackup
import org.jetbrains.kotlin.incremental.IncrementalCompilerFacade
import java.io.File
import java.net.URLClassLoader
import java.util.*
import javax.inject.Inject

internal class GradleIncrementalCompilationFacadeRunner(
    taskProvider: GradleCompileTaskProvider,
    jdkToolsJar: File?,
    compilerExecutionSettings: CompilerExecutionSettings,
    buildMetrics: BuildMetricsReporter,
    private val workerExecutor: WorkerExecutor
) : GradleCompilerRunner(taskProvider, jdkToolsJar, compilerExecutionSettings, buildMetrics) {
    override fun runCompilerAsync(
        workArgs: GradleKotlinCompilerWorkArguments,
        taskOutputsBackup: TaskOutputsBackup?
    ): WorkQueue {

        buildMetrics.addTimeMetric(BuildPerformanceMetric.CALL_WORKER)
        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(IncrementalFacadeRunnerAction::class.java) { params ->
            params.compilerWorkArguments.set(workArgs)
            if (taskOutputsBackup != null) {
                params.taskOutputsToRestore.set(taskOutputsBackup.outputsToRestore)
                params.buildDir.set(taskOutputsBackup.buildDirectory)
                params.snapshotsDir.set(taskOutputsBackup.snapshotsDir)
                params.metricsReporter.set(buildMetrics)
            }
        }
        return workQueue
    }

    internal abstract class IncrementalFacadeRunnerAction @Inject constructor(
        private val fileSystemOperations: FileSystemOperations,
    ) : WorkAction<IncrementalFacadeRunnerParameters> {
        private val logger = Logging.getLogger("kotlin-compile-worker")

        override fun execute() {
            val taskOutputsBackup = if (parameters.snapshotsDir.isPresent) {
                TaskOutputsBackup(
                    fileSystemOperations,
                    parameters.buildDir,
                    parameters.snapshotsDir,
                    parameters.taskOutputsToRestore.get(),
                    logger
                )
            } else {
                null
            }

            try {
                val classpath = parameters.compilerWorkArguments.get().compilerFacadeClasspath
                logger.warn("IC facade classpath: $classpath")
                logger.warn("Trying to invoke IncrementalCompilerFacade.doSomething()...")
                // TODO: cache classloader
                val parentClassloader = LimitedScopeClassLoaderDelegator(
                    IncrementalCompilerFacade::class.java.classLoader,
                    ClassLoader.getSystemClassLoader(),
                    setOf(IncrementalCompilerFacade::class.java.name) // need to share API classes, so we could use them here without reflection
                )
                val classloader = URLClassLoader(classpath.toList().map { it.toURI().toURL() }.toTypedArray(), parentClassloader)
                // TODO: don't do this right here
                val implementationsPropertiesFile = "kotlin-ic-facade/implementations.properties"
                val conn = classloader.getResource(implementationsPropertiesFile)?.openConnection()
                    ?: error("No $implementationsPropertiesFile found")
                conn.useCaches = false
                val implementations = Properties()
                conn.getInputStream().use {
                    implementations.load(it)
                }
                val cls = classloader.loadClass(implementations[IncrementalCompilerFacade::class.java.name].toString())
                val facade = cls.constructors[0].newInstance() as IncrementalCompilerFacade
                facade.doSomething()
            } catch (e: FailedCompilationException) {
                // Restore outputs only in cases where we expect that the user will make some changes to their project:
                //   - For a compilation error, the user will need to fix their source code
                //   - For an OOM error, the user will need to increase their memory settings
                // In the other cases where there is nothing the user can fix in their project, we should not restore the outputs.
                // Otherwise, the next build(s) will likely fail in exactly the same way as this build because their inputs and outputs are
                // the same.
                if (taskOutputsBackup != null && (e is CompilationErrorException || e is OOMErrorException)) {
                    parameters.metricsReporter.get().measure(BuildTime.RESTORE_OUTPUT_FROM_BACKUP) {
                        logger.info("Restoring task outputs to pre-compilation state")
                        taskOutputsBackup.restoreOutputs()
                    }
                }

                throw e
            } finally {
                taskOutputsBackup?.deleteSnapshot()
            }
        }
    }

    internal interface IncrementalFacadeRunnerParameters : WorkParameters {
        val compilerWorkArguments: Property<GradleKotlinCompilerWorkArguments>
        val taskOutputsToRestore: ListProperty<File>
        val snapshotsDir: DirectoryProperty
        val buildDir: DirectoryProperty
        val metricsReporter: Property<BuildMetricsReporter>
    }
}

private class LimitedScopeClassLoaderDelegator(
    private val parent: ClassLoader,
    fallback: ClassLoader,
    private val allowedClasses: Set<String>,
) : ClassLoader(fallback) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return if (name in allowedClasses) {
            parent.loadClass(name)
        } else {
            super.loadClass(name, resolve)
        }
    }
}