/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.cli.common.ExitCode
import java.io.File

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

    class CompilerParameters(
        private val compilerArgs: Array<String>,
        private val sourceModules: Collection<TestModule>
    ) : LoggedData() {
        private val testDataFiles: List<File>
            get() = sourceModules.asSequence()
                .filterIsInstance<TestModule.Exclusive>()
                .map { it.testCase.origin.testDataFile }
                .toMutableList()
                .apply { sort() }

        override fun computeText() = buildString {
            appendArguments("COMPILER ARGUMENTS:", listOf("\$\$kotlinc-native\$\$") + compilerArgs)
            appendLine()
            appendList("TEST DATA FILES (COMPILED TOGETHER):", testDataFiles)
        }
    }

    class CompilerCall(
        private val parameters: CompilerParameters,
        private val exitCode: ExitCode,
        private val compilerOutput: String,
        private val compilerOutputHasErrors: Boolean,
        private val durationMillis: Long
    ) : LoggedData() {
        override fun computeText(): String {
            val problems = listOfNotNull(
                "- Non-zero exit code".takeIf { exitCode != ExitCode.OK },
                "- Errors reported by the compiler".takeIf { compilerOutputHasErrors }
            )

            return buildString {
                if (problems.isNotEmpty()) {
                    appendLine("COMPILATION PROBLEMS:")
                    problems.forEach(::appendLine)
                    appendLine()
                }

                appendLine("COMPILER CALL:")
                appendLine("- Exit code: ${exitCode.code} (${exitCode.name})")
                appendDuration(durationMillis)
                appendLine()
                appendLine("========== BEGIN: RAW COMPILER OUTPUT ==========")
                if (compilerOutput.isNotEmpty()) appendLine(compilerOutput.trimEnd())
                appendLine("========== END: RAW COMPILER OUTPUT ==========")
                appendLine()
                appendLine(parameters)
            }
        }
    }

    class CompilerCallUnexpectedFailure(
        private val parameters: CompilerParameters,
        private val throwable: Throwable
    ) : LoggedData() {
        override fun computeText() = buildString {
            appendLine("UNEXPECTED FAILURE IN COMPILER:")
            appendLine("$throwable")
            appendLine()
            appendLine("STACK TRACE:")
            appendLine(throwable.stackTraceToString())
            appendLine()
            appendLine(parameters)
        }
    }

    class TestRunParameters(
        private val compilerCall: CompilerCall,
        private val origin: TestOrigin.SingleTestDataFile,
        private val runArgs: Iterable<String>,
        private val runParameters: List<TestRunParameter>
    ) : LoggedData() {
        override fun computeText() = buildString {
            appendLine("TEST DATA FILE:")
            appendLine(origin.testDataFile)
            appendLine()
            appendArguments("TEST RUN ARGUMENTS:", runArgs)
            appendLine()
            runParameters.get<TestRunParameter.WithInputData> {
                appendLine("INPUT DATA FILE:")
                appendLine(inputDataFile)
                appendLine()
            }
            runParameters.get<TestRunParameter.WithExpectedOutputData> {
                appendLine("EXPECTED OUTPUT DATA FILE:")
                appendLine(expectedOutputDataFile)
                appendLine()
            }
            appendLine(compilerCall)
        }
    }

    class TestRun(
        private val parameters: TestRunParameters,
        private val exitCode: Int,
        private val stdOut: String,
        private val stdErr: String,
        private val durationMillis: Long
    ) : LoggedData() {
        override fun computeText() = buildString {
            appendLine("TEST RUN:")
            appendLine("- Exit code: $exitCode")
            appendDuration(durationMillis)
            appendLine()
            appendLine("========== BEGIN: TEST STDOUT ==========")
            if (stdOut.isNotEmpty()) appendLine(stdOut.trimEnd())
            appendLine("========== END: TEST STDOUT ==========")
            appendLine()
            appendLine("========== BEGIN: TEST STDERR ==========")
            if (stdErr.isNotEmpty()) appendLine(stdErr.trimEnd())
            appendLine("========== END: TEST STDERR ==========")
            appendLine()
            appendLine(parameters)
        }
    }

    companion object {
        @JvmStatic
        protected fun StringBuilder.appendList(header: String, list: Iterable<Any?>): StringBuilder {
            appendLine(header)
            list.forEach(::appendLine)
            return this
        }

        @JvmStatic
        protected fun StringBuilder.appendArguments(header: String, args: Iterable<String>): StringBuilder {
            appendLine(header)

            fun String.sanitize() = if (startsWith("--ktest_filter")) "'$this'" else this

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

        protected fun StringBuilder.appendDuration(durationMillis: Long): StringBuilder =
            append("- Duration: ").append(String.format("%.2f", durationMillis.toDouble() / 1000)).appendLine(" seconds")
    }
}
