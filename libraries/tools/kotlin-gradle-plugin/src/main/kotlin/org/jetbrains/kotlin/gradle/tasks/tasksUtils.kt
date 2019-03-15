package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.internal.tasks.TaskWithLocalState
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.kotlinDebug

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

    log.kotlinDebug {
        val suffix = reason?.let { " ($it)" }.orEmpty()
        "Clearing output$suffix:"
    }

    for (file in allOutputFiles()) {
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
