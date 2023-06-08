package org.jetbrains.kotlin.gradle.tasks

import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.compilerRunner.KotlinLogger
import org.jetbrains.kotlin.gradle.internal.tasks.TaskWithLocalState
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import org.jetbrains.kotlin.incremental.deleteRecursivelyOrThrow
import java.io.File

/** Throws [FailedCompilationException] if compilation completed with [exitCode] != [ExitCode.OK]. */
fun throwExceptionIfCompilationFailed(
    exitCode: ExitCode,
    executionStrategy: KotlinCompilerExecutionStrategy
) {
    when (exitCode) {
        ExitCode.COMPILATION_ERROR -> throw CompilationErrorException("Compilation error. See log for more details")
        ExitCode.INTERNAL_ERROR -> throw FailedCompilationException("Internal compiler error. See log for more details")
        ExitCode.SCRIPT_EXECUTION_ERROR -> throw FailedCompilationException("Script execution error. See log for more details")
        ExitCode.OOM_ERROR -> {
            val exceptionMessage = when (executionStrategy) {
                KotlinCompilerExecutionStrategy.DAEMON -> kotlinDaemonOOMHelperMessage
                KotlinCompilerExecutionStrategy.IN_PROCESS -> kotlinInProcessOOMHelperMessage
                KotlinCompilerExecutionStrategy.OUT_OF_PROCESS -> kotlinOutOfProcessOOMHelperMessage
            }
            throw OOMErrorException(exceptionMessage)
        }
        ExitCode.OK -> Unit
        else -> throw IllegalStateException("Unexpected exit code: $exitCode")
    }
}

internal const val kotlinDaemonOOMHelperMessage = "Not enough memory to run compilation. " +
        "Try to increase it via 'gradle.properties':\nkotlin.daemon.jvmargs=-Xmx<size>"

internal const val kotlinInProcessOOMHelperMessage = "Not enough memory to run compilation. " +
        " Try to increase it via 'gradle.properties':\norg.gradle.jvmargs=-Xmx<size>"

internal const val kotlinOutOfProcessOOMHelperMessage = "Not enough memory to run compilation."

private const val kotlinDaemonCrashedMessage =
    "Connection to the Kotlin daemon has been unexpectedly lost. This might be caused by the daemon being killed by another process or the operating system, or by JVM crash."

internal fun Throwable.hasOOMCause(): Boolean = when (cause) {
    is OutOfMemoryError -> true
    else -> cause?.hasOOMCause() ?: false
}

/** Exception thrown when [ExitCode] != [ExitCode.OK]. */
internal open class FailedCompilationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** Exception thrown when [ExitCode] == [ExitCode.COMPILATION_ERROR]. */
internal class CompilationErrorException(message: String) : FailedCompilationException(message)

/** Exception thrown when [ExitCode] == [ExitCode.OOM_ERROR]. */
internal class OOMErrorException(message: String) : FailedCompilationException(message)

/** Exception thrown when during the compilation [java.rmi.RemoteException] is caught */
internal class DaemonCrashedException(cause: Throwable) : FailedCompilationException(kotlinDaemonCrashedMessage, cause)

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
                    file.deleteDirectoryContents()
                }
                file.isFile -> {
                    log.debug("Deleting output file: $file")
                    file.deleteRecursivelyOrThrow()
                }
            }
        }
    }
}
