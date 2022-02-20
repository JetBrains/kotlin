package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.compilerRunner.KotlinLogger
import org.jetbrains.kotlin.gradle.internal.tasks.TaskWithLocalState
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.incremental.cleanDirectoryContents
import org.jetbrains.kotlin.incremental.deleteRecursivelyOrThrow
import java.io.File

fun throwGradleExceptionIfError(
    exitCode: ExitCode,
    executionStrategy: KotlinCompilerExecutionStrategy
) {
    when (exitCode) {
        ExitCode.COMPILATION_ERROR -> throw GradleException("Compilation error. See log for more details")
        ExitCode.INTERNAL_ERROR -> throw GradleException("Internal compiler error. See log for more details")
        ExitCode.SCRIPT_EXECUTION_ERROR -> throw GradleException("Script execution error. See log for more details")
        ExitCode.OOM_ERROR -> {
            var exceptionMessage = "Not enough memory to run compilation."
            when (executionStrategy) {
                KotlinCompilerExecutionStrategy.DAEMON ->
                    exceptionMessage += " Try to increase it via 'gradle.properties':\nkotlin.daemon.jvmargs=-Xmx<size>"
                KotlinCompilerExecutionStrategy.IN_PROCESS ->
                    exceptionMessage += " Try to increase it via 'gradle.properties':\norg.gradle.jvmargs=-Xmx<size>"
                KotlinCompilerExecutionStrategy.OUT_OF_PROCESS -> Unit
            }
            throw GradleException(exceptionMessage)
        }
        ExitCode.OK -> Unit
        else -> throw IllegalStateException("Unexpected exit code: $exitCode")
    }
}

internal fun TaskWithLocalState.cleanOutputsAndLocalState(reason: String? = null) {
    val log = GradleKotlinLogger(logger)
    cleanOutputsAndLocalState(allOutputFiles(), log, metrics.get(), reason)
}

internal fun cleanOutputsAndLocalState(
    outputFiles: Iterable<File>,
    log: KotlinLogger,
    metrics: BuildMetricsReporter,
    reason: String? = null
) {
    log.kotlinDebug {
        val suffix = reason?.let { " ($it)" }.orEmpty()
        "Cleaning output$suffix:"
    }

    metrics.measure(BuildTime.CLEAR_OUTPUT) {
        for (file in outputFiles) {
            when {
                file.isDirectory -> {
                    log.debug("Deleting contents of output directory: $file")
                    file.cleanDirectoryContents()
                }
                file.isFile -> {
                    log.debug("Deleting output file: $file")
                    file.deleteRecursivelyOrThrow()
                }
            }
        }
    }
}
