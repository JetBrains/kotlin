package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.jetbrains.kotlin.cli.common.ExitCode

fun throwGradleExceptionIfError(exitCode: ExitCode) {
    when (exitCode) {
        ExitCode.COMPILATION_ERROR -> throw GradleException("Compilation error. See log for more details")
        ExitCode.INTERNAL_ERROR -> throw GradleException("Internal compiler error. See log for more details")
        ExitCode.SCRIPT_EXECUTION_ERROR -> throw GradleException("Script execution error. See log for more details")
        ExitCode.OK -> {}
        else -> throw IllegalStateException("Unexpected exit code: $exitCode")
    }
}