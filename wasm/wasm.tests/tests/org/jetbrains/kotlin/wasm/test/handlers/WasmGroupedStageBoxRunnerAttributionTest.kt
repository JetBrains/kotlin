/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.test.GroupingStageInputsHolder
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.grouping.GroupedTestsExportedEntryPointGenerator
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.wasm.test.blackbox.computeProxyLauncherClassName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * End-to-end test of the JVM side of the grouped K/Wasm run: it drives the real
 * [AbstractWasmGroupingStageBoxRunner.processArtifact] over the stdout a batch of tests would produce on several
 * VMs, and checks that every outcome lands on the test that caused it.
 *
 * The VMs are the only thing faked. Their stdout is assembled from the real [GroupedTestsResultProtocol] constants
 * and [GroupedTestsResultProtocol.escape] — exactly the bytes the generated driver prints — and a crashed VM's
 * output arrives the way it really does: not in the output collector, but embedded in the failure that
 * `ExternalTool.run` raises for a non-zero exit code, wrapped in a [WasmVMException].
 */
class WasmGroupedStageBoxRunnerAttributionTest {
    @Test
    fun `given mid-line noise, a one-VM crasher, escaped failure text and a never-run test then each is attributed on its own`() {
        val passing = GroupedTest("testPassing")
        val failing = GroupedTest("testFailing")
        val crasher = GroupedTest("testCrasher")
        val neverRan = GroupedTest("testNeverRan")
        val batch = listOf(passing, failing, crasher, neverRan)

        // Keep the simulated stdout honest: the driver must really print its lines the way this test builds them,
        // newline-prefixed so that a test's leftover output cannot glue onto the line that follows it.
        val driverSource = GroupedTestsResultProtocol.generateResultCollectingRunnerSource(
            proxyClassNames = batch.map { it.id },
            exportedEntryPointGenerator = NoOpEntryPointGenerator,
        )
        val printedLinePrefix = "println(\"\\n" + GroupedTestsResultProtocol.LINE_PREFIX + GroupedTestsResultProtocol.SEP + "\""
        assertTrue(printedLinePrefix in driverSource, driverSource)

        // A VM that finished the whole batch: `passing` also writes to stdout without a trailing newline, so its
        // terminal line would be glued onto that leftover if the driver did not prefix every line with '\n'.
        val finishedVmStdout = buildString {
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
            appendProtocolLine(crasher.id, GroupedTestsResultProtocol.STARTED)
            appendProtocolLine(crasher.id, GroupedTestsResultProtocol.PASSED)
            appendProtocolSentinel(GroupedTestsResultProtocol.END)
        }

        // A second VM that `crasher` took down: it never printed a terminal line for it, nor the END sentinel, and
        // never reached `neverRan`. Note that `crasher` passed on the VM above — the crash must still be reported.
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
            appendProtocolLine(crasher.id, GroupedTestsResultProtocol.STARTED)
        }
        val crashedVmFailure = WasmVMException(
            AssertionError(
                "Command \"js --module ./test.mjs\" terminated with exit code 133 in working dir \"/tmp/batch\"\n" +
                        "OUTPUT:\n$crashedVmStdout\n---"
            ),
            vmName = "SpiderMonkey",
        )

        runner(batch, vmStdout = listOf(finishedVmStdout), vmFailures = listOf(crashedVmFailure))
            .processArtifact(FakeWasmArtifact)

        // Mid-line noise did not swallow the terminal line of `passing`.
        assertNull(passing.reportedFailure, "A passing test was failed: ${passing.reportedFailure?.message}")

        // The failure text survived escaping, transport and unescaping: separators and line breaks are intact, and
        // the runner joins the message with the stack trace.
        assertEquals("$FAILURE_MESSAGE\n$FAILURE_DETAILS", failing.reportedFailure?.message)

        // Passing on one VM does not hide the crash of another.
        val crasherMessage = crasher.reportedFailure?.message.orEmpty()
        assertTrue("reported a result on at least one VM" in crasherMessage, crasherMessage)
        assertTrue("most likely crashed that VM" in crasherMessage, crasherMessage)

        // No line at all, not even a STARTED one: reported as never run rather than as a crasher.
        val neverRanMessage = neverRan.reportedFailure?.message.orEmpty()
        assertTrue("no per-test result was reported for '${neverRan.id}'" in neverRanMessage, neverRanMessage)
        assertTrue("silently skipped" in neverRanMessage, neverRanMessage)
        assertTrue("most likely crashed the VM" !in neverRanMessage, neverRanMessage)
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
            // Stands in for the test engine's executor: the runner reports a per-test failure by throwing inside
            // this block, so capturing it is exactly what JUnit would attribute to this single test.
            catchingExecutor = NonGroupingStageOutput.CatchingExecutor { _: (Throwable) -> WrappedException, block: () -> Unit ->
                try {
                    block()
                } catch (e: Throwable) {
                    failures += e
                }
            },
        )

        val reportedFailure: Throwable?
            // TODO KT-87785: get() silently tolerates multiple failures attributed to one test. That's a reachable bug class
            // (the excessive-ids branch blames groupingStageInputs.first(), which can also be missing or failed → two attributions),
            // and the test would report the first and pass. Should be singleOrNull() plus an explicit assertion on failures.size.
            get() = failures.singleOrNull() ?: failures.firstOrNull()
    }

    private fun runner(
        batch: List<GroupedTest>,
        vmStdout: List<String>,
        vmFailures: List<Throwable>,
    ): AbstractWasmGroupingStageBoxRunner {
        val testServices = TestServices().apply {
            register(GroupingStageInputsHolder::class, GroupingStageInputsHolder(batch.map { it.input }))
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
        val FAILURE_DETAILS =
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

        /** Only the target-independent part of the driver matters here, so the entry point can be anything. */
        val NoOpEntryPointGenerator = object : GroupedTestsExportedEntryPointGenerator() {
            override fun generateExportedEntryPointSource(runAllFunctionName: String): String =
                "fun entryPoint() { $runAllFunctionName() }"
        }
    }
}
