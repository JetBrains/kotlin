/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtilRt
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.needSmallBinary
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.OptimizationMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestOutputFilter
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TestReport
import org.jetbrains.kotlin.native.executors.RunProcessResult
import org.jetbrains.kotlin.native.executors.runProcess
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import org.jetbrains.kotlin.utils.yieldIfNotNull
import org.junit.jupiter.api.Assumptions
import java.io.File
import kotlin.time.Duration

sealed interface TestRunCheck {

    fun apply(testRun: TestRun, runResult: RunResult): Result

    sealed interface Result {
        data object Passed : Result
        data class Failed(val reason: String) : Result
    }

    sealed class ExecutionTimeout(val timeout: Duration) : TestRunCheck {
        class ShouldNotExceed(timeout: Duration) : ExecutionTimeout(timeout) {
            override fun apply(testRun: TestRun, runResult: RunResult): Result =
                if (!runResult.hasFinishedOnTime)
                    Result.Failed("Timeout exceeded during test execution.")
                else Result.Passed
        }

        class ShouldExceed(timeout: Duration) : ExecutionTimeout(timeout) {
            override fun apply(testRun: TestRun, runResult: RunResult): Result =
                if (runResult.hasFinishedOnTime)
                    Result.Failed("Test is expected to fail with exceeded timeout, which hasn't happened.")
                else Result.Passed
        }

        // When we want the execution to stop on reaching the timeout, but it's not a failure.
        // For example: stress tests, whose only check is that the execution did not crash.
        class MayExceed(timeout: Duration) : ExecutionTimeout(timeout) {
            override fun apply(testRun: TestRun, runResult: RunResult): Result {
                return Result.Passed
            }
        }
    }

    sealed class ExitCode : TestRunCheck {
        data object AnyNonZero : ExitCode()

        data class Expected(val expectedExitCode: Int) : ExitCode()

        override fun apply(testRun: TestRun, runResult: RunResult): Result {
            // Don't check the exit code if it is unknown.
            val knownExitCode: Int = runResult.exitCode ?: return Result.Passed

            return when (this) {
                AnyNonZero -> {
                    if (knownExitCode == 0)
                        Result.Failed("Test exited with zero code, which wasn't expected.")
                    else Result.Passed
                }
                is Expected -> {
                    if (knownExitCode != expectedExitCode)
                        Result.Failed("Exit code is $knownExitCode while $expectedExitCode was expected.")
                    else Result.Passed
                }
            }
        }
    }

    enum class Output {
        STDOUT,
        STDERR,

        /**
         * [STDOUT] followed by [STDERR]
         */
        ALL,
    }

    fun RunResult.processOutputAsString(output: Output) = when (output) {
        Output.STDOUT -> processOutput.stdOut.filteredOutput
        Output.STDERR -> processOutput.stdErr
        Output.ALL -> processOutput.stdOut.filteredOutput + processOutput.stdErr
    }

    class OutputDataFile(val output: Output = Output.ALL, val file: File) : TestRunCheck {
        override fun apply(testRun: TestRun, runResult: RunResult): Result {
            val expectedOutput = file.readText()
            val actualFilteredOutput = runResult.processOutputAsString(output)

            return if (StringUtilRt.convertLineSeparators(expectedOutput) != StringUtilRt.convertLineSeparators(actualFilteredOutput))
                Result.Failed("Tested process output mismatch. See \"TEST STDOUT\" and \"EXPECTED OUTPUT DATA FILE\" below.")
            else Result.Passed
        }
    }

    class OutputMatcher(val output: Output = Output.ALL, val match: (String) -> Boolean) : TestRunCheck {
        override fun apply(testRun: TestRun, runResult: RunResult): Result =
            try {
                if (!match(runResult.processOutputAsString(output))) {
                    Result.Failed("Tested process output has not passed validation.")
                } else Result.Passed
            } catch (t: Throwable) {
                if (t is Exception || t is AssertionError) {
                    Result.Failed("Tested process output has not passed validation: ${t.message}")
                } else {
                    throw t
                }
            }
    }

    class TestFiltering(val testOutputFilter: TestOutputFilter) : TestRunCheck {
        override fun apply(testRun: TestRun, runResult: RunResult): Result {
            if (testOutputFilter != TestOutputFilter.NO_FILTERING) {
                val testReport = runResult.processOutput.stdOut.testReport

                checkNotNull(testReport) { "TestRun has TestFiltering enabled, but test report is null" }

                if (testReport.isEmpty()) Result.Failed("No tests have been found. Test report is empty")

                testRun.runParameters.get<TestRunParameter.WithFilter> {
                    testReport.checkDisabled()

                    return listOf(
                        verifyNoSuchTests(
                            testReport.passedTests.filter { testName -> !testMatches(testName) },
                            "Excessive tests have been executed"
                        ),
                        verifyNoSuchTests(
                            testReport.ignoredTests.filter { testName -> !testMatches(testName) },
                            "Excessive tests have been ignored"
                        ),
                        verifyNoSuchTests(testReport.failedTests, "Failed tests found in the test report")
                    ).filterIsInstance<Result.Failed>().firstOrNull() ?: Result.Passed
                }

                testRun.runParameters.getAll<TestRunParameter.WithIgnoredTestFilter> {
                    Assumptions.assumeFalse(
                        testReport.ignoredTests.any { testName ->
                            testName.packageName == testRun.testCase.nominalPackageName
                        },
                        "Test case is disabled"
                    )

                    return listOf(
                        verifyNoSuchTests(
                            testReport.passedTests.filter { testName -> !testMatches(testName) },
                            "Ignored tests have been executed"
                        ),
                        verifyNoSuchTests(
                            testReport.failedTests.filter { testName ->
                                testName.packageName == testRun.testCase.nominalPackageName
                            },
                            "Test failure found in the test report"
                        )
                    ).filterIsInstance<Result.Failed>().firstOrNull() ?: Result.Passed
                }

                if (testRun.testCase.kind == TestKind.STANDALONE) {
                    testReport.checkDisabled()

                    return verifyNoSuchTests(testReport.failedTests, "Failed tests found in the test report")
                }

                if (!testRun.runParameters.has<TestRunParameter.WithFilter>()) {
                    testReport.checkDisabled()

                    return verifyNoSuchTests(
                        testReport.failedTests.filter { testName ->
                            testName.packageName == testRun.testCase.nominalPackageName
                        },
                        "Test ${testRun.testCase.id} failure found in the test report"
                    )
                }
            }
            return Result.Passed
        }

        private fun verifyNoSuchTests(tests: Collection<TestName>, subject: String): Result {
            return if (tests.isNotEmpty()) {
                Result.Failed(
                    buildString {
                        append(subject).append(':')
                        tests.forEach { appendLine().append(" - ").append(it) }
                    }
                )
            } else Result.Passed
        }

        private fun TestReport.checkDisabled() {
            Assumptions.assumeFalse(
                ignoredTests.isNotEmpty() && passedTests.isEmpty(),
                "Test case is disabled"
            )
        }
    }

    class FileCheckMatcher(val settings: Settings, val testDataFile: File) : TestRunCheck {
        val prefixes: String
            get() {
                val testTarget = settings.get<KotlinNativeTargets>().testTarget
                val optimizationMode = settings.get<OptimizationMode>()
                val checkPrefixes = buildList {
                    add("CHECK")
                    add("CHECK-${testTarget.abiInfoString}")
                    add("CHECK-${testTarget.name.toUpperCaseAsciiOnly()}")
                    if (testTarget.family.isAppleFamily) {
                        add("CHECK-APPLE")
                    }
                    if (testTarget.needSmallBinary() || optimizationMode == OptimizationMode.DEBUG
                        || settings.get<ExplicitBinaryOptions>().getOrNull<Boolean>(BinaryOptions.smallBinary) == true) {
                        add("CHECK-SMALLBINARY")
                    } else {
                        add("CHECK-BIGBINARY")
                    }
                }
                val optMode = when (optimizationMode) {
                    OptimizationMode.NO, OptimizationMode.DEBUG -> "DEBUG" // generated LLVM bitcode should be the same; split them up if this stops being the case.
                    OptimizationMode.OPT -> "OPT"
                }
                val checkPrefixesWithOptMode = checkPrefixes.map { "$it-$optMode" }
                val cacheMode = settings.get<CacheMode>().alias
                val checkPrefixesWithCacheMode = checkPrefixes.map { "$it-CACHE_$cacheMode" }
                return (checkPrefixes + checkPrefixesWithOptMode + checkPrefixesWithCacheMode).joinToString(",")
            }

        // Shameless borrowing `val KonanTarget.abiInfo` from module `:kotlin-native:backend.native`, which cannot be imported here for now.
        private val KonanTarget.abiInfoString: String
            get() = when {
                this == KonanTarget.MINGW_X64 -> "WINDOWSX64"
                !family.isAppleFamily && architecture == Architecture.ARM64 -> "AAPCS"
                else -> "DEFAULTABI"
            }

        override fun apply(testRun: TestRun, runResult: RunResult): Result {
            val fileCheckDump = runResult.testExecutable.executable.fileCheckDump!!
            val result = doFileCheck(fileCheckDump)

            return if (!(result.stdout.isEmpty() && result.stderr.isEmpty())) {
                val shortOutText = result.stdout.lines().take(100)
                val shortErrText = result.stderr.lines().take(100)

                Result.Failed(
                    """
                    FileCheck matching of ${fileCheckDump.absolutePath}
                    with '--check-prefixes $prefixes'
                    failed with result=$result:
                    ${shortOutText.joinToString("\n")}
                    ${shortErrText.joinToString("\n")}
                    """.trimIndent()
                )
            } else Result.Passed
        }

        internal fun doFileCheck(fileCheckDump: File): RunProcessResult {
            val fileCheckExecutable = settings.configurables.absoluteLlvmHome + File.separator + "bin" + File.separator +
                    if (SystemInfo.isWindows) "FileCheck.exe" else "FileCheck"
            require(File(fileCheckExecutable).exists()) {
                "$fileCheckExecutable does not exist. Make sure Distribution for `settings.configurables` " +
                        "was created using `propertyOverrides` to specify development variant of LLVM instead of user variant."
            }
            return try {
                runProcess(
                    fileCheckExecutable,
                    testDataFile.absolutePath,
                    "--input-file",
                    fileCheckDump.absolutePath,
                    "--check-prefixes", prefixes,
                    "--allow-unused-prefixes",
                    "--allow-deprecated-dag-overlap" // TODO specify it via new test directive for `function_attributes_at_callsite.kt`
                )
            } catch (t: Throwable) {
                RunProcessResult(Duration.ZERO, "FileCheck utility failed:", t.toString())
            }
        }
    }
}

data class TestRunChecks(
    val executionTimeoutCheck: ExecutionTimeout,
    val testFiltering: TestFiltering,
    val exitCodeCheck: ExitCode?,
    val outputDataFile: OutputDataFile?,
    val outputMatcher: OutputMatcher?,
    val fileCheckMatcher: FileCheckMatcher?,
) : Iterable<TestRunCheck> {

    override fun iterator() = iterator {
        yield(executionTimeoutCheck)
        yield(testFiltering)
        yieldIfNotNull(exitCodeCheck)
        yieldIfNotNull(outputDataFile)
        yieldIfNotNull(outputMatcher)
        yieldIfNotNull(fileCheckMatcher)
    }

    companion object {
        // The most frequently used case:
        @Suppress("TestFunctionName")
        fun Default(timeout: Duration) = TestRunChecks(
            executionTimeoutCheck = ExecutionTimeout.ShouldNotExceed(timeout),
            testFiltering = TestFiltering(TestOutputFilter.NO_FILTERING),
            exitCodeCheck = ExitCode.Expected(0),
            outputDataFile = null,
            outputMatcher = null,
            fileCheckMatcher = null,
        )
    }
}
