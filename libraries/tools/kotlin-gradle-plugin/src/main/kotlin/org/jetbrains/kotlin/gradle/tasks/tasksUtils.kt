package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.gradle.api.Task
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.compilerRunner.KotlinLogger
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
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

internal fun <T : Task> T.localStateDirectories(): List<File> =
    outputsCompatible.files.files.filter { it.isDirectory }

internal fun <T : Task> T.clearLocalStateDirectories(reason: String? = null) {
    clearLocalStateDirectories(GradleKotlinLogger(logger), localStateDirectories(), reason)
}

internal fun clearLocalStateDirectories(log: KotlinLogger, localStateDirectories: List<File>, reason: String?) {
    log.kotlinDebug {
        val suffix = reason?.let { " ($it)" }.orEmpty()
        "Clearing output directories$suffix:"
    }
    for (dir in localStateDirectories) {
        if (!dir.exists()) continue
        when {
            dir.isDirectory -> {
                dir.deleteRecursively()
                dir.mkdirs()
                log.kotlinDebug { "  deleted $dir" }
            }
            else -> log.kotlinDebug { "  skipping $dir (not a directory)" }
        }
    }
}