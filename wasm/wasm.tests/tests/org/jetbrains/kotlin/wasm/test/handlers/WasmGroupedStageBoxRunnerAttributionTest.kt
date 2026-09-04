/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.test.GroupingStageInputsHolder
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.TestInfrastructureException
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.BatchingPackageInserter.Companion.computePackage
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.SourceFileProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.wasm.test.blackbox.computeProxyLauncherClassName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opentest4j.MultipleFailuresError
import java.io.File

class WasmGroupedStageBoxRunnerAttributionTest {
    @Test
    fun `given a passing, a failing and a never-run test then each is attributed on its own`() {
        val passing = GroupedTest("testPassing")
        val failing = GroupedTest("testFailing")
        val neverRan = GroupedTest("testNeverRan")

        val vmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(passing.id, GroupedTestsResultProtocol.STARTED)
            append("box() output with no trailing newline")
            appendProtocolLine(passing.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolLine(failing.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(
                failing.id,
                GroupedTestsResultProtocol.FAILED,
                GroupedTestsResultProtocol.escape(FAILURE_MESSAGE),
                GroupedTestsResultProtocol.escape(FAILURE_DETAILS),
            )
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        runner(listOf(passing, failing, neverRan), vmStdout = listOf(vmStdout), vmFailures = emptyList())
            .processArtifact(DriverLinkedBatchArtifact)

        assertNull(passing.reportedFailure, "A passing test was failed: ${passing.reportedFailure?.message}")

        val failingMessage = failing.reportedFailure?.message.orEmpty()
        assertTrue("$FAILURE_MESSAGE\n$FAILURE_DETAILS" in failingMessage, failingMessage)

        val neverRanMessage = neverRan.reportedFailure?.message.orEmpty()
        assertTrue("no per-test result was reported for '${neverRan.id}'" in neverRanMessage, neverRanMessage)
    }

    @Test
    fun `given a VM that died mid-batch then the results it printed before the crash are still attributed`() {
        val passing = GroupedTest("testPassing")
        val failing = GroupedTest("testFailing")

        val crashedVmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(passing.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(passing.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolLine(failing.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(
                failing.id,
                GroupedTestsResultProtocol.FAILED,
                GroupedTestsResultProtocol.escape(FAILURE_MESSAGE),
                GroupedTestsResultProtocol.escape(FAILURE_DETAILS),
            )
        }
        val vmFailure = vmCrash(crashedVmStdout, vmName = "SpiderMonkey")
        val thrown = assertThrows(Throwable::class.java) {
            runner(listOf(passing, failing), vmStdout = emptyList(), vmFailures = listOf(vmFailure))
                .processArtifact(DriverLinkedBatchArtifact)
        }
        assertEquals(vmFailure, thrown)

        assertNull(passing.reportedFailure, "A passing test was failed: ${passing.reportedFailure?.message}")
        val failingMessage = failing.reportedFailure?.message.orEmpty()
        assertTrue("$FAILURE_MESSAGE\n$FAILURE_DETAILS" in failingMessage, failingMessage)
    }

    @Test
    fun `given a test that took the only VM down before reporting anything then it is named as the crash cause`() {
        val passing = GroupedTest("testPassing")
        val crasher = GroupedTest("testCrasher")

        val crashedVmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(passing.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(passing.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolLine(crasher.id, GroupedTestsResultProtocol.STARTED)
        }

        runner(
            listOf(passing, crasher),
            vmStdout = emptyList(),
            vmFailures = listOf(vmCrash(crashedVmStdout, vmName = "V8")),
        ).processArtifact(DriverLinkedBatchArtifact)

        assertNull(passing.reportedFailure, "A passing test was failed: ${passing.reportedFailure?.message}")

        val message = crasher.reportedFailure?.message.orEmpty()
        assertTrue("no per-test result was reported for '${crasher.id}'" in message, message)
        assertTrue("it most likely crashed that VM" in message, message)
        assertTrue("line on the VM with no terminal" in message, message)
        assertFalse("was silently skipped" in message, message)
        assertTrue("Collected outputs:" in message, message)
    }

    @Test
    fun `given a VM that died without parsable output then its crash surfaces next to an unrelated test failure`() {
        val failing = GroupedTest("testFailing")

        val finishedVmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(failing.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(
                failing.id,
                GroupedTestsResultProtocol.FAILED,
                GroupedTestsResultProtocol.escape(FAILURE_MESSAGE),
                GroupedTestsResultProtocol.escape(FAILURE_DETAILS),
            )
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }
        val vmCrash = vmCrash("startup output with no structured block", vmName = "SpiderMonkey")

        val thrown = assertThrows(Throwable::class.java) {
            runner(listOf(failing), vmStdout = listOf(finishedVmStdout), vmFailures = listOf(vmCrash))
                .processArtifact(DriverLinkedBatchArtifact)
        }
        assertEquals(vmCrash, thrown)

        val failingMessage = failing.reportedFailure?.message.orEmpty()
        assertTrue("$FAILURE_MESSAGE\n$FAILURE_DETAILS" in failingMessage, failingMessage)
    }

    @Test
    fun `given several unexplained VM failures then all of them are reported`() {
        val passing = GroupedTest("testPassing")
        val firstVmFailure = vmCrash("first VM output without a structured block", vmName = "VM-1")
        val secondVmFailure = vmCrash("second VM output without a structured block", vmName = "VM-2")
        val finishedVmOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(passing.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(passing.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        val thrown = assertThrows(MultipleFailuresError::class.java) {
            runner(
                listOf(passing),
                vmStdout = listOf(finishedVmOutput),
                vmFailures = listOf(firstVmFailure, secondVmFailure),
            ).processArtifact(DriverLinkedBatchArtifact)
        }

        assertTrue(firstVmFailure in thrown.failures, thrown.failures.toString())
        assertTrue(secondVmFailure in thrown.failures, thrown.failures.toString())

        assertNull(passing.reportedFailure, "A passing test was failed: ${passing.reportedFailure?.message}")
    }

    @Test
    fun `given a grouped batch whose driver never ran when the VM exits cleanly then every test is failed`() {
        val first = GroupedTest("testFirst")
        val second = GroupedTest("testSecond")

        val stdoutWithoutBlock = "unrelated VM output\nwith no structured result block in it\n"

        runner(listOf(first, second), vmStdout = listOf(stdoutWithoutBlock), vmFailures = emptyList())
            .processArtifact(DriverLinkedBatchArtifact)

        for (test in listOf(first, second)) {
            val message = test.reportedFailure?.message.orEmpty()
            assertTrue(GroupedTestsResultProtocol.BEGIN in message, message)
            assertTrue("not a single test reported a result" in message, message)
        }
    }

    @Test
    fun `given a grouped batch whose driver ran on only one VM then every test is failed`() {
        val first = GroupedTest("testFirst")
        val second = GroupedTest("testSecond")

        val outputWithResults = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(first.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(first.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolLine(second.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(second.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }
        val outputWithoutResults = "fallback to startUnitTests()\n"

        runner(
            listOf(first, second),
            vmStdout = listOf(outputWithResults, outputWithoutResults),
            vmFailures = emptyList(),
        ).processArtifact(DriverLinkedBatchArtifact)

        for (test in listOf(first, second)) {
            val message = test.reportedFailure?.message.orEmpty()
            assertTrue("every driver-enabled VM" in message, message)
            assertTrue("VM-2" in message, message)
            assertFalse("VM-1" in message, message)
        }
    }

    @Test
    fun `given complete VM blocks that split the expected results then each missing execution is failed`() {
        val first = GroupedTest("testFirst")
        val second = GroupedTest("testSecond")

        val firstVmOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(first.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(first.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }
        val secondVmOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(second.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(second.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        runner(
            listOf(first, second),
            vmStdout = listOf(firstVmOutput, secondVmOutput),
            vmFailures = emptyList(),
        ).processArtifact(DriverLinkedBatchArtifact)

        val firstMessage = first.reportedFailure?.message.orEmpty()
        assertTrue("did not report a terminal result in every successful" in firstMessage, firstMessage)
        assertTrue("VM-2" in firstMessage, firstMessage)
        assertFalse("VM-1" in firstMessage, firstMessage)

        val secondMessage = second.reportedFailure?.message.orEmpty()
        assertTrue("did not report a terminal result in every successful" in secondMessage, secondMessage)
        assertTrue("VM-1" in secondMessage, secondMessage)
        assertFalse("VM-2" in secondMessage, secondMessage)
    }

    @Test
    fun `given a test failing with no message on one VM and missing from another then the empty message is explained`() {
        val failing = GroupedTest("testFailing")
        val other = GroupedTest("testOther")

        val firstVmOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(failing.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(failing.id, GroupedTestsResultProtocol.FAILED)
            appendProtocolLine(other.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(other.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }
        val secondVmOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(other.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(other.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        runner(listOf(failing, other), vmStdout = listOf(firstVmOutput, secondVmOutput), vmFailures = emptyList())
            .processArtifact(DriverLinkedBatchArtifact)

        val message = failing.reportedFailure?.message.orEmpty()
        assertTrue("did not report a terminal result in every successful" in message, message)
        assertTrue("carrying neither a message nor details" in message, message)
    }

    @Test
    fun `given a test globally missing from a multi-VM batch then the silently-skipped hint is still shown`() {
        val neverReported = GroupedTest("testNeverReported")
        val other = GroupedTest("testOther")

        val vmOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(other.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(other.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        runner(listOf(neverReported, other), vmStdout = listOf(vmOutput, vmOutput), vmFailures = emptyList())
            .processArtifact(DriverLinkedBatchArtifact)

        val message = neverReported.reportedFailure?.message.orEmpty()
        assertTrue("did not report a terminal result in every successful" in message, message)
        assertTrue("silently skipped" in message, message)
    }

    @Test
    fun `given the same VM missing results in several modes then every mode is named`() {
        val missing = GroupedTest("testMissing")
        val reported = GroupedTest("testReported")
        val output = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(reported.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(reported.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        runner(
            listOf(missing, reported),
            vmStdout = listOf(output, output),
            vmExecutionNames = listOf(
                formatWasmExecutionName("V8", "dev"),
                formatWasmExecutionName("V8", "dce"),
            ),
            vmFailures = emptyList(),
        ).processArtifact(DriverLinkedBatchArtifact)

        assertEquals("V8 (dev)", formatWasmExecutionName("V8", "dev"))
        assertEquals("V8 (dce)", formatWasmExecutionName("V8", "dce"))
        val message = missing.reportedFailure?.message.orEmpty()
        assertTrue("V8 (dev)" in message, message)
        assertTrue("V8 (dce)" in message, message)
    }

    @Test
    fun `given a malformed protocol line then every test is failed instead of dropping the line`() {
        val first = GroupedTest("testFirst")
        val second = GroupedTest("testSecond")
        val malformed = "${GroupedTestsResultProtocol.LINE_PREFIX}${GroupedTestsResultProtocol.SEP}" +
                "${first.id}${GroupedTestsResultProtocol.SEP}BROKEN${GroupedTestsResultProtocol.SEP}message${GroupedTestsResultProtocol.SEP}details"

        val vmOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(first.id, GroupedTestsResultProtocol.PASSED)
            appendLine(malformed)
            appendProtocolLine(second.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        runner(listOf(first, second), vmStdout = listOf(vmOutput), vmFailures = emptyList())
            .processArtifact(DriverLinkedBatchArtifact)

        for (test in listOf(first, second)) {
            val message = test.reportedFailure?.message.orEmpty()
            assertTrue("malformed structured result protocol line" in message, message)
            assertTrue(malformed in message, message)
        }
    }

    @Test
    fun `given a result line truncated by a crash then the rejected batch still names the crashing test`() {
        val passing = GroupedTest("testPassing")
        val crasher = GroupedTest("testCrashing")

        val vmOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(passing.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(passing.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolLine(crasher.id, GroupedTestsResultProtocol.STARTED)
            append("\n")
            append(GroupedTestsResultProtocol.LINE_PREFIX)
            append(GroupedTestsResultProtocol.SEP).append(crasher.id)
            append(GroupedTestsResultProtocol.SEP).append("PAS")
        }

        runner(listOf(passing, crasher), vmStdout = listOf(vmOutput), vmFailures = emptyList())
            .processArtifact(DriverLinkedBatchArtifact)

        val crasherMessage = crasher.reportedFailure?.message.orEmpty()
        assertTrue("malformed structured result protocol line" in crasherMessage, crasherMessage)
        assertTrue("it most likely crashed that VM" in crasherMessage, crasherMessage)

        val passingMessage = passing.reportedFailure?.message.orEmpty()
        assertTrue("this test reported: PASSED" in passingMessage, passingMessage)
        assertFalse("it most likely crashed that VM" in passingMessage, passingMessage)
    }

    @Test
    fun `given a test failing on several VMs then all failure diagnostics are retained`() {
        val test = GroupedTest("testFailing")
        val firstMessage = "failure from VM-1"
        val firstDetails = "details from VM-1"
        val secondMessage = "failure from VM-2"
        val secondDetails = "details from VM-2"
        val firstVmOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(
                test.id,
                GroupedTestsResultProtocol.FAILED,
                GroupedTestsResultProtocol.escape(firstMessage),
                GroupedTestsResultProtocol.escape(firstDetails),
            )
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }
        val secondVmOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(
                test.id,
                GroupedTestsResultProtocol.FAILED,
                GroupedTestsResultProtocol.escape(secondMessage),
                GroupedTestsResultProtocol.escape(secondDetails),
            )
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        runner(
            listOf(test),
            vmStdout = listOf(firstVmOutput, secondVmOutput),
            vmExecutionNames = listOf("V8 (dev)", "V8 (dce)"),
            vmFailures = emptyList(),
        )
            .processArtifact(DriverLinkedBatchArtifact)

        val message = test.reportedFailure?.message.orEmpty()
        assertTrue("$firstMessage\n$firstDetails" in message, message)
        assertTrue("$secondMessage\n$secondDetails" in message, message)
        assertTrue("[V8 (dev)] $firstMessage" in message, message)
        assertTrue("[V8 (dce)] $secondMessage" in message, message)
    }

    @Test
    fun `given a failure with a protocol message then unrelated batch output is not attached`() {
        val failing = GroupedTest("testFailing")
        val unrelatedBatchOutput = "output from another test in the same batch"
        val vmOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(failing.id, GroupedTestsResultProtocol.STARTED)
            append(unrelatedBatchOutput).append("\n")
            appendProtocolLine(
                failing.id,
                GroupedTestsResultProtocol.FAILED,
                GroupedTestsResultProtocol.escape(FAILURE_MESSAGE),
                GroupedTestsResultProtocol.escape(FAILURE_DETAILS),
            )
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        runner(listOf(failing), vmStdout = listOf(vmOutput), vmFailures = emptyList())
            .processArtifact(DriverLinkedBatchArtifact)

        val message = failing.reportedFailure?.message.orEmpty()
        assertTrue("$FAILURE_MESSAGE\n$FAILURE_DETAILS" in message, message)
        assertFalse(unrelatedBatchOutput in message, message)
    }

    @Test
    fun `given a grouped batch whose driver did not complete on one VM then every test is failed`() {
        val first = GroupedTest("testFirst")
        val second = GroupedTest("testSecond")

        val completeOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(first.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(first.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolLine(second.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(second.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }
        val incompleteOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(first.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(first.id, GroupedTestsResultProtocol.PASSED)
        }

        runner(
            listOf(first, second),
            vmStdout = listOf(completeOutput, incompleteOutput),
            vmFailures = emptyList(),
        ).processArtifact(DriverLinkedBatchArtifact)

        for (test in listOf(first, second)) {
            val message = test.reportedFailure?.message.orEmpty()
            assertTrue("Incomplete on:" in message, message)
            assertTrue("VM-2" in message, message)
            assertFalse("VM-1" in message, message)
        }
    }

    @Test
    fun `given a single isolated test without a structured block then only a VM failure fails it`() {
        val passing = GroupedTest("testOnlyOne")
        runner(
            listOf(passing),
            vmStdout = listOf("output of the single-test runner\n"),
            vmFailures = emptyList(),
            boxExportMode = true,
        ).processArtifact(DriverlessBatchArtifact)
        assertNull(passing.reportedFailure, "An isolated passing test was failed: ${passing.reportedFailure?.message}")

        val failing = GroupedTest("testOnlyOne")
        val vmFailure = WasmVMException(AssertionError("Wrong box result 'FAIL'; Expected \"OK\""), vmName = "V8")
        runner(listOf(failing), vmStdout = emptyList(), vmFailures = listOf(vmFailure), boxExportMode = true)
            .processArtifact(DriverlessBatchArtifact)
        assertEquals(vmFailure, failing.reportedFailure)
    }

    @Test
    fun `given a single isolated test crashing on several VMs then every diagnostic is retained`() {
        val failing = GroupedTest("testOnlyOne")
        val v8Failure = WasmVMException(AssertionError("Wrong box result 'FAIL'"), vmName = "V8")
        val spiderMonkeyFailure = WasmVMException(AssertionError("Wrong box result 'CRASH'"), vmName = "SpiderMonkey")

        runner(
            listOf(failing),
            vmStdout = emptyList(),
            vmFailures = listOf(v8Failure, spiderMonkeyFailure),
            boxExportMode = true,
        )
            .processArtifact(DriverlessBatchArtifact)

        val reported = failing.reportedFailure
        assertTrue(reported is MultipleFailuresError, reported.toString())
        val failures = (reported as MultipleFailuresError).failures
        assertTrue(v8Failure in failures, failures.toString())
        assertTrue(spiderMonkeyFailure in failures, failures.toString())
    }

    @Test
    fun `given a single non-isolated test whose driver never ran then that test is failed`() {
        val alone = GroupedTest("testAlone")

        runner(listOf(alone), vmStdout = listOf("VM output with no structured result block\n"), vmFailures = emptyList())
            .processArtifact(DriverLinkedBatchArtifact)

        val message = alone.reportedFailure?.message.orEmpty()
        assertTrue("not a single test reported a result" in message, message)
    }

    @Test
    fun `given a singleton unit-test batch without driver metadata then it is rejected before execution`() {
        val onlyTest = GroupedTest("testOnly")
        var executionCount = 0

        val error = assertThrows(TestInfrastructureException::class.java) {
            runner(
                listOf(onlyTest),
                vmStdout = listOf("no structured result block\n"),
                vmFailures = emptyList(),
                onRunTestCode = { executionCount++ },
            ).processArtifact(DriverlessBatchArtifact)
        }

        assertEquals(0, executionCount, "The runner must validate the artifact before starting any VM")
        assertTrue("is not linked with the result-collecting driver" in error.message.orEmpty(), error.message.orEmpty())
    }

    @Test
    fun `given a singleton artifact with driver metadata then it stays on the unit-test path`() {
        val onlyTest = GroupedTest("testOnly")
        val vmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(onlyTest.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(onlyTest.id, GroupedTestsResultProtocol.FAILED, "failure from the grouped driver")
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        runner(
            listOf(onlyTest),
            vmStdout = listOf(vmStdout),
            vmFailures = emptyList(),
            boxExportMode = true,
        ).processArtifact(DriverLinkedBatchArtifact)

        assertTrue("failure from the grouped driver" in onlyTest.reportedFailure?.message.orEmpty())
    }

    @Test
    fun `given a test crashing one VM and passing on another then the crash is still reported`() {
        val crasher = GroupedTest("testCrasher")
        val other = GroupedTest("testOther")

        val finishedVmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(other.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(other.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolLine(crasher.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(crasher.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }
        val crashedVmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(other.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(other.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolLine(crasher.id, GroupedTestsResultProtocol.STARTED)
        }

        runner(
            listOf(other, crasher),
            vmStdout = listOf(finishedVmStdout),
            vmFailures = listOf(vmCrash(crashedVmStdout, vmName = "SpiderMonkey")),
        ).processArtifact(DriverLinkedBatchArtifact)

        assertNull(other.reportedFailure, "A passing test was failed: ${other.reportedFailure?.message}")

        val message = crasher.reportedFailure?.message.orEmpty()
        assertTrue("it most likely crashed that VM" in message, message)
        assertTrue("Collected outputs:" in message, message)
    }

    @Test
    fun `given a test failing on one VM and crashing another then the failure and the crash are both reported`() {
        val failingCrasher = GroupedTest("testFailingCrasher")

        val finishedVmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(failingCrasher.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(
                failingCrasher.id,
                GroupedTestsResultProtocol.FAILED,
                GroupedTestsResultProtocol.escape(FAILURE_MESSAGE),
                GroupedTestsResultProtocol.escape(FAILURE_DETAILS),
            )
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }
        val crashedVmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(failingCrasher.id, GroupedTestsResultProtocol.STARTED)
        }

        runner(
            listOf(failingCrasher),
            vmStdout = listOf(finishedVmStdout),
            vmFailures = listOf(
                vmCrash(
                    crashedVmStdout,
                    vmName = "WasmEdge",
                    executionName = "WasmEdge (dce)",
                )
            ),
        ).processArtifact(DriverLinkedBatchArtifact)

        val message = failingCrasher.reportedFailure?.message.orEmpty()
        assertTrue("line on another VM with no terminal" in message, message)
        assertTrue("Execution: WasmEdge (dce)." in message, message)
        assertTrue(FAILURE_MESSAGE in message, message)
        assertTrue(FAILURE_DETAILS in message, message)
        assertTrue("it most likely crashed that VM" in message, message)
    }

    @Test
    fun `given several tests in a batch that claims no driver then the infrastructure is rejected`() {
        val first = GroupedTest("testFirst")
        val second = GroupedTest("testSecond")

        val error = assertThrows(TestInfrastructureException::class.java) {
            runner(listOf(first, second), vmStdout = listOf("no structured block here\n"), vmFailures = emptyList())
                .processArtifact(DriverlessBatchArtifact)
        }

        val message = error.message.orEmpty()
        assertTrue("is not linked with the result-collecting driver" in message, message)
        assertNull(first.reportedFailure, "A rejected batch must not also attribute failures to its tests")
        assertNull(second.reportedFailure, "A rejected batch must not also attribute failures to its tests")
    }

    @Test
    fun `given box-export mode with several tests in the batch then the infrastructure is rejected`() {
        val first = GroupedTest("testFirst")
        val second = GroupedTest("testSecond")

        val error = assertThrows(TestInfrastructureException::class.java) {
            runner(listOf(first, second), vmStdout = emptyList(), vmFailures = emptyList(), boxExportMode = true)
                .processArtifact(DriverlessBatchArtifact)
        }

        val message = error.message.orEmpty()
        assertTrue("reports a single " in message, message)
        assertNull(second.reportedFailure, "The rejected batch must not attribute a failure to a test it skipped")
    }

    @Test
    fun `given box-export mode failing on several VMs then every diagnostic is retained`() {
        val test = GroupedTest("testOnlyOne")
        val v8Failure = WasmVMException(AssertionError("Wrong box result 'FAIL'"), vmName = "V8")
        val spiderMonkeyFailure = WasmVMException(AssertionError("Wrong box result 'CRASH'"), vmName = "SpiderMonkey")

        runner(
            listOf(test),
            vmStdout = emptyList(),
            vmFailures = listOf(v8Failure, spiderMonkeyFailure),
            boxExportMode = true,
        ).processArtifact(DriverlessBatchArtifact)

        val reported = test.reportedFailure
        assertTrue(reported is MultipleFailuresError, reported.toString())
        val failures = (reported as MultipleFailuresError).failures
        assertTrue(v8Failure in failures, failures.toString())
        assertTrue(spiderMonkeyFailure in failures, failures.toString())
    }

    @Test
    fun `given a failure reported with no message then the batch output is still attached`() {
        val failing = GroupedTest("testFailing")
        val printedByTheTest = "what the test printed before failing"

        val vmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(failing.id, GroupedTestsResultProtocol.STARTED)
            append(printedByTheTest).append("\n")
            appendProtocolLine(failing.id, GroupedTestsResultProtocol.FAILED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        runner(listOf(failing), vmStdout = listOf(vmStdout), vmFailures = emptyList())
            .processArtifact(DriverLinkedBatchArtifact)

        val message = failing.reportedFailure?.message.orEmpty()
        assertTrue("neither a message nor details" in message, message)
        assertTrue(printedByTheTest in message, message)
    }

    @Test
    fun `given a result for a test outside the batch then the infrastructure is rejected`() {
        val inBatch = GroupedTest("testInBatch")
        val foreign = GroupedTest("testForeign")

        val vmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(inBatch.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolLine(foreign.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        val error = assertThrows(TestInfrastructureException::class.java) {
            runner(listOf(inBatch), vmStdout = listOf(vmStdout), vmFailures = emptyList())
                .processArtifact(DriverLinkedBatchArtifact)
        }

        val message = error.message.orEmpty()
        assertTrue("reported results for tests that are not part of it" in message, message)
        assertTrue(foreign.id in message, message)
    }

    @Test
    fun `given test infos with colliding package hashes then launcher names remain distinct`() {
        val first = KotlinTestInfo("org.jetbrains.kotlin.wasm.test.Aa", "test", emptySet())
        val second = KotlinTestInfo("org.jetbrains.kotlin.wasm.test.BB", "test", emptySet())

        assertEquals(computePackage(first).hashCode(), computePackage(second).hashCode())
        assertNotEquals(
            computeProxyLauncherClassName(first),
            computeProxyLauncherClassName(second),
            "Distinct tests must not generate the same synthetic launcher class",
        )
    }

    @Test
    fun `given a malformed line on one VM and a crash on another then the crash is not blamed for the rejection`() {
        val crasher = GroupedTest("testCrashing")
        val other = GroupedTest("testOther")

        val malformed = "${GroupedTestsResultProtocol.LINE_PREFIX}${GroupedTestsResultProtocol.SEP}" +
                "${crasher.id}${GroupedTestsResultProtocol.SEP}BROKEN${GroupedTestsResultProtocol.SEP}message${GroupedTestsResultProtocol.SEP}details"
        val cleanVmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(other.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(other.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolLine(crasher.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(crasher.id, GroupedTestsResultProtocol.PASSED)
            appendLine(malformed)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        val crashedVmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(other.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(other.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolLine(crasher.id, GroupedTestsResultProtocol.STARTED)
        }

        runner(
            listOf(crasher, other),
            vmStdout = listOf(cleanVmStdout),
            vmFailures = listOf(vmCrash(crashedVmStdout, vmName = "VM-2")),
        ).processArtifact(DriverLinkedBatchArtifact)

        val crasherMessage = crasher.reportedFailure?.message.orEmpty()
        assertTrue("it most likely crashed that VM" in crasherMessage, crasherMessage)
        assertTrue("not necessarily related" in crasherMessage, crasherMessage)
        assertFalse("likely cause of the rejected block" in crasherMessage, crasherMessage)
    }

    @Test
    fun `given a large malformed line and several tests then diagnostics stay bounded`() {
        val tests = listOf(
            GroupedTest("testFirst"),
            GroupedTest("testSecond"),
            GroupedTest("testThird"),
        )
        val malformedLine = "${GroupedTestsResultProtocol.LINE_PREFIX}${GroupedTestsResultProtocol.SEP}" +
                "${tests.first().id}${GroupedTestsResultProtocol.SEP}BROKEN${GroupedTestsResultProtocol.SEP}" +
                "x".repeat(100_000)
        val vmOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendLine(malformedLine)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        runner(tests, vmStdout = listOf(vmOutput), vmFailures = emptyList())
            .processArtifact(DriverLinkedBatchArtifact)

        for (test in tests) {
            val message = test.reportedFailure?.message.orEmpty()
            assertTrue(message.length <= 16 * 1024, "Diagnostic was not bounded: ${message.length}")
            assertTrue("truncated; original length=${malformedLine.length} chars" in message, message)
            assertTrue("SHA-256=" in message, message)
            assertFalse("x".repeat(10_000) in message, "The raw malformed line was copied into the diagnostic")
        }
    }

    @Test
    fun `given a STARTED line truncated by a crash then that test is still named`() {
        val passing = GroupedTest("testPassing")
        val crasher = GroupedTest("testCrashing")

        val vmOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(passing.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(passing.id, GroupedTestsResultProtocol.PASSED)
            append("\n")
            append(GroupedTestsResultProtocol.LINE_PREFIX)
            append(GroupedTestsResultProtocol.SEP).append(crasher.id)
            append(GroupedTestsResultProtocol.SEP).append("STAR")
        }

        runner(listOf(passing, crasher), vmStdout = listOf(vmOutput), vmFailures = emptyList())
            .processArtifact(DriverLinkedBatchArtifact)

        val crasherMessage = crasher.reportedFailure?.message.orEmpty()
        assertTrue("cut off while this test was reporting" in crasherMessage, crasherMessage)
        assertTrue("likely cause of the rejection" in crasherMessage, crasherMessage)

        val passingMessage = passing.reportedFailure?.message.orEmpty()
        assertTrue("this test reported: PASSED" in passingMessage, passingMessage)
        assertFalse("cut off while this test was reporting" in passingMessage, passingMessage)
    }

    @Test
    fun `given a grouped test without a box method then the infrastructure is rejected`() {
        val withBox = GroupedTest("testWithBox")
        val withoutBox = GroupedTest("testWithoutBox", moduleStructure = NoBoxFileModuleStructure)

        val vmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(withBox.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        val error = assertThrows(TestInfrastructureException::class.java) {
            runner(listOf(withBox, withoutBox), vmStdout = listOf(vmStdout), vmFailures = emptyList())
                .processArtifact(DriverLinkedBatchArtifact)
        }

        val message = error.message.orEmpty()
        assertTrue("does not have a box() method" in message, message)
        assertTrue("WasmGroupingTestIsolator" in message, message)
    }

    @Test
    fun `given a complete but empty result block then the empty report is reported too`() {
        val neverReported = GroupedTest("testNeverReported")

        val vmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        runner(listOf(neverReported), vmStdout = listOf(vmStdout), vmFailures = emptyList())
            .processArtifact(DriverLinkedBatchArtifact)

        val message = neverReported.reportedFailure?.message.orEmpty()
        assertTrue("No tests have been found" in message, message)
        assertTrue("no per-test result was reported for '${neverReported.id}'" in message, message)
    }

    @Test
    fun `given a batch where every VM failed to start then those failures are reported, not just the missing block`() {
        val first = GroupedTest("testFirst")
        val second = GroupedTest("testSecond")

        val firstVmFailure = vmCrash("failed to instantiate the module", vmName = "VM-1")
        val secondVmFailure = vmCrash("failed to instantiate the module", vmName = "VM-2")

        val thrown = assertThrows(MultipleFailuresError::class.java) {
            runner(
                listOf(first, second),
                vmStdout = emptyList(),
                vmFailures = listOf(firstVmFailure, secondVmFailure),
            ).processArtifact(DriverLinkedBatchArtifact)
        }

        assertTrue(firstVmFailure in thrown.failures, thrown.failures.toString())
        assertTrue(secondVmFailure in thrown.failures, thrown.failures.toString())

        for (test in listOf(first, second)) {
            val message = test.reportedFailure?.message.orEmpty()
            assertTrue("not a single test reported a result" in message, message)
        }
    }

    @Test
    fun `given a VM that printed no block and another that failed then that failure is reported too`() {
        val onlyTest = GroupedTest("testOnly")
        val vmFailure = vmCrash("engine died with no parsable output", vmName = "VM-2")

        val thrown = assertThrows(WasmVMException::class.java) {
            runner(
                listOf(onlyTest),
                vmStdout = listOf("fallback to startUnitTests()\n"),
                vmFailures = listOf(vmFailure),
            ).processArtifact(DriverLinkedBatchArtifact)
        }

        assertEquals(vmFailure, thrown)

        val message = onlyTest.reportedFailure?.message.orEmpty()
        assertTrue("did not print a" in message, message)
    }

    @Test
    fun `given a VM whose block was left open and another that failed then that failure is reported too`() {
        val onlyTest = GroupedTest("testOnly")
        val vmFailure = vmCrash("engine died with no parsable output", vmName = "VM-2")

        val incompleteOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(onlyTest.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(onlyTest.id, GroupedTestsResultProtocol.PASSED)
        }

        val thrown = assertThrows(WasmVMException::class.java) {
            runner(
                listOf(onlyTest),
                vmStdout = listOf(incompleteOutput),
                vmFailures = listOf(vmFailure),
            ).processArtifact(DriverLinkedBatchArtifact)
        }

        assertEquals(vmFailure, thrown)

        val message = onlyTest.reportedFailure?.message.orEmpty()
        assertTrue("did not print a complete" in message, message)
    }

    @Test
    fun `given a rejected block and a VM failure it cannot explain then that failure is reported too`() {
        val onlyTest = GroupedTest("testOnly")

        val vmFailure = vmCrash("engine died with no parsable output", vmName = "VM-2")
        val malformed = "${GroupedTestsResultProtocol.LINE_PREFIX}${GroupedTestsResultProtocol.SEP}" +
                "${onlyTest.id}${GroupedTestsResultProtocol.SEP}BROKEN${GroupedTestsResultProtocol.SEP}" +
                "message${GroupedTestsResultProtocol.SEP}details"

        val vmOutput = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendLine(malformed)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        val thrown = assertThrows(WasmVMException::class.java) {
            runner(
                listOf(onlyTest),
                vmStdout = listOf(vmOutput),
                vmFailures = listOf(vmFailure),
            ).processArtifact(DriverLinkedBatchArtifact)
        }

        assertEquals(vmFailure, thrown)

        val message = onlyTest.reportedFailure?.message.orEmpty()
        assertTrue("malformed structured result protocol line" in message, message)
    }

    private class IdentitySourceFileProvider : SourceFileProvider() {
        override val preprocessors: List<SourceFilePreprocessor> = emptyList()

        override fun getKotlinSourceDirectoryForModule(module: TestModule): File = error("Not used in this test")

        override fun getJavaSourceDirectoryForModule(module: TestModule): File = error("Not used in this test")

        override fun getAdditionalFilesDirectoryForModule(module: TestModule): File = error("Not used in this test")

        override fun getContentOfSourceFile(
            testFile: TestFile,
            preprocessorFilter: ((SourceFilePreprocessor) -> Boolean)?,
        ): String = testFile.originalContent

        override fun getOrCreateRealFileForSourceFile(testFile: TestFile): File = error("Not used in this test")
    }

    private class GroupedTest(
        methodName: String,
        moduleStructure: TestModuleStructure = SingleBoxFileModuleStructure,
    ) {
        private val failures = mutableListOf<Throwable>()

        val testInfo: KotlinTestInfo = KotlinTestInfo(
            className = "org.jetbrains.kotlin.wasm.test.WasmJsCodegenBoxTestGenerated\$Box\$Grouping",
            methodName = methodName,
            tags = emptySet(),
        )

        val id: String = computeProxyLauncherClassName(testInfo)

        val input: NonGroupingStageOutput = NonGroupingStageOutput(
            testServices = TestServices().apply {
                register(KotlinTestInfo::class, testInfo)
                register(TestModuleStructure::class, moduleStructure)
                register(SourceFileProvider::class, IdentitySourceFileProvider())
            },
            catchingExecutor = { _, block ->
                try {
                    block()
                } catch (e: Throwable) {
                    failures += e
                }
            },
        )

        val reportedFailure: Throwable?
            get() {
                assertTrue(failures.size <= 1, "Expected at most one failure for $id, got: $failures")
                return failures.singleOrNull()
            }
    }

    private fun runner(
        batch: List<GroupedTest>,
        vmStdout: List<String>,
        vmExecutionNames: List<String> = vmStdout.indices.map { "VM-${it + 1}" },
        vmFailures: List<Throwable>,
        boxExportMode: Boolean = false,
        onRunTestCode: () -> Unit = {},
    ): AbstractWasmGroupingStageBoxRunner {
        assertEquals(vmStdout.size, vmExecutionNames.size)
        val testServices = TestServices().apply {
            register(AssertionsService::class, JUnit5Assertions)
            register(GroupingStageInputsHolder::class, GroupingStageInputsHolder(batch.map { it.input }))
        }
        return object : AbstractWasmGroupingStageBoxRunner(testServices) {
            override fun shouldUseBoxExportMode(): Boolean = boxExportMode

            override fun runTestCode(
                artifact: BinaryArtifacts.Wasm,
                useUnitTestRunnerOnly: Boolean,
                outputCollector: MutableList<WasmVMOutput>?,
            ): List<Throwable> {
                onRunTestCode()
                outputCollector?.addAll(vmStdout.mapIndexed { index, output ->
                    WasmVMOutput(
                        vmName = "VM-${index + 1}",
                        output = output,
                        executionName = vmExecutionNames[index],
                    )
                })
                return vmFailures
            }
        }
    }

    private companion object {
        const val FAILURE_MESSAGE = "Test failed with: FAIL|1. Expected <OK>, actual <FAIL|1>."

        const val FAILURE_DETAILS =
            "AssertionError: boom | with a pipe\n\tat Foo.box(foo.kt:1)\n\tat ProxyLauncher.runTest(ProxyBatchLauncher.kt:3)"

        fun StringBuilder.appendProtocolSentinel(sentinel: String) {
            append("\n").append(sentinel).append("\n")
        }

        fun StringBuilder.appendProtocolLine(id: String, status: String, message: String = "", details: String = "") {
            append("\n")
            append(GroupedTestsResultProtocol.LINE_PREFIX)
            for (field in listOf(id, status, message, details)) {
                append(GroupedTestsResultProtocol.SEP).append(field)
            }
            append("\n")
        }

        fun vmCrash(capturedStdout: String, vmName: String, executionName: String = vmName): Throwable = WasmVMException(
            AssertionError(
                "Command \"$vmName ./test.mjs\" terminated with exit code 133 in working dir \"/tmp/batch\"\n" +
                        "OUTPUT:\n$capturedStdout\n---"
            ),
            vmName = vmName,
            executionName = executionName,
        )

        val SingleBoxFileModuleStructure: TestModuleStructure =
            singleFileModuleStructure("fun box(): String = \"OK\"")

        val NoBoxFileModuleStructure: TestModuleStructure =
            singleFileModuleStructure("fun helper(): String = \"OK\"")

        fun singleFileModuleStructure(fileContent: String): TestModuleStructure = object : TestModuleStructure() {
            override val modules: List<TestModule> = listOf(
                TestModule(
                    name = "main",
                    files = listOf(
                        TestFile(
                            relativePath = "main.kt",
                            originalContent = fileContent,
                            originalFile = File("main.kt"),
                            startLineNumberInOriginalFile = 0,
                            isAdditional = false,
                            directives = RegisteredDirectives.Empty,
                        )
                    ),
                    allDependencies = emptyList(),
                    directives = RegisteredDirectives.Empty,
                    languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
                )
            )
            override val allDirectives: RegisteredDirectives get() = RegisteredDirectives.Empty
            override val originalTestDataFiles: List<File> get() = emptyList()
        }

        val DriverLinkedBatchArtifact = object : BinaryArtifacts.Wasm() {
            override val hasGroupedTestsDriver: Boolean get() = true
        }

        val DriverlessBatchArtifact = object : BinaryArtifacts.Wasm() {}
    }
}
