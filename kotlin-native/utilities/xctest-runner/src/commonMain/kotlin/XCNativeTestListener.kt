/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import kotlin.native.internal.test.*
import kotlin.time.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import platform.darwin.NSObject
import platform.XCTest.*

/**
 * XCTest observation.
 *
 * Logs tests and notifies listeners set with [testSettings].
 * @see TestSettings
 */
internal class XCNativeTestListener(private val testSettings: TestSettings) : NSObject(), XCTestObservationProtocol {
    private val listeners: Set<TestListener> = testSettings.listeners

    private val logger: TestLogger = testSettings.logger

    private inline fun sendToListeners(event: TestListener.() -> Unit) {
        logger.event()
        listeners.forEach(event)
    }

    private fun XCTest.getTestDuration(): Duration =
            testRun?.totalDuration
                    ?.toDuration(DurationUnit.SECONDS)
                    ?: Duration.ZERO

    override fun testCase(testCase: XCTestCase, didRecordIssue: XCTIssue) {
        val duration = testCase.getTestDuration()
        val error = didRecordIssue.associatedError
        val throwable = if (error is NSErrorWithKotlinException) {
            error.kotlinException
        } else {
            Throwable(didRecordIssue.compactDescription)
        }
        if (testCase is XCTestCaseRunner) {
            sendToListeners { fail(testCase.testCase, throwable, duration.inWholeMilliseconds) }
        }
    }

    override fun testCase(testCase: XCTestCase, didRecordExpectedFailure: XCTExpectedFailure) {
        logger.log("TestCase: $testCase got expected failure: ${didRecordExpectedFailure.failureReason}")
        this.testCase(testCase, didRecordExpectedFailure.issue)
    }

    override fun testCaseDidFinish(testCase: XCTestCase) {
        val duration = testCase.getTestDuration()
        if (testCase.testRun?.hasSucceeded == true) {
            if (testCase is XCTestCaseRunner) {
                val test = testCase.testCase
                if (!test.ignored) sendToListeners { pass(test, duration.inWholeMilliseconds) }
            }
        }
    }

    override fun testCaseWillStart(testCase: XCTestCase) {
        if (testCase is XCTestCaseRunner) {
            val test = testCase.testCase
            if (test.ignored) {
                sendToListeners { ignore(test) }
            } else {
                sendToListeners { start(test) }
            }
        }
    }

    override fun testSuite(testSuite: XCTestSuite, didRecordIssue: XCTIssue) {
        logger.log("TestSuite ${testSuite.name} recorded issue: ${didRecordIssue.compactDescription}")
    }

    override fun testSuite(testSuite: XCTestSuite, didRecordExpectedFailure: XCTExpectedFailure) {
        logger.log("TestSuite ${testSuite.name} got expected failure: ${didRecordExpectedFailure.failureReason}")
        this.testSuite(testSuite, didRecordExpectedFailure.issue)
    }

    override fun testSuiteDidFinish(testSuite: XCTestSuite) {
        val duration = testSuite.getTestDuration().inWholeMilliseconds
        if (testSuite is XCTestSuiteRunner) {
            sendToListeners { finishSuite(testSuite.testSuite, duration) }
        } else if (testSuite.name == TOP_LEVEL_SUITE) {
            sendToListeners {
                finishIteration(testSettings, 0, duration)  // test iterations are not supported
                finishTesting(testSettings, duration)
            }
        }
    }

    override fun testSuiteWillStart(testSuite: XCTestSuite) {
        if (testSuite is XCTestSuiteRunner) {
            sendToListeners { startSuite(testSuite.testSuite) }
        } else if (testSuite.name == TOP_LEVEL_SUITE) {
            sendToListeners {
                startTesting(testSettings)
                startIteration(testSettings, 0, testSettings.testSuites)  // test iterations are not supported
            }
        }
    }
}