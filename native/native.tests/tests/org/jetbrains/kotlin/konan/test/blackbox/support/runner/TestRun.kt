/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.runner

import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.Executable
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Success
import org.jetbrains.kotlin.konan.test.blackbox.support.util.DumpedTestListing
import org.jetbrains.kotlin.konan.test.blackbox.support.util.startsWith
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.io.IOException

internal class TestExecutable(
    val executable: Executable,
    val loggedCompilationToolCall: LoggedData.CompilerCall,
    val testNames: Collection<TestName>
) {
    companion object {
        fun fromCompilationResult(testCase: TestCase, compilationResult: Success<out Executable>): TestExecutable {
            val testNames = when (testCase.kind) {
                TestKind.REGULAR, TestKind.STANDALONE -> {
                    val testDumpFile = compilationResult.resultingArtifact.testDumpFile
                    val testDump = try {
                        testDumpFile.readText()
                    } catch (e: IOException) {
                        fail { compilationResult.loggedData.withErrorMessage("Failed to read test dump file: $testDumpFile", e) }
                    }

                    try {
                        DumpedTestListing.parse(testDump)
                    } catch (e: Exception) {
                        fail { compilationResult.loggedData.withErrorMessage("Failed to parse test dump file: $testDumpFile", e) }
                    }
                }
                else -> emptyList()
            }

            return TestExecutable(
                executable = compilationResult.resultingArtifact,
                loggedCompilationToolCall = compilationResult.loggedData,
                testNames = testNames
            )
        }
    }
}

internal data class TestRun(
    val displayName: String,
    val executable: TestExecutable,
    val runParameters: List<TestRunParameter>,
    val testCase: TestCase,
    val checks: TestRunChecks,
    val expectedFailure: Boolean,
)

internal sealed interface TestRunParameter {
    fun applyTo(programArgs: MutableList<String>)

    sealed class WithFilter : TestRunParameter {
        abstract fun testMatches(testName: TestName): Boolean
    }

    class WithPackageFilter(private val packageName: PackageName) : WithFilter() {
        init {
            assertFalse(packageName.isEmpty())
        }

        override fun applyTo(programArgs: MutableList<String>) {
            programArgs += "--ktest_filter=$packageName.*"
        }

        override fun testMatches(testName: TestName) = testName.packageName.startsWith(packageName)
    }

    class WithTestFilter(val testName: TestName) : WithFilter() {
        override fun applyTo(programArgs: MutableList<String>) {
            programArgs += "--ktest_filter=$testName"
        }

        override fun testMatches(testName: TestName) = this.testName == testName
    }

    class WithIgnoredTestFilter(val testName: TestName) : WithFilter() {
        override fun applyTo(programArgs: MutableList<String>) {
            programArgs += "--ktest_filter=*-$testName"
        }

        override fun testMatches(testName: TestName) = this.testName != testName
    }

    class WithGTestPatterns(val positivePatterns: Set<String> = setOf("*"), val negativePatterns: Set<String>) : WithFilter() {
        val positiveRegexes = positivePatterns.map(::fromGTestPattern)
        val negativeRegexes = negativePatterns.map(::fromGTestPattern)

        override fun applyTo(programArgs: MutableList<String>) {
            "--ktest_filter=${positivePatterns.joinToString(":")}-${negativePatterns.joinToString(":")}"
        }

        override fun testMatches(testName: TestName): Boolean {
            val testNameStr = testName.toString()
            return positiveRegexes.any { it.matches(testNameStr) } &&
                    !negativeRegexes.any { it.matches(testNameStr) }
        }
    }

    object WithTCTestLogger : TestRunParameter {
        override fun applyTo(programArgs: MutableList<String>) {
            programArgs += "--ktest_logger=TEAMCITY"
            programArgs += "--ktest_no_exit_code"
        }
    }

    class WithInputData(val inputDataFile: File) : TestRunParameter {
        override fun applyTo(programArgs: MutableList<String>) = Unit
    }

    class WithLLDB(val commands: List<String>) : TestRunParameter {
        override fun applyTo(programArgs: MutableList<String>) {
            programArgs.add(0, "lldb")
            programArgs.addAll(commands)
        }
    }

    // Currently, used only for logging the data.
    class WithExpectedOutputData(val expectedOutputDataFile: File) : TestRunParameter {
        override fun applyTo(programArgs: MutableList<String>) = Unit
    }

    class WithFreeCommandLineArguments(val args: List<String>) : TestRunParameter {
        override fun applyTo(programArgs: MutableList<String>) {
            programArgs += args
        }
    }
}

internal inline fun <reified T : TestRunParameter> List<TestRunParameter>.has(): Boolean =
    firstIsInstanceOrNull<T>() != null

internal inline fun <reified T : TestRunParameter> List<TestRunParameter>.get(onFound: T.() -> Unit) {
    firstIsInstanceOrNull<T>()?.let(onFound)
}


// must be in sync with `fromGTestPattern(String)` in kotlin-native/runtime/src/main/kotlin/kotlin/native/internal/test/TestRunner.kt
internal fun fromGTestPattern(pattern: String): Regex {
    val result = StringBuilder()
    var prevIndex = 0
    pattern.forEachIndexed { index, c ->
        if (c == '*' || c == '?') {
            result.append(pattern.substringEscaped(prevIndex until index))
            prevIndex = index + 1
            result.append(if (c == '*') ".*" else ".")
        }
    }
    result.append(pattern.substringEscaped(prevIndex until pattern.length))
    return result.toString().toRegex()
}

private fun String.substringEscaped(range: IntRange) = this.substring(range).let { if (it.isNotEmpty()) Regex.escape(it) else "" }
