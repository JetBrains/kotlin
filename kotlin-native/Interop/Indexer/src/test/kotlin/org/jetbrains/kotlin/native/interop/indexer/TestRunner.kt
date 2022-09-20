package org.jetbrains.kotlin.native.interop.indexer

import kotlinx.coroutines.*
import org.jetbrains.kotlin.native.interop.indexer.UnfilteredProcessOutput.Companion.launchReader
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.opentest4j.TestAbortedException
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.time.*

class TestRunner(private val programArgs: List<String>, private val directory: java.io.File) {

    fun run() = try {
        val run = buildRun()
        val runResult = run.run()
        if (runResult.exitCode != 0) {
            println("programArgs: $programArgs")
            println("directory: $directory")
            println("runResult.exitCode: ${runResult.exitCode}")
            println("runResult.processOutput.stdOut: ${runResult.processOutput.stdOut}")
            println("runResult.processOutput.stdErr: ${runResult.processOutput.stdErr}")
        }
        assertEquals(0, runResult.exitCode)
    } catch (t: Throwable) {
        when (t) {
            is AssertionError, is TestAbortedException -> throw t
            else -> {
                handleUnexpectedFailure(t)
                throw t
            }
        }
    }
    private fun buildResultHandler(runResult: RunResult) = ResultHandler(runResult)

    private fun handleUnexpectedFailure(t: Throwable) {
        println("Test execution failed with unexpected exception. ${t.message}")
    }

    @OptIn(ExperimentalTime::class)
    internal fun buildRun() = AbstractRun {
        runBlocking(Dispatchers.IO) {
            val unfilteredOutput = UnfilteredProcessOutput()
            val unfilteredOutputReader: Job

            val executionTimeout: Duration = Duration.parse("2000ms")

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

    private inner class ResultHandler(val runResult: RunResult) {
//        override fun getLoggedRun() = LoggedData.TestRun(getLoggedParameters(), runResult)

        fun handle() {
            verifyTestReport(runResult.processOutput.stdOut.testReport)
        }

        private fun verifyTestReport(testReport: TestReport?) {
            if (testReport == null) return

            assertTrue(!testReport.isEmpty()) { "No tests have been found." }

//            testRun.runParameters.get<TestRunParameter.WithFilter> {
//                verifyNoSuchTests(
//                        testReport.passedTests.filter { testName -> !testMatches(testName) },
//                        "Excessive tests have been executed"
//                )
//
//                verifyNoSuchTests(
//                        testReport.ignoredTests.filter { testName -> !testMatches(testName) },
//                        "Excessive tests have been ignored"
//                )
//            }

            verifyNoSuchTests(testReport.failedTests, "There are failed tests")

            Assumptions.assumeFalse(testReport.ignoredTests.isNotEmpty() && testReport.passedTests.isEmpty(), "Test case is disabled")
        }

        private fun verifyNoSuchTests(tests: Collection<String>, subject: String) = assertTrue(tests.isEmpty()) {
            buildString {
                append(subject).append(':')
                tests.forEach { appendLine().append(" - ").append(it) }
            }
        }
    }

}

internal fun interface AbstractRun {
    fun run(): RunResult
}

private class UnfilteredProcessOutput {
    private val stdOut = ByteArrayOutputStream()
    private val stdErr = ByteArrayOutputStream()

    fun toProcessOutput(outputFilter: TestOutputFilter): ProcessOutput {
        val testOutput = stdOut.toString()
        val stdErr1 = stdErr.toString()
        return ProcessOutput(
                stdOut = outputFilter.filter(testOutput),
                stdErr = stdErr1
        )
    }

    companion object {
        fun CoroutineScope.launchReader(unfilteredOutput: UnfilteredProcessOutput, process: Process): Job = launch {
            launch { process.inputStream.copyTo(unfilteredOutput.stdOut) }
            launch { process.errorStream.copyTo(unfilteredOutput.stdErr) }
        }
    }
}

internal data class RunResult(
        val exitCode: Int?,
        val timeout: Duration,
        val duration: Duration,
        val hasFinishedOnTime: Boolean,
        val processOutput: ProcessOutput
) {
    init {
        // null exit code is possible only when test run hasn't finished on time.
        check(exitCode != null || !hasFinishedOnTime)
    }
}

internal class ProcessOutput(val stdOut: TestOutputFilter.FilteredOutput, val stdErr: String)

internal interface TestOutputFilter {
    fun filter(testOutput: String): FilteredOutput

    data class FilteredOutput(val filteredOutput: String, val testReport: TestReport?)

    companion object {
        val NO_FILTERING = object : TestOutputFilter {
            override fun filter(testOutput: String) = FilteredOutput(testOutput, null)
        }
    }
}

internal class TestReport(
        val passedTests: Collection<String>,
        val failedTests: Collection<String>,
        val ignoredTests: Collection<String>
) {
    fun isEmpty(): Boolean = passedTests.isEmpty() && failedTests.isEmpty() && ignoredTests.isEmpty()
}
//internal class TestRun(
//        val displayName: String,
//        val executable: TestExecutable,
//        val runParameters: List<TestRunParameter>,
//        val testCaseId: TestCaseId,
//        val checks: TestRunChecks
//)