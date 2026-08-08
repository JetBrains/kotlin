/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.test.GroupingStageInputsHolder
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol
import org.jetbrains.kotlin.test.grouping.markGroupedTestsDriverGenerated
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.wasm.test.blackbox.computeProxyLauncherClassName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests the JVM side of a grouped K/Wasm run: it drives the real
 * [AbstractWasmGroupingStageBoxRunner.processArtifact] over the stdout a batch would produce, and checks that every
 * outcome lands on the test that caused it.
 *
 * Only the VMs are faked. Their stdout is built from the real [GroupedTestsResultProtocol] constants and
 * [GroupedTestsResultProtocol.escape] — the exact bytes the generated driver prints.
 */
class WasmGroupedStageBoxRunnerAttributionTest {
    @Test
    fun `given a passing, a failing and a never-run test then each is attributed on its own`() {
        val passing = GroupedTest("testPassing")
        val failing = GroupedTest("testFailing")
        val neverRan = GroupedTest("testNeverRan")

        // `passing` also writes to stdout without a trailing newline, so its result line would be glued onto that
        // leftover if the driver did not prefix every line with '\n'.
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
            .processArtifact(FakeWasmArtifact)

        // Mid-line noise did not swallow the result line of `passing`.
        assertNull(passing.reportedFailure, "A passing test was failed: ${passing.reportedFailure?.message}")

        // The failure text survived escaping, transport and unescaping: separators and line breaks are intact, and the
        // runner joins the message with the stack trace.
        assertEquals("$FAILURE_MESSAGE\n$FAILURE_DETAILS", failing.reportedFailure?.message)

        val neverRanMessage = neverRan.reportedFailure?.message.orEmpty()
        assertTrue("no per-test result was reported for '${neverRan.id}'" in neverRanMessage, neverRanMessage)
    }

    @Test
    fun `given a VM that died mid-batch then the results it printed before the crash are still attributed`() {
        val passing = GroupedTest("testPassing")
        val failing = GroupedTest("testFailing")

        // A crashed VM reports through the failure that `ExternalTool.run` raises for a non-zero exit code, with the
        // captured stdout embedded in its message, rather than through the output collector.
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
            // No END sentinel: the VM died right here.
        }
        // Both tests had already reported when the VM went down, so no test's start localizes the crash and it is
        // surfaced against the batch — while the results it did print are still attributed to their tests.
        val vmFailure = vmCrash(crashedVmStdout, vmName = "SpiderMonkey")
        val thrown = assertThrows(Throwable::class.java) {
            runner(listOf(passing, failing), vmStdout = emptyList(), vmFailures = listOf(vmFailure))
                .processArtifact(FakeWasmArtifact)
        }
        assertEquals(vmFailure, thrown)

        assertNull(passing.reportedFailure, "A passing test was failed: ${passing.reportedFailure?.message}")
        assertEquals("$FAILURE_MESSAGE\n$FAILURE_DETAILS", failing.reportedFailure?.message)
    }

    @Test
    fun `given a test that took the only VM down before reporting anything then it is named as the crash cause`() {
        // The plain crash: no engine ever reported an outcome for `crasher`, so it is both absent from the report and
        // the last test the VM started. It has to be named, and with the crash diagnosis rather than with the
        // "silently skipped" one that a test the VM never reached gets — those two are indistinguishable by the report
        // alone, and only the STARTED line tells them apart.
        val passing = GroupedTest("testPassing")
        val crasher = GroupedTest("testCrasher")

        val crashedVmStdout = buildString {
            appendProtocolSentinel(GroupedTestsResultProtocol.BEGIN)
            appendProtocolLine(passing.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(passing.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolLine(crasher.id, GroupedTestsResultProtocol.STARTED)
            // Neither a terminal line nor an END sentinel: the VM went down inside `crasher`.
        }

        runner(
            listOf(passing, crasher),
            vmStdout = emptyList(),
            vmFailures = listOf(vmCrash(crashedVmStdout, vmName = "V8")),
        ).processArtifact(FakeWasmArtifact)

        assertNull(passing.reportedFailure, "A passing test was failed: ${passing.reportedFailure?.message}")

        val message = crasher.reportedFailure?.message.orEmpty()
        assertTrue("no per-test result was reported for '${crasher.id}'" in message, message)
        assertTrue("it most likely crashed that VM" in message, message)
        assertFalse("was silently skipped" in message, message)
        // The crash is only diagnosable with the VM output, so it has to come along.
        assertTrue("Collected outputs:" in message, message)
    }

    @Test
    fun `given a VM that died without parsable output then its crash surfaces next to an unrelated test failure`() {
        val failing = GroupedTest("testFailing")

        // One engine ran the batch and reported the failure; the other died so early that it printed no block at all,
        // so no test's start localizes it and nothing above can be blamed for it.
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

        // Reported against the batch rather than against a test, since no test can be blamed — but reported, which is
        // the point: gating this on "some test failed" dropped it behind the assertion failure below.
        val thrown = assertThrows(Throwable::class.java) {
            runner(listOf(failing), vmStdout = listOf(finishedVmStdout), vmFailures = listOf(vmCrash))
                .processArtifact(FakeWasmArtifact)
        }
        assertEquals(vmCrash, thrown)

        // ...and the test that did report a failure is still failed with its own assertion.
        assertEquals("$FAILURE_MESSAGE\n$FAILURE_DETAILS", failing.reportedFailure?.message)
    }

    @Test
    fun `given a grouped batch whose driver never ran when the VM exits cleanly then every test is failed`() {
        val first = GroupedTest("testFirst")
        val second = GroupedTest("testSecond")

        // What a grouped batch prints when `test.mjs` does not find the driver export and falls back to the compiler's
        // `startUnitTests()`: the launcher classes carry no `@kotlin.test.Test`, so nothing runs, nothing is reported,
        // and the VM exits cleanly. Not a single test of the batch was actually executed.
        val stdoutWithoutBlock = "unrelated VM output\nwith no structured result block in it\n"

        runner(listOf(first, second), vmStdout = listOf(stdoutWithoutBlock), vmFailures = emptyList())
            .processArtifact(FakeWasmArtifact)

        for (test in listOf(first, second)) {
            val message = test.reportedFailure?.message.orEmpty()
            assertTrue(GroupedTestsResultProtocol.BEGIN in message, message)
            assertTrue("not a single test reported a result" in message, message)
        }
    }

    @Test
    fun `given a single isolated test without a structured block then only a VM failure fails it`() {
        // An isolated batch gets no driver at all, so the absence of a block is expected there: its verdict comes from
        // the `box()` check in the launcher glue, which makes the VM exit non-zero on failure.
        val passing = GroupedTest("testOnlyOne")
        runner(
            listOf(passing),
            vmStdout = listOf("output of the single-test runner\n"),
            vmFailures = emptyList(),
            driverGenerated = false,
        ).processArtifact(FakeWasmArtifact)
        assertNull(passing.reportedFailure, "An isolated passing test was failed: ${passing.reportedFailure?.message}")

        val failing = GroupedTest("testOnlyOne")
        val vmFailure = WasmVMException(AssertionError("Wrong box result 'FAIL'; Expected \"OK\""), vmName = "V8")
        runner(listOf(failing), vmStdout = emptyList(), vmFailures = listOf(vmFailure), driverGenerated = false)
            .processArtifact(FakeWasmArtifact)
        assertEquals(vmFailure, failing.reportedFailure)
    }

    @Test
    fun `given a single non-isolated test whose driver never ran then that test is failed`() {
        // A test that ended up alone in its batch without being isolated (a unique batch token) is still linked with the
        // driver, and its `box()` is reached through the launcher FQN rather than exported — so nothing but the result
        // block can report its verdict. Deciding this by batch size would report the clean VM exit below as a pass.
        val alone = GroupedTest("testAlone")

        runner(listOf(alone), vmStdout = listOf("VM output with no structured result block\n"), vmFailures = emptyList())
            .processArtifact(FakeWasmArtifact)

        val message = alone.reportedFailure?.message.orEmpty()
        assertTrue("not a single test reported a result" in message, message)
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
        // On the second VM `crasher` never got to report anything: it took the VM down while executing.
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
        ).processArtifact(FakeWasmArtifact)

        assertNull(other.reportedFailure, "A passing test was failed: ${other.reportedFailure?.message}")

        // Passing on one VM does not hide the crash of another.
        val message = crasher.reportedFailure?.message.orEmpty()
        assertTrue("it most likely crashed that VM" in message, message)
        // The crash is only diagnosable with the VM output, so it has to come along.
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
            vmFailures = listOf(vmCrash(crashedVmStdout, vmName = "WasmEdge")),
        ).processArtifact(FakeWasmArtifact)

        // One report carrying both halves: the assertion alone would hide that this test takes an engine down, and the
        // crash alone would hide what it actually asserted.
        val message = failingCrasher.reportedFailure?.message.orEmpty()
        assertTrue(FAILURE_MESSAGE in message, message)
        assertTrue(FAILURE_DETAILS in message, message)
        assertTrue("it most likely crashed that VM" in message, message)
    }

    /**
     * One test of the batch: its [KotlinTestInfo], the `ProxyLauncher_<hash>` [id] the protocol keys it by, and the
     * failure the runner attributed to it, if any.
     */
    private class GroupedTest(methodName: String) {
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
                register(TestModuleStructure::class, SingleBoxFileModuleStructure)
            },
            // Stands in for the test engine's executor: the runner reports a per-test failure by throwing inside this
            // block, so capturing it is exactly what the engine would attribute to this single test.
            catchingExecutor = { _, block ->
                try {
                    block()
                } catch (e: Throwable) {
                    failures += e
                }
            },
        )

        /** The failure attributed to this test; more than one attribution for one test is a bug in the runner. */
        val reportedFailure: Throwable?
            get() {
                assertTrue(failures.size <= 1, "Expected at most one failure for $id, got: $failures")
                return failures.singleOrNull()
            }
    }

    private fun runner(
        batch: List<GroupedTest>,
        vmStdout: List<String>,
        vmFailures: List<Throwable>,
        driverGenerated: Boolean = true,
    ): AbstractWasmGroupingStageBoxRunner {
        val testServices = TestServices().apply {
            register(GroupingStageInputsHolder::class, GroupingStageInputsHolder(batch.map { it.input }))
            // What the stage-2 facade records for a batch it linked with the generated driver.
            if (driverGenerated) markGroupedTestsDriverGenerated()
        }
        return object : AbstractWasmGroupingStageBoxRunner(testServices) {
            override fun shouldUseBoxExportMode(): Boolean = false

            override fun runTestCode(
                artifact: BinaryArtifacts.Wasm,
                useUnitTestRunnerOnly: Boolean,
                outputCollector: MutableList<String>?,
            ): List<Throwable> {
                // VMs that finished report through the collector; the ones that died report through exceptions.
                outputCollector?.addAll(vmStdout)
                return vmFailures
            }
        }
    }

    private companion object {
        /** A failure message with a raw separator in it, so the escaping has to carry it through the wire format. */
        const val FAILURE_MESSAGE = "Test failed with: FAIL|1. Expected <OK>, actual <FAIL|1>."

        /** A multi-line stack trace, which the protocol has to keep on a single wire line. */
        const val FAILURE_DETAILS =
            "AssertionError: boom | with a pipe\n\tat Foo.box(foo.kt:1)\n\tat ProxyLauncher.runTest(ProxyBatchLauncher.kt:3)"

        /** Reproduces how the generated driver prints a sentinel: on its own line, prefixed with a newline. */
        fun StringBuilder.appendProtocolSentinel(sentinel: String) {
            append("\n").append(sentinel).append("\n")
        }

        /** Reproduces how the generated driver prints a result line, including the empty message/details fields. */
        fun StringBuilder.appendProtocolLine(id: String, status: String, message: String = "", details: String = "") {
            append("\n")
            append(GroupedTestsResultProtocol.LINE_PREFIX)
            for (field in listOf(id, status, message, details)) {
                append(GroupedTestsResultProtocol.SEP).append(field)
            }
            append("\n")
        }

        /** How a crashed VM reports: `ExternalTool.run` raises the non-zero exit code with the captured stdout in it. */
        fun vmCrash(capturedStdout: String, vmName: String): Throwable = WasmVMException(
            AssertionError(
                "Command \"$vmName ./test.mjs\" terminated with exit code 133 in working dir \"/tmp/batch\"\n" +
                        "OUTPUT:\n$capturedStdout\n---"
            ),
            vmName = vmName,
        )

        /** Every grouped test must have a `box()`, or the runner rejects the batch as an infrastructure error. */
        val SingleBoxFileModuleStructure = object : TestModuleStructure() {
            override val modules: List<TestModule> = listOf(
                TestModule(
                    name = "main",
                    files = listOf(
                        TestFile(
                            relativePath = "main.kt",
                            originalContent = "fun box(): String = \"OK\"",
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

        val FakeWasmArtifact = object : BinaryArtifacts.Wasm() {}
    }
}
