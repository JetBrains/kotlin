/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import kotlin.native.internal.test.*
import kotlin.time.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import platform.Foundation.NSError
import platform.darwin.NSObject
import platform.XCTest.*

/**
 * Test execution observation.
 *
 * This is a bridge between XCTest execution and reporting that brings an ability to get results test-by-test.
 * It logs tests and notifies listeners set with [testSettings].
 * See also [XCTestObservation on Apple documentation](https://developer.apple.com/documentation/xctest/xctestobservation)
 *
 * @see TestSettings
 */
internal class NativeTestObserver(private val testSettings: TestSettings) : NSObject(), XCTestObservationProtocol {
    private val listeners = testSettings.listeners
    private val logger = testSettings.logger

    private inline fun sendToListeners(event: TestListener.() -> Unit) {
        logger.event()
        listeners.forEach(event)
    }

    private fun XCTest.getTestDuration(): Duration =
        testRun?.totalDuration
            ?.toDuration(DurationUnit.SECONDS)
            ?: Duration.ZERO

    /**
     * Failed test case execution.
     *
     * Records test failures sending them to test listeners.
     */
    override fun testCase(testCase: XCTestCase, didRecordIssue: XCTIssue) {
        if (testCase is XCTestCaseWrapper) {
            val duration = testCase.getTestDuration()
            val error = didRecordIssue.associatedError as NSError
            val throwable = if (error is NSErrorWithKotlinException) {
                error.kotlinException
            } else {
                Throwable(didRecordIssue.compactDescription)
            }
            sendToListeners { fail(testCase.testCase, throwable, duration.inWholeMilliseconds) }
        }
    }

    /**
     * Records expected failures as failed test as soon as such expectations should be processed in the test.
     */
    override fun testCase(testCase: XCTestCase, didRecordExpectedFailure: XCTExpectedFailure) {
        logger.log("TestCase: $testCase got expected failure: ${didRecordExpectedFailure.failureReason}")
        this.testCase(testCase, didRecordExpectedFailure.issue)
    }

    /**
     * Test case finish notification.
     * Both successful and failed executions get this notification.
     */
    override fun testCaseDidFinish(testCase: XCTestCase) {
        val duration = testCase.getTestDuration()
        if (testCase.testRun?.hasSucceeded == true) {
            if (testCase is XCTestCaseWrapper) {
                val test = testCase.testCase
                if (!test.ignored) sendToListeners { pass(test, duration.inWholeMilliseconds) }
            }
        }
    }

    /**
     * Test case start notification.
     */
    override fun testCaseWillStart(testCase: XCTestCase) {
        if (testCase is XCTestCaseWrapper) {
            val test = testCase.testCase
            if (test.ignored) {
                sendToListeners { ignore(test) }
            } else {
                sendToListeners { start(test) }
            }
        }
    }

    /**
     * Test suite failure notification.
     *
     * Logs the failure of the test suite execution.
     */
    override fun testSuite(testSuite: XCTestSuite, didRecordIssue: XCTIssue) {
        logger.log("TestSuite ${testSuite.name} recorded issue: ${didRecordIssue.compactDescription}")
    }

    /**
     * Test suite expected failure.
     *
     * Logs the failure of the test suite execution.
     * Treat expected failures as ordinary unexpected one.
     */
    override fun testSuite(testSuite: XCTestSuite, didRecordExpectedFailure: XCTExpectedFailure) {
        logger.log("TestSuite ${testSuite.name} got expected failure: ${didRecordExpectedFailure.failureReason}")
        this.testSuite(testSuite, didRecordExpectedFailure.issue)
    }

    /**
     * Test suite finish notification.
     */
    override fun testSuiteDidFinish(testSuite: XCTestSuite) {
        val duration = testSuite.getTestDuration().inWholeMilliseconds
        if (testSuite is XCTestSuiteWrapper) {
            sendToListeners { finishSuite(testSuite.testSuite, duration) }
        } else if (testSuite.name == TOP_LEVEL_SUITE) {
            sendToListeners {
                finishIteration(testSettings, 0, duration)  // test iterations are not supported
                finishTesting(testSettings, duration)
            }
        }
    }

    /**
     * Test suite start notification.
     */
    override fun testSuiteWillStart(testSuite: XCTestSuite) {
        if (testSuite is XCTestSuiteWrapper) {
            sendToListeners { startSuite(testSuite.testSuite) }
        } else if (testSuite.name == TOP_LEVEL_SUITE) {
            sendToListeners {
                startTesting(testSettings)
                startIteration(testSettings, 0, testSettings.testSuites)  // test iterations are not supported
            }
        }
    }

    override fun debugDescription() = "Native test listener with test settings $testSettings"
}