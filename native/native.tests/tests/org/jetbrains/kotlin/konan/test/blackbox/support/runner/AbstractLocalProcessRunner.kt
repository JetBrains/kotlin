/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtilRt.convertLineSeparators
import kotlinx.coroutines.*
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.AbstractRunner.AbstractRun
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.ExecutionTimeout
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.ExitCode
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.UnfilteredProcessOutput.Companion.launchReader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.OptimizationMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestOutputFilter
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import org.junit.jupiter.api.Assertions.fail
import java.io.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.*

internal abstract class AbstractLocalProcessRunner<R>(protected val checks: TestRunChecks) : AbstractRunner<R>() {
    protected abstract val visibleProcessName: String
    protected abstract val executable: TestExecutable
    protected abstract val programArgs: List<String>
    protected abstract val outputFilter: TestOutputFilter

    protected open fun customizeProcess(process: Process) = Unit

    @OptIn(ExperimentalTime::class)
    final override fun buildRun() = AbstractRun {
        runBlocking(Dispatchers.IO) {
            val unfilteredOutput = UnfilteredProcessOutput()
            val unfilteredOutputReader: Job

            val executionTimeout: Duration = checks.executionTimeoutCheck.timeout

            val process: Process
            val hasFinishedOnTime: Boolean

            // Don't ignore IO errors that happen just after the process is started.
            val ignoreIOErrorsInProcessOutput = AtomicBoolean(false)

            val duration = measureTime {
                process = ProcessBuilder(programArgs).directory(executable.executable.executableFile.parentFile).start()
                customizeProcess(process)

                unfilteredOutputReader = launchReader(
                    unfilteredOutput,
                    processStdout = process.inputStream,
                    processStderr = process.errorStream,
                    ignoreIOErrorsInProcessOutput
                )

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
                    ignoreIOErrorsInProcessOutput.set(true) // Ignore IO errors caused by the closed streams of the killed process.
                    unfilteredOutputReader.cancel() // Cancel it. No need to read streams, actually.
                    process.destroyForcibly() // kill -9
                    null
                }
            }

            RunResult(
                testExecutable = executable,
                exitCode = exitCode,
                timeout = executionTimeout,
                duration = duration,
                hasFinishedOnTime = hasFinishedOnTime,
                processOutput = unfilteredOutput.toProcessOutput(outputFilter)
            )
        }
    }

    abstract override fun buildResultHandler(runResult: RunResult): LocalResultHandler<R> // ?? Narrow returned type.
}

internal abstract class LocalResultHandler<R>(
    runResult: RunResult,
    private val visibleProcessName: String,
    protected val checks: TestRunChecks
) : AbstractResultHandler<R>(runResult) {
    override fun handle(): R {
        checks.forEach { check ->
            when (check) {
                is ExecutionTimeout.ShouldNotExceed -> verifyExpectation(runResult.hasFinishedOnTime) {
                    "Timeout exceeded during test execution."
                }
                is ExecutionTimeout.ShouldExceed -> verifyExpectation(!runResult.hasFinishedOnTime) {
                    "Test is expected to fail with exceeded timeout, which hasn't happened."
                }
                is TestRunCheck.ExpectedFailure -> {
                    val testReport = runResult.processOutput.stdOut.testReport
                    verifyExpectation(testReport != null) {
                        "testReport is expected to be non-null"
                    }
                    verifyExpectation(!testReport!!.isEmpty()) {
                        "testReport is expected to be non-empty"
                    }
                    verifyExpectation(testReport.failedTests.isNotEmpty()) {
                        "Test did not fail as expected"
                    }
                    verifyExpectation(testReport.passedTests.isEmpty()) {
                        "Test unexpectedly passed"
                    }
                }
                is ExitCode -> {
                    // Don't check exit code if it is unknown.
                    val knownExitCode: Int = runResult.exitCode ?: return@forEach
                    when (check) {
                        is ExitCode.Expected -> verifyExpectation(knownExitCode == check.expectedExitCode) {
                            "$visibleProcessName exit code is $knownExitCode while ${check.expectedExitCode} was expected."
                        }
                        is ExitCode.AnyNonZero -> verifyExpectation(knownExitCode != 0) {
                            "$visibleProcessName exited with zero code, which wasn't expected."
                        }
                    }
                }
                is TestRunCheck.OutputDataFile -> {
                    val expectedOutput = check.file.readText()
                    val actualFilteredOutput = runResult.processOutput.stdOut.filteredOutput + runResult.processOutput.stdErr

                    // Don't use verifyExpectation(expected, actual) to avoid exposing potentially large test output in exception message
                    // and blowing up test logs.
                    verifyExpectation(convertLineSeparators(expectedOutput) == convertLineSeparators(actualFilteredOutput)) {
                        "Tested process output mismatch. See \"TEST STDOUT\" and \"EXPECTED OUTPUT DATA FILE\" below."
                    }
                }
                is TestRunCheck.OutputMatcher -> {
                    try {
                        verifyExpectation(check.match(runResult.processOutput.stdOut.filteredOutput)) {
                            "Tested process output has not passed validation."
                        }
                    } catch (t: Throwable) {
                        if (t is Exception || t is AssertionError) {
                            fail<Nothing>(
                                getLoggedRun().withErrorMessage("Tested process output has not passed validation: " + t.message),
                                t
                            )
                        } else {
                            throw t
                        }
                    }
                }
                is TestRunCheck.FileCheckMatcher -> {
                    val fileCheckExecutable = check.settings.configurables.absoluteLlvmHome + File.separator + "bin" + File.separator +
                            if (SystemInfo.isWindows) "FileCheck.exe" else "FileCheck"
                    require(File(fileCheckExecutable).exists()) {
                        "$fileCheckExecutable does not exist. Make sure Distribution for `settings.configurables` " +
                                "was created using `propertyOverrides` to specify development variant of LLVM instead of user variant."
                    }
                    val fileCheckDump = runResult.testExecutable.executable.fileCheckDump!!
                    val fileCheckOut = File(fileCheckDump.absolutePath + ".out")
                    val fileCheckErr = File(fileCheckDump.absolutePath + ".err")

                    val testTarget = check.settings.get<KotlinNativeTargets>().testTarget
                    val checkPrefixes = buildList {
                        add("CHECK")
                        add("CHECK-${testTarget.abiInfoString}")
                        add("CHECK-${testTarget.name.toUpperCaseAsciiOnly()}")
                        if (testTarget.family.isAppleFamily) {
                            add("CHECK-APPLE")
                        }
                    }
                    val optimizationMode = check.settings.get<OptimizationMode>().name
                    val checkPrefixesWithOptMode = checkPrefixes.map { "$it-$optimizationMode" }
                    val commaSeparatedCheckPrefixes = (checkPrefixes + checkPrefixesWithOptMode).joinToString(",")

                    val result = ProcessBuilder(
                        fileCheckExecutable,
                        check.testDataFile.absolutePath,
                        "--input-file",
                        fileCheckDump.absolutePath,
                        "--check-prefixes", commaSeparatedCheckPrefixes,
                        "--allow-deprecated-dag-overlap" // TODO specify it via new test directive for `function_attributes_at_callsite.kt`
                    ).redirectOutput(fileCheckOut)
                        .redirectError(fileCheckErr)
                        .start()
                        .waitFor()
                    val errText = fileCheckErr.readText()
                    val outText = fileCheckOut.readText()
                    verifyExpectation(result == 0 && errText.isEmpty() && outText.isEmpty()) {
                        val shortOutText = outText.lines().take(100)
                        val shortErrText = errText.lines().take(100)
                        "FileCheck matching of ${fileCheckDump.absolutePath}\n" +
                                "with '--check-prefixes $commaSeparatedCheckPrefixes'\n" +
                                "failed with result=$result:\n" +
                                shortOutText.joinToString("\n") + "\n" +
                                shortErrText.joinToString("\n")
                    }
                }
            }
        }

        return doHandle()
    }

    protected abstract fun doHandle(): R
}

private class UnfilteredProcessOutput {
    private val stdOut = ByteArrayOutputStream()
    private val stdErr = ByteArrayOutputStream()

    fun toProcessOutput(outputFilter: TestOutputFilter): ProcessOutput = ProcessOutput(
        stdOut = outputFilter.filter(stdOut.toString(Charsets.UTF_8.name())),
        stdErr = stdErr.toString(Charsets.UTF_8.name())
    )

    companion object {
        fun CoroutineScope.launchReader(
            unfilteredOutput: UnfilteredProcessOutput,
            processStdout: InputStream,
            processStderr: InputStream,
            ignoreIOErrors: AtomicBoolean
        ): Job = launch {
            fun InputStream.safeCopyTo(output: OutputStream) {
                try {
                    copyTo(output)
                } catch (e: IOException) {
                    if (ignoreIOErrors.get()) { // Note: Just checking `!process.isAlive` seems to be not reliable in concurrent environment.
                        // The IO exception might be caused by the closed stream due to process death. Just ignore.
                    } else {
                        // The process is still alive. Some I/O error happened, which is better to rethrow.
                        throw e
                    }
                }
            }

            launch { processStdout.safeCopyTo(unfilteredOutput.stdOut) }
            launch { processStderr.safeCopyTo(unfilteredOutput.stdErr) }
        }
    }
}

// Shameless borrowing `val KonanTarget.abiInfo` from module `:kotlin-native:backend.native`, which cannot be imported here for now.
val KonanTarget.abiInfoString: String
    get() = when {
        this == KonanTarget.MINGW_X64 -> "WINDOWSX64"
        !family.isAppleFamily && architecture == Architecture.ARM64 -> "AAPCS"
        else -> "DEFAULTABI"
    }
