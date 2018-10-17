package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.gradle.api.Task
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.gradle.utils.outputsCompatible
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

internal val <T : Task> T.outputDirectories: List<File>
    get() = outputsCompatible.files.files.filter { it.isDirectory }

internal fun <T : Task> T.clearOutputDirectories(reason: String? = null) {
    logger.kotlinDebug {
        val suffix = reason?.let { " ($it)" }.orEmpty()
        "Clearing output directories for task '$path'$suffix:"
    }
    val outputDirectories = outputDirectories
    for (dir in outputDirectories) {
        when {
            dir.isDirectory -> {
                dir.deleteRecursively()
                dir.mkdirs()
                logger.kotlinDebug { "  deleted $dir" }
            }
            else -> logger.kotlinDebug { "  skipping $dir (not a directory)" }
        }
    }
}