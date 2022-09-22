package org.jetbrains.kotlin.konan.blackboxtest.support.runner

import kotlinx.coroutines.*
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.UnfilteredProcessOutput.Companion.launchReader
import org.jetbrains.kotlin.konan.blackboxtest.support.util.TestOutputFilter
import org.junit.jupiter.api.Assertions.assertTrue
import org.opentest4j.TestAbortedException
import kotlin.time.*

class InteropTestRunner(private val programArgs: List<String>, private val directory: java.io.File) {

    fun run() = try {
        val run = buildRun()
        val runResult = run.run()
        assertTrue(runResult.exitCode == 0) { "exitCode: runResult.exitCode\nstdOut: ${runResult.processOutput.stdOut}\nstderr: ${runResult.processOutput.stdErr}" }
    } catch (t: Throwable) {
        when (t) {
            is AssertionError, is TestAbortedException -> throw t
            else -> {
                handleUnexpectedFailure(t)
                throw t
            }
        }
    }

    private fun handleUnexpectedFailure(t: Throwable) {
        println("Test execution failed with unexpected exception. ${t.message}")
    }

    @OptIn(ExperimentalTime::class)
    internal fun buildRun() = AbstractRun {
        runBlocking(Dispatchers.IO) {
            val unfilteredOutput = UnfilteredProcessOutput()
            val unfilteredOutputReader: Job

            val executionTimeout: Duration = Duration.parse("10000ms")  // cinterop takes really long time

            val process: Process
            val hasFinishedOnTime: Boolean

            val duration = measureTime {
                process = ProcessBuilder(programArgs).directory(directory).start()

                unfilteredOutputReader = launchReader(unfilteredOutput, process)

                hasFinishedOnTime = process.waitFor(
                        executionTimeout.toLong(DurationUnit.MILLISECONDS),
                        DurationUnit.MILLISECONDS.toTimeUnit()
                )
            }

            val exitCode: Int? = if (hasFinishedOnTime) {
                unfilteredOutputReader.join() // Wait until all output streams are drained.
                process.exitValue()
            } else {
                try { // It could happen just by an accident that the process has exited by itself.
                    val exitCode = process.exitValue() // Fetch exit code.
                    unfilteredOutputReader.join() // Wait until all streams are drained.
                    exitCode
                } catch (_: IllegalThreadStateException) { // Still not destroyed.
                    unfilteredOutputReader.cancel() // Cancel it. No need to read streams, actually.
                    process.destroyForcibly() // kill -9
                    null
                }
            }

            RunResult(
                    exitCode = exitCode,
                    timeout = executionTimeout,
                    duration = duration,
                    hasFinishedOnTime = hasFinishedOnTime,
                    processOutput = unfilteredOutput.toProcessOutput(TestOutputFilter.NO_FILTERING)
            )
        }
    }
}

internal fun interface AbstractRun {
    fun run(): RunResult
}
