/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.testing.tcsmc

import jetbrains.buildServer.messages.serviceMessages.TestFinished
import jetbrains.buildServer.messages.serviceMessages.TestFailed
import jetbrains.buildServer.messages.serviceMessages.TestStarted
import jetbrains.buildServer.messages.serviceMessages.TestSuiteFinished
import jetbrains.buildServer.messages.serviceMessages.TestSuiteStarted
import kotlin.test.Test

class BufferedNonTestOutputTest : TCServiceMessagesClientTest() {
    /**
     * Validates that non-test output arriving before any test is started gets buffered and emitted
     * as StdErr on the root descriptor when the test suite closes normally.
     *
     * This covers the edge case where stderr/stdout arrives outside any test context (e.g., from
     * test framework initialization or teardown) and must not be lost or leak to the Gradle log.
     * See KT-69896: the fix buffers such output and attaches it to the root node on close.
     */
    @Test
    fun testOutputBeforeAnyTestGoesToRootStdErrOnClose() {
        assertEvents(
            """
STARTED SUITE root // root
  StdErr[pending before test] // root
COMPLETED SKIPPED // root
            """
        ) {
            regularText("pending before test")
        }
    }

    /**
     * Validates that non-test output arriving between suite-start and test-start gets buffered
     * and flushed as StdErr on the next test that starts.
     *
     * This is the normal happy path: when a test process writes to stderr before the first test
     * begins (e.g., during test framework setup), the buffered output is attached to that test's
     * stderr so it appears in the test report, not in the Gradle build log.
     */
    @Test
    fun testOutputBeforeTestIsFlushedToNextTestStdErr() {
        assertEvents(
            """
STARTED SUITE root // root
  STARTED SUITE  // root/
    STARTED TEST displayName: Test, classDisplayName: , className: , name: Test // root//Test
      StdErr[pending before test start] // root//Test
    COMPLETED SUCCESS // root//Test
  COMPLETED SUCCESS // root/
COMPLETED SUCCESS // root
            """
        ) {
            serviceMessage(TestSuiteStarted(""))
            regularText("pending before test start")
            serviceMessage(TestStarted("Test", false, null))
            serviceMessage(TestFinished("Test", 0))
            serviceMessage(TestSuiteFinished(""))
        }
    }

    /**
     * Simulates the race condition from KT-69896: stderr output (e.g. from `e.printStackTrace()`)
     * arrives after `testFinished` has been processed on the stdout stream.
     *
     * In the real scenario, the K/N test process writes the stack trace to stderr before emitting
     * `##teamcity[testFinished ...]` on stdout, but OS pipe buffering and Gradle's separate reader
     * threads cause the Gradle side to process `testFinished` first. The stderr lines then arrive
     * when the test node is no longer the active leaf.
     *
     * The expected behavior is that such output is still attributed to the just-finished test as
     * StdErr, not silently buffered to the root node or misattributed to the next test.
     */
    @Test
    fun testStderrAfterTestFinishedIsAttributedToFinishedTest() {
        assertEvents(
            """
STARTED SUITE root // root
  STARTED SUITE  // root/
    STARTED TEST displayName: Test, classDisplayName: , className: , name: Test // root//Test
      StdErr[kotlin.IllegalStateException] // root//Test
      StdErr[    at Test.testStderr(Test.kt:6)] // root//Test
    COMPLETED SUCCESS // root//Test
  COMPLETED SUCCESS // root/
COMPLETED SUCCESS // root
            """
        ) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestStarted("Test", false, null))
            serviceMessage(TestFinished("Test", 0))
            // Stderr arrives after testFinished due to the stdout/stderr race
            regularText("kotlin.IllegalStateException")
            regularText("    at Test.testStderr(Test.kt:6)")
            serviceMessage(TestSuiteFinished(""))
        }
    }

    /**
     * Same race condition as above, but with two tests: stderr from the first test arrives after
     * `testFinished` for that test but before `testStarted` for the second test.
     *
     * The expected behavior is that the output is attributed to the first test (the one that
     * produced it), not to the second test.
     */
    @Test
    fun testStderrAfterTestFinishedIsNotMisattributedToNextTest() {
        assertEvents(
            """
STARTED SUITE root // root
  STARTED SUITE  // root/
    STARTED TEST displayName: Test1, classDisplayName: , className: , name: Test1 // root//Test1
      StdErr[stderr from test1] // root//Test1
    COMPLETED SUCCESS // root//Test1
    STARTED TEST displayName: Test2, classDisplayName: , className: , name: Test2 // root//Test2
    COMPLETED SUCCESS // root//Test2
  COMPLETED SUCCESS // root/
COMPLETED SUCCESS // root
            """
        ) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestStarted("Test1", false, null))
            serviceMessage(TestFinished("Test1", 0))
            // Stderr from Test1 arrives after testFinished
            regularText("stderr from test1")
            serviceMessage(TestStarted("Test2", false, null))
            serviceMessage(TestFinished("Test2", 0))
            serviceMessage(TestSuiteFinished(""))
        }
    }

    /**
     * Covers the late-tail race where stdout delivers both `testFinished` and `testSuiteFinished`
     * before the stderr stream has fully drained.
     *
     * The expected behavior is that trailing stderr still belongs to the last test in the suite.
     */
    @Test
    fun testStderrAfterSuiteFinishedStillBelongsToLastTest() {
        assertEvents(
            """
STARTED SUITE root // root
  STARTED SUITE  // root/
    STARTED TEST displayName: Test, classDisplayName: , className: , name: Test // root//Test
      StdErr[late stderr line 1] // root//Test
      StdErr[late stderr line 2] // root//Test
    COMPLETED SUCCESS // root//Test
  COMPLETED SUCCESS // root/
COMPLETED SUCCESS // root
            """
        ) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestStarted("Test", false, null))
            serviceMessage(TestFinished("Test", 0))
            serviceMessage(TestSuiteFinished(""))
            // stderr tail arrives after suiteFinished due to stream scheduling
            regularText("late stderr line 1")
            regularText("late stderr line 2")
        }
    }

    @Test
    fun testLateStderrAfterFailedTestStaysInFailedTest() {
        assertEvents(
            """
STARTED SUITE root // root
  STARTED SUITE  // root/
    STARTED TEST displayName: Test, classDisplayName: , className: , name: Test // root//Test
      FAILURE  // root//Test
      StdErr[post-failure stderr] // root//Test
    COMPLETED FAILURE // root//Test
  COMPLETED FAILURE // root/
COMPLETED FAILURE // root
            """
        ) {
            serviceMessage(TestSuiteStarted(""))
            serviceMessage(TestStarted("Test", false, null))
            serviceMessage(TestFailed("Test", null as Throwable?))
            serviceMessage(TestFinished("Test", 0))
            regularText("post-failure stderr")
            serviceMessage(TestSuiteFinished(""))
        }
    }

    @Test
    fun testNestedSuiteDeferredClosePreservesLateStderr() {
        assertEvents(
            """
STARTED SUITE root // root
  STARTED SUITE outer.inner // root/outer/inner
    STARTED TEST displayName: Test, classDisplayName: inner, className: inner, name: Test // root/outer/inner/Test
      StdErr[late stderr after inner close] // root/outer/inner/Test
    COMPLETED SUCCESS // root/outer/inner/Test
  COMPLETED SUCCESS // root/outer/inner
COMPLETED SUCCESS // root
            """
        ) {
            serviceMessage(TestSuiteStarted("outer"))
            serviceMessage(TestSuiteStarted("inner"))
            serviceMessage(TestStarted("Test", false, null))
            serviceMessage(TestFinished("Test", 0))
            serviceMessage(TestSuiteFinished("inner"))
            regularText("late stderr after inner close")
            serviceMessage(TestSuiteFinished("outer"))
        }
    }

}
