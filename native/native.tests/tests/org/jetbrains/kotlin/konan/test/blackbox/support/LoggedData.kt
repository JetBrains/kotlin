/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.RunResult
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunParameter
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.get
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.util.SafeEnvVars
import org.jetbrains.kotlin.konan.test.blackbox.support.util.SafeProperties
import java.io.File
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * A piece of information that makes sense for the user, and that can be logged to a file or
 * displayed inside an error message in user-friendly way.
 *
 * Handles all the necessary formatting right inside of [computeText]. Caches the resulting text to avoid re-computation.
 */
internal abstract class LoggedData {
    private val text: String by lazy { computeText() }
    protected abstract fun computeText(): String
    final override fun toString() = text

    fun withErrorMessage(errorMessage: String, t: Throwable? = null): String = buildString {
        appendLine(errorMessage)
        appendLine()
        if (t != null) {
            appendLine(t.stackTraceToString())
            appendLine()
        }
        appendLine(this@LoggedData)
    }

    class JVMEnvironment : LoggedData() {
        private val properties = SafeProperties()

        override fun computeText() = buildString {
            appendLine("ENVIRONMENT VARIABLES:")
            SafeEnvVars.forEach { (name, safeValue) ->
                append("- ").append(name).append(" = ").appendLine(safeValue)
            }
            appendLine()
            appendLine("JVM PROPERTIES:")
            properties.forEach { (name, safeValue) ->
                append("- ").append(name).append(" = ").appendLine(safeValue)
            }
        }
    }

    class CompilerParameters(
        private val home: KotlinNativeHome,
        private val compilerArgs: Array<String>,
        private val sourceModules: Collection<TestModule>,
        private val environment: JVMEnvironment = JVMEnvironment() // Capture environment.
    ) : LoggedData() {
        private val testDataFiles: List<File>
            get() = buildList {
                sourceModules.forEach { module ->
                    if (module !is TestModule.Exclusive) return@forEach
                    this += (module.testCase.id as? TestCaseId.TestDataFile)?.file ?: return@forEach
                }
                sort()
            }

        override fun computeText() = buildString {
            appendArguments("COMPILER ARGUMENTS:", listOf(home.dir.resolve("bin/kotlinc-native").path) + compilerArgs)
            appendLine()
            appendLine(environment)

            val testDataFiles = testDataFiles
            if (testDataFiles.isNotEmpty()) {
                appendLine()
                appendList("TEST DATA FILES (COMPILED TOGETHER):", testDataFiles)
            }
        }
    }

    abstract class CompilerCall : LoggedData()

    class CInteropParameters(
        private val extraArgs: Array<String>,
        private val defFile: File,
        private val environment: JVMEnvironment = JVMEnvironment() // Capture environment.
    ) : LoggedData() {
        override fun computeText() = buildString {
            appendArguments("CINTEROP INVOCATION EXTRA ARGUMENTS:", extraArgs.toList())
            appendLine()
            appendLine(environment)

            appendLine()
            appendLine("TEST DEF FILE: ${defFile.canonicalPath}")
        }
    }

    class CompilationToolCall(
        private val toolName: String,
        private val parameters: LoggedData,
        val exitCode: ExitCode,
        val toolOutput: String,
        private val toolOutputHasErrors: Boolean,
        private val duration: Duration
    ) : CompilerCall() {
        override fun computeText(): String {
            val problems = listOfNotNull(
                "- Non-zero exit code".takeIf { exitCode != ExitCode.OK },
                "- Errors reported by the $toolName".takeIf { toolOutputHasErrors }
            )

            return buildString {
                if (problems.isNotEmpty()) {
                    appendLine("$toolName PROBLEMS:")
                    problems.forEach(::appendLine)
                    appendLine()
                }

                appendLine("$toolName CALL:")
                appendLine("- Exit code: ${exitCode.code} (${exitCode.name})")
                appendDuration(duration)
                appendLine()
                appendPotentiallyLargeOutput(toolOutput, "RAW $toolName OUTPUT", truncateLargeOutput = false)
                appendLine()
                appendLine(parameters)
            }
        }
    }

    class NoopCompilerCall(val artifactFile: File) : CompilerCall() {
        override fun computeText() = "No compiler call performed for external (given) artifact $artifactFile"
    }

    class CompilationToolCallUnexpectedFailure(parameters: LoggedData, throwable: Throwable) : UnexpectedFailure(parameters, throwable)

    class TestRunParameters(
        private val compilationToolCall: CompilerCall,
        private val testCaseId: TestCaseId?,
        private val runArgs: Iterable<String>,
        private val runParameters: List<TestRunParameter>?
    ) : LoggedData() {
        override fun computeText() = buildString {
            when {
                testCaseId is TestCaseId.TestDataFile -> {
                    appendLine("TEST DATA FILE:")
                    appendLine(testCaseId.file)
                    appendLine()
                }
                testCaseId != null -> {
                    appendLine("TEST CASE ID:")
                    appendLine(testCaseId)
                    appendLine()
                }
            }
            appendArguments("TEST RUN ARGUMENTS:", runArgs)
            appendLine()
            runParameters?.get<TestRunParameter.WithInputData> {
                appendLine("INPUT DATA FILE:")
                appendLine(inputDataFile)
                appendLine()
            }
            runParameters?.get<TestRunParameter.WithExpectedOutputData> {
                appendLine("EXPECTED OUTPUT DATA FILE:")
                appendLine(expectedOutputDataFile)
                appendLine()
            }
            appendLine(compilationToolCall)
        }
    }

    class TestRun(
        private val parameters: TestRunParameters,
        private val runResult: RunResult
    ) : LoggedData() {
        override fun computeText() = buildString {
            appendLine("TEST RUN:")
            appendLine("- Exit code: ${runResult.exitCode ?: "<unknown>"}")
            appendDuration(runResult.timeout, runResult.duration, runResult.hasFinishedOnTime)
            appendLine()
            appendPotentiallyLargeOutput(runResult.processOutput.stdOut.filteredOutput, "STDOUT", truncateLargeOutput = true)
            appendLine()
            appendPotentiallyLargeOutput(runResult.processOutput.stdErr, "STDERR", truncateLargeOutput = true)
            appendLine()
            appendLine(parameters)
        }
    }

    class TestRunUnexpectedFailure(parameters: TestRunParameters, throwable: Throwable) : UnexpectedFailure(parameters, throwable)

    abstract class UnexpectedFailure(
        private val parameters: LoggedData,
        private val throwable: Throwable
    ) : LoggedData() {
        override fun computeText() = buildString {
            appendLine("ERROR MESSAGE:")
            appendLine("${throwable.message}")
            appendLine()
            appendLine("STACK TRACE:")
            appendLine(throwable.stackTraceToString().trimEnd())
            appendLine()
            appendLine(parameters)
        }
    }

    companion object {
        protected fun StringBuilder.appendList(header: String, list: Iterable<Any?>): StringBuilder {
            appendLine(header)
            list.forEach(::appendLine)
            return this
        }

        protected fun StringBuilder.appendArguments(header: String, args: Iterable<String>): StringBuilder {
            appendLine(header)

            fun String.sanitize() = if (startsWith("--ktest_") && substringBefore('=').endsWith("_filter")) "'$this'" else this

            var lastArgIsOptionWithoutEqualsSign = false
            args.forEachIndexed { index, arg ->
                val isOption = arg[0] == '-'
                val isSourceFile = !isOption && arg.substringAfterLast('.') == "kt"
                if (index > 0) {
                    if (isOption || isSourceFile || !lastArgIsOptionWithoutEqualsSign)
                        append(" \\\n")
                    else
                        append(' ')
                }
                lastArgIsOptionWithoutEqualsSign = isOption && '=' !in arg
                append(arg.sanitize())
            }

            appendLine()
            return this
        }

        protected fun StringBuilder.appendDuration(timeout: Duration, duration: Duration, hasFinishedOnTime: Boolean) {
            append("- Max permitted duration: ").appendLine(timeout.toString(DurationUnit.SECONDS, 2))
            appendDuration(duration)
            appendLine("- Finished on time: $hasFinishedOnTime")
        }

        protected fun StringBuilder.appendDuration(duration: Duration) {
            append("- Duration: ").appendLine(duration.toString(DurationUnit.SECONDS, 2))
        }

        private fun StringBuilder.appendPotentiallyLargeOutput(output: String, subject: String, truncateLargeOutput: Boolean) {
            appendLine("========== BEGIN: $subject ==========")
            if (output.length > MAX_PRINTED_OUTPUT_LENGTH && truncateLargeOutput) {
                append(output.substring(0, MAX_PRINTED_OUTPUT_LENGTH).trimEnd()).appendLine("...")
                appendLine()
                appendLine("********** The output is too large (${output.length} characters in total), it has been truncated to avoid excessive logs **********")
            } else if (output.isNotEmpty()) {
                appendLine(output.trimEnd())
            }
            appendLine("========== END: $subject ==========")
        }

        private const val MAX_PRINTED_OUTPUT_LENGTH = 8 * 1024
    }
}
