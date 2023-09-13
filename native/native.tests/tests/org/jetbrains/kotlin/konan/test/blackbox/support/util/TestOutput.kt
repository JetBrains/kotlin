/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import jetbrains.buildServer.messages.serviceMessages.*
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import java.text.ParseException
import org.jetbrains.kotlin.konan.test.blackbox.support.util.TCTestReportParseState as State

internal class TestReport(
    val passedTests: Collection<TestName>,
    val failedTests: Collection<TestName>,
    val ignoredTests: Collection<TestName>
) {
    fun isEmpty(): Boolean = passedTests.isEmpty() && failedTests.isEmpty() && ignoredTests.isEmpty()
}

internal interface TestOutputFilter {
    fun filter(testOutput: String): FilteredOutput

    data class FilteredOutput(val filteredOutput: String, val testReport: TestReport?)

    companion object {
        val NO_FILTERING = object : TestOutputFilter {
            override fun filter(testOutput: String) = FilteredOutput(testOutput, null)
        }
    }
}

/**
 * Processed TC test reports like this:
 *
 *   ##teamcity[testSuiteStarted name='sample.test.SampleTestKt' locationHint='ktest:suite://sample.test.SampleTestKt']
 *   ##teamcity[testStarted name='one' locationHint='ktest:test://sample.test.SampleTestKt.one']
 *   ##teamcity[testFinished name='one' duration='0']
 *   ##teamcity[testIgnored name='two']
 *   ##teamcity[testSuiteFinished name='sample.test.SampleTestKt']
 */
internal object TCTestOutputFilter : TestOutputFilter {
    override fun filter(testOutput: String): TestOutputFilter.FilteredOutput {
        val callback = TCTestMessageParserCallback()
        ServiceMessagesParser().parse(testOutput, callback)
        callback.finish()

        assertTrue(callback.errors.isEmpty()) {
            buildString {
                appendLine("Failed to parse TC test output.")
                callback.errors.forEach { error ->
                    appendLine()
                    append("Error: ").appendLine(error)
                }
                appendLine()
                appendLine("Full test output:")
                append(testOutput)
            }
        }

        return TestOutputFilter.FilteredOutput(
            filteredOutput = callback.nonTestOutput.toString(),
            testReport = TestReport(callback.passedTests, callback.failedTests, callback.ignoredTests)
        )
    }
}

private class TCTestMessageParserCallback : ServiceMessageParserCallback {
    private var afterMessage = false
    private var state: State = State.Begin

    val passedTests = mutableListOf<TestName>()
    val failedTests = mutableListOf<TestName>()
    val ignoredTests = mutableListOf<TestName>()

    val nonTestOutput = StringBuilder()
    val errors = mutableListOf<String>()

    override fun regularText(text: String) {
        val actualText = if (afterMessage) {
            if (text.startsWith("\r\n"))
                text.removePrefix("\r\n")
            else
                text.removePrefix("\n")
        } else
            text

        nonTestOutput.append(actualText)
        afterMessage = false
    }

    override fun serviceMessage(message: ServiceMessage) {
        fun unexpectedMessage(): State {
            addError {
                """
                    Unexpected TC test message: "$message"
                    State: $state
                """.trimIndent()
            }
            return state
        }

        state = when (message) {
            is TestSuiteStarted -> when (state) {
                is State.Begin,
                is State.TestSuiteFinished -> State.TestSuiteStarted(message.suiteName)
                else -> unexpectedMessage()
            }
            is TestSuiteFinished -> when (state) {
                is State.TestSuiteStarted,
                is State.TestIgnored,
                is State.TestFinished -> State.TestSuiteFinished
                else -> unexpectedMessage()
            }
            is TestIgnored -> when (state) {
                is State.TestSuiteStarted,
                is State.TestIgnored,
                is State.TestFinished -> State.TestIgnored(state.testSuite, message.simpleTestName).also { ignoredTests += it.testName }
                else -> unexpectedMessage()
            }
            is TestStarted -> when (state) {
                is State.TestSuiteStarted,
                is State.TestIgnored,
                is State.TestFinished -> State.TestStarted(state.testSuite, message.simpleTestName)
                else -> unexpectedMessage()
            }
            is TestFailed -> when (val s = state) {
                is State.TestStarted -> {
                    nonTestOutput.append(message.stacktrace)
                    State.TestFailed(s.testSuite, message.simpleTestName).also { failedTests += it.testName }
                }
                else -> unexpectedMessage()
            }
            is TestFinished -> when (state) {
                is State.TestStarted -> State.TestFinished(state.testSuite, message.simpleTestName).also { passedTests += it.testName }
                is State.TestFailed -> State.TestFinished(state.testSuite, message.simpleTestName)
                else -> unexpectedMessage()
            }
            else -> {
                addError { "Unsupported TC test message: $message" }
                state
            }
        }
        afterMessage = true
    }

    override fun parseException(e: ParseException, text: String) {
        errors += buildString {
            append("Failed to parse TC test message: \"").append(text).appendLine("\"")
            appendLine(e.toString())
        }
    }

    private fun addError(error: () -> String) {
        errors += error()
    }

    fun finish() {
        // The last test state is "TestStarted" this likely means that the test process terminated during test execution (SIGSEGV, etc).
        (state as? State.TestStarted)?.let { failedTests += it.testName }
    }
}

private sealed interface TCTestReportParseState {
    object Begin : State

    class TestSuiteStarted(val testSuiteName: String) : State
    object TestSuiteFinished : State

    sealed class TestState(val testSuite: TestSuiteStarted, val simpleTestName: String) : State {
        val testName: TestName get() = TestName("${testSuite.testSuiteName}.$simpleTestName")
    }

    class TestIgnored(testSuite: TestSuiteStarted, simpleTestName: String) : TestState(testSuite, simpleTestName)
    class TestStarted(testSuite: TestSuiteStarted, simpleTestName: String) : TestState(testSuite, simpleTestName)
    class TestFailed(testSuite: TestSuiteStarted, simpleTestName: String) : TestState(testSuite, simpleTestName)
    class TestFinished(testSuite: TestSuiteStarted, simpleTestName: String) : TestState(testSuite, simpleTestName)
}

private inline val State.testSuite: State.TestSuiteStarted
    get() = if (this is State.TestSuiteStarted) this else (this as State.TestState).testSuite

private inline val BaseTestMessage.simpleTestName get() = testName
