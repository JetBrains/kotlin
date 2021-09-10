package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.compilerRunner.KotlinLogger
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.internal.tasks.TaskWithLocalState
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import java.io.File

fun throwGradleExceptionIfError(exitCode: ExitCode) {
    when (exitCode) {
        ExitCode.COMPILATION_ERROR -> throw GradleException("Compilation error. See log for more details")
        ExitCode.INTERNAL_ERROR -> throw GradleException("Internal compiler error. See log for more details")
        ExitCode.SCRIPT_EXECUTION_ERROR -> throw GradleException("Script execution error. See log for more details")
        ExitCode.OK -> {
        }
        else -> throw IllegalStateException("Unexpected exit code: $exitCode")
    }
}

internal fun TaskWithLocalState.clearLocalState(reason: String? = null) {
    val log = GradleKotlinLogger(logger)
    clearLocalState(allOutputFiles(), log, metrics.get(), reason)
}

internal fun clearLocalState(
    outputFiles: Iterable<File>,
    log: KotlinLogger,
    metrics: BuildMetricsReporter,
    reason: String? = null
) {
    log.kotlinDebug {
        val suffix = reason?.let { " ($it)" }.orEmpty()
        "Clearing output$suffix:"
    }

    metrics.measure(BuildTime.CLEAR_OUTPUT) {
        for (file in outputFiles) {
            if (!file.exists()) continue
            when {
                file.isDirectory -> {
                    log.debug("Deleting output directory: $file")
                    file.deleteRecursively()
                    file.mkdirs()
                }
                file.isFile -> {
                    log.debug("Deleting output file: $file")
                    file.delete()
                }
            }
        }
    }
}
