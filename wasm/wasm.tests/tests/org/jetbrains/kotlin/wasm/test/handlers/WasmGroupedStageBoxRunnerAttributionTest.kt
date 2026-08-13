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
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.BatchingPackageInserter.Companion.computePackage
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.SourceFileProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.wasm.test.blackbox.computeProxyLauncherClassName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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

        assertVerdict(GroupedTestVerdict.FAILED, failing)
        assertEquals("$FAILURE_MESSAGE\n$FAILURE_DETAILS", failing.reportedFailure?.message)

        assertVerdict(GroupedTestVerdict.MISSING, neverRan)
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
        assertEquals("$FAILURE_MESSAGE\n$FAILURE_DETAILS", failing.reportedFailure?.message)
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

        assertVerdict(GroupedTestVerdict.CRASHED, crasher)
        val message = crasher.reportedFailure?.message.orEmpty()
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

        assertEquals("$FAILURE_MESSAGE\n$FAILURE_DETAILS", failing.reportedFailure?.message)
    }

    @Test
    fun `given a grouped batch whose driver never ran when the VM exits cleanly then every test is failed`() {
        val first = GroupedTest("testFirst")
        val second = GroupedTest("testSecond")

        val stdoutWithoutBlock = "unrelated VM output\nwith no structured result block in it\n"

        runner(listOf(first, second), vmStdout = listOf(stdoutWithoutBlock), vmFailures = emptyList())
            .processArtifact(DriverLinkedBatchArtifact)

        for (test in listOf(first, second)) {
            assertVerdict(GroupedTestVerdict.NO_RESULT_BLOCK, test)
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
            assertVerdict(GroupedTestVerdict.NO_RESULT_BLOCK, test)
            val message = test.reportedFailure?.message.orEmpty()
            assertTrue("VM-2" in message, message)
        }
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
            assertVerdict(GroupedTestVerdict.INCOMPLETE_RESULT_BLOCK, test)
            val message = test.reportedFailure?.message.orEmpty()
            assertTrue("VM-2" in message, message)
        }
    }

    @Test
    fun `given a single isolated test without a structured block then only a VM failure fails it`() {
        val passing = GroupedTest("testOnlyOne")
        runner(
            listOf(passing),
            vmStdout = listOf("output of the single-test runner\n"),
            vmFailures = emptyList(),
        ).processArtifact(DriverlessBatchArtifact)
        assertNull(passing.reportedFailure, "An isolated passing test was failed: ${passing.reportedFailure?.message}")

        val failing = GroupedTest("testOnlyOne")
        val vmFailure = WasmVMException(AssertionError("Wrong box result 'FAIL'; Expected \"OK\""), vmName = "V8")
        runner(listOf(failing), vmStdout = emptyList(), vmFailures = listOf(vmFailure))
            .processArtifact(DriverlessBatchArtifact)
        assertEquals(vmFailure, failing.reportedFailure)
    }

    @Test
    fun `given a single non-isolated test whose driver never ran then that test is failed`() {
        val alone = GroupedTest("testAlone")

        runner(listOf(alone), vmStdout = listOf("VM output with no structured result block\n"), vmFailures = emptyList())
            .processArtifact(DriverLinkedBatchArtifact)

        assertVerdict(GroupedTestVerdict.NO_RESULT_BLOCK, alone)
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

        assertVerdict(GroupedTestVerdict.CRASHED, crasher)
        val message = crasher.reportedFailure?.message.orEmpty()
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
        ).processArtifact(DriverLinkedBatchArtifact)

        assertVerdict(GroupedTestVerdict.CRASHED, failingCrasher)
        val message = failingCrasher.reportedFailure?.message.orEmpty()
        assertTrue(FAILURE_MESSAGE in message, message)
        assertTrue(FAILURE_DETAILS in message, message)
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

    private fun assertVerdict(expected: GroupedTestVerdict, test: GroupedTest) {
        val failure = test.reportedFailure
        assertEquals(expected, (failure as? GroupedTestFailure)?.verdict, failure?.message ?: "${test.id} was not failed")
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
        vmFailures: List<Throwable>,
        boxExportMode: Boolean = false,
    ): AbstractWasmGroupingStageBoxRunner {
        val testServices = TestServices().apply {
            register(GroupingStageInputsHolder::class, GroupingStageInputsHolder(batch.map { it.input }))
        }
        return object : AbstractWasmGroupingStageBoxRunner(testServices) {
            override fun shouldUseBoxExportModeWhenDriverless(): Boolean = boxExportMode

            override fun runTestCode(
                artifact: BinaryArtifacts.Wasm,
                useUnitTestRunnerOnly: Boolean,
                outputCollector: MutableList<WasmVMOutput>?,
            ): List<Throwable> {
                outputCollector?.addAll(vmStdout.mapIndexed { index, output ->
                    WasmVMOutput(vmName = "VM-${index + 1}", output = output)
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

        fun vmCrash(capturedStdout: String, vmName: String): Throwable = WasmVMException(
            AssertionError(
                "Command \"$vmName ./test.mjs\" terminated with exit code 133 in working dir \"/tmp/batch\"\n" +
                        "OUTPUT:\n$capturedStdout\n---"
            ),
            vmName = vmName,
            output = capturedStdout,
        )

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

        val DriverLinkedBatchArtifact = object : BinaryArtifacts.Wasm() {
            override val hasGroupedTestsDriver: Boolean get() = true
        }

        val DriverlessBatchArtifact = object : BinaryArtifacts.Wasm() {}
    }
}
