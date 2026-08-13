/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.GroupingStageHandler
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.report.TestRunChecks
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceProviders.hasBoxMethod
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.wasm.test.blackbox.computeProxyLauncherClassName

/**
 * Shared base class for grouping stage handlers in WASM test infrastructure.
 *
 * Encapsulates code common to JS and WASI folder-based grouped runs:
 *   - dispatching test execution to VMs and collecting their outputs/exceptions;
 *   - attributing the per-test results the launcher's driver printed (see [GroupedTestsResultProtocol]) back to the
 *     individual grouping inputs via their [NonGroupingStageOutput.catchingExecutor], so that the test engine reports
 *     each failure against the specific test rather than against the whole batch.
 */
abstract class AbstractWasmGroupingStageBoxRunner(
    testServices: TestServices
) : GroupingStageHandler<BinaryArtifacts.Wasm>(
    testServices,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false,
) {
    override val artifactKind: TestArtifactKind<BinaryArtifacts.Wasm>
        get() = ArtifactKinds.Wasm

    /**
     * Whether a *driverless* artifact is run in box-export mode, calling `box()` directly and expecting "OK",
     * rather than through the unit-test runner.
     */
    protected abstract fun shouldUseBoxExportModeWhenDriverless(): Boolean

    /**
     * Runs the test code for the given artifact and returns any exceptions that occurred.
     *
     * @param artifact the compiled WASM artifact to execute
     * @param useUnitTestRunnerOnly if true, use the unit-test runner; if false, call `box()` directly
     * @param outputCollector if non-null, collects stdout from VM executions (for [GroupedTestsResultProtocol] parsing)
     * @return list of exceptions thrown during test execution
     */
    protected abstract fun runTestCode(
        artifact: BinaryArtifacts.Wasm,
        useUnitTestRunnerOnly: Boolean,
        outputCollector: MutableList<WasmVMOutput>?,
    ): List<Throwable>

    override fun processArtifact(artifact: BinaryArtifacts.Wasm) {
        val inputs = testServices.groupingStageInputs
        // Run mode must be perfectly matched to the way the batch was compiled,
        // otherwise the per-test results printed by the driver are never parsed and the batch passes for free.
        val useBoxExportMode = !artifact.hasGroupedTestsDriver && shouldUseBoxExportModeWhenDriverless()

        if (useBoxExportMode) {
            // Box export mode: call box() directly and expect "OK"
            val input = inputs.first()
            val exceptions = runTestCode(
                artifact,
                useUnitTestRunnerOnly = false,
                outputCollector = null,
            )
            if (exceptions.isNotEmpty()) {
                input.failWith(exceptions.first())
            }
        } else {
            // Unit test mode: run the batch and parse the structured result block from stdout.
            val collectedOutputs = mutableListOf<WasmVMOutput>()
            val exceptions = runTestCode(
                artifact,
                useUnitTestRunnerOnly = true,
                outputCollector = collectedOutputs,
            )
            handleRunResult(artifact, collectedOutputs = collectedOutputs, exceptions = exceptions)
        }
    }

    private fun handleRunResult(
        artifact: BinaryArtifacts.Wasm,
        collectedOutputs: List<WasmVMOutput>,
        exceptions: List<Throwable>,
    ) {
        // A VM failure carries the stdout captured before the crash, so a partial block is recovered too.
        val texts = collectedOutputs.map { it.output } + exceptions.mapNotNull { it.capturedVmOutput() }

        if (artifact.hasGroupedTestsDriver) {
            val vmsWithoutBlock = collectedOutputs.filter { output ->
                !GroupedTestsResultProtocol.parseMerged(listOf(output.output)).sawStructuredBlock
            }.map { it.vmName }.distinct()
            if (vmsWithoutBlock.isNotEmpty()) {
                failWholeBatch(
                    GroupedTestVerdict.NO_RESULT_BLOCK,
                    texts,
                    "Sanity check failed: the grouped batch did not print a " +
                            "'${GroupedTestsResultProtocol.BEGIN}' block for every driver-enabled VM. " +
                            "Missing from: ${vmsWithoutBlock.joinToString()}. A VM exited successfully without invoking the " +
                            "launcher's result-collecting driver; not a single test reported a result on that " +
                            "VM, so the results from the other VMs cannot establish complete test coverage.",
                )
                return
            }

            val vmsWithIncompleteBlock = collectedOutputs.filter { output ->
                !GroupedTestsResultProtocol.hasCompleteStructuredBlock(output.output)
            }.map { it.vmName }.distinct()
            if (vmsWithIncompleteBlock.isNotEmpty()) {
                failWholeBatch(
                    GroupedTestVerdict.INCOMPLETE_RESULT_BLOCK,
                    texts,
                    "Sanity check failed: the grouped batch did not print a complete " +
                            "'${GroupedTestsResultProtocol.BEGIN}'/'${GroupedTestsResultProtocol.END}' block for " +
                            "every driver-enabled VM. Incomplete on: ${vmsWithIncompleteBlock.joinToString()}. A VM exited " +
                            "successfully before the launcher's result-collecting driver completed, so the " +
                            "results from the other VMs cannot establish complete test coverage.",
                )
                return
            }
        }

        val parsedBatchResult = GroupedTestsResultProtocol.parseMerged(texts)
        if (parsedBatchResult.sawStructuredBlock) {
            attributeStructuredResults(parsedBatchResult, exceptions, texts)
            return
        }

        // A driver-linked batch reports every verdict through the driver, so no block at all means it was never
        // invoked: `test.mjs` fell back to `startUnitTests()`, which finds nothing to run (the launcher classes carry
        // no `@kotlin.test.Test`) and exits cleanly — the batch would be green with no test having run.
        if (artifact.hasGroupedTestsDriver) {
            failWholeBatch(
                GroupedTestVerdict.NO_RESULT_BLOCK,
                texts,
                "Sanity check failed: the grouped batch printed no '${GroupedTestsResultProtocol.BEGIN}' block, " +
                        "so not a single test reported a result. The launcher's result-collecting driver was " +
                        "never invoked — most likely its exported entry point " +
                        "(`runGroupedTests` on wasm-js, `startTest` on wasm-wasi) was missing or renamed, which " +
                        "means no test of this batch actually ran.",
            )
            return
        }

        if (exceptions.isNotEmpty()) {
            testServices.groupingStageInputs.forEach { it.failWith(exceptions.firstWithOthersSuppressed()) }
        }
    }

    /** Fails every test of a batch whose results cannot establish coverage at all. */
    private fun failWholeBatch(verdict: GroupedTestVerdict, texts: List<String>, reason: String) {
        testServices.groupingStageInputs.forEach { input ->
            input.failWithVerdict(verdict, texts, reason)
        }
    }

    private fun attributeStructuredResults(
        parsedBatchResult: GroupedTestsResultProtocol.ParsedBatchResult,
        exceptions: List<Throwable>,
        texts: List<String>,
    ) {
        val testReport = parsedBatchResult.toTestReport()
        val expectedIds = testServices.groupingStageInputs.map { input ->
            computeProxyLauncherClassName(input.testServices.testInfo)
        }

        testServices.groupingStageInputs.firstOrNull { !it.hasBoxMethod() }?.let { input ->
            testInfraError(
                "Test ${input.testInfo} does not have a box() method, so its execution status cannot be reported " +
                        "via the grouped result protocol. Please isolate this test using either existing ways in " +
                        "WasmGroupingTestIsolator or add a new rule there."
            )
        }

        val excessiveIds = TestRunChecks.findExcessiveResults(expectedIds, testReport)
        checkTestInfrastructure(excessiveIds.isEmpty()) {
            "Grouped batch reported results for tests that are not part of it: $excessiveIds. Expected: $expectedIds"
        }

        val emptyReportReason = TestRunChecks.emptyReportReason(testReport)
        val missingIds = TestRunChecks.findMissingResults(expectedIds, testReport).toSet()
        val crashAttributedIds = mutableSetOf<String>()

        for (input in testServices.groupingStageInputs) {
            val id = computeProxyLauncherClassName(input.testServices.testInfo)
            when {
                id in missingIds -> {
                    input.failWithVerdict(
                        if (parsedBatchResult.crashedInProgress(id)) GroupedTestVerdict.CRASHED else GroupedTestVerdict.MISSING,
                        texts,
                        emptyReportReason,
                        "Sanity check failed: no per-test result was reported for '$id' in the grouped batch.",
                        if (parsedBatchResult.crashedInProgress(id)) {
                            crashDiagnosis(id, "the VM")
                        } else {
                            "The test was expected to run as part of the batch, but produced no " +
                                    "'${GroupedTestsResultProtocol.LINE_PREFIX}' line, not even a " +
                                    "'${GroupedTestsResultProtocol.STARTED}' one. This typically indicates the test " +
                                    "was silently skipped (e.g. a stripped ProxyLauncher class), or that a VM crashed " +
                                    "before this test's launcher was reached."
                        },
                    )
                    if (parsedBatchResult.crashedInProgress(id)) crashAttributedIds += id
                }
                id in testReport.failedTests -> {
                    val outcome = parsedBatchResult.outcomes.getValue(id)
                    val reportedFailure = listOfNotNull(outcome.message, outcome.details).joinToString("\n")
                    if (parsedBatchResult.crashedInProgress(id)) {
                        input.failWithVerdict(GroupedTestVerdict.CRASHED, texts, reportedFailure, crashDiagnosis(id, "another VM"))
                        crashAttributedIds += id
                    } else {
                        input.failWith(GroupedTestFailure(GroupedTestVerdict.FAILED, reportedFailure))
                    }
                }
                parsedBatchResult.crashedInProgress(id) -> {
                    input.failWithVerdict(GroupedTestVerdict.CRASHED, texts, crashDiagnosis(id, "another VM"))
                    crashAttributedIds += id
                }
            }
        }

        val unexplainedExceptions = exceptions.filter { exception ->
            val crashedThere = GroupedTestsResultProtocol.parseMerged(listOfNotNull(exception.capturedVmOutput())).crashedIds
            crashedThere.none { it in crashAttributedIds }
        }
        if (unexplainedExceptions.isNotEmpty()) {
            throw unexplainedExceptions.firstWithOthersSuppressed()
        }
    }

    private fun crashDiagnosis(id: String, vm: String): String =
        "Test '$id' printed a '${GroupedTestsResultProtocol.STARTED}' line on $vm with no terminal " +
                "'${GroupedTestsResultProtocol.PASSED}'/'${GroupedTestsResultProtocol.FAILED}' result — it most " +
                "likely crashed that VM (a hard trap, OOM, or process exit) while executing."

    private fun NonGroupingStageOutput.failWithVerdict(verdict: GroupedTestVerdict, texts: List<String>, vararg lines: String?) {
        failWith(GroupedTestFailure(verdict, (lines.filterNotNull() + "Collected outputs:" + texts).joinToString("\n")))
    }

    private fun NonGroupingStageOutput.failWith(error: Throwable) {
        catchingExecutor.executeWithCatching({ WrappedException.FromGroupingHandler(it, this@AbstractWasmGroupingStageBoxRunner) }) {
            throw error
        }
    }

    private fun List<Throwable>.firstWithOthersSuppressed(): Throwable = first().also { first ->
        drop(1).forEach { other -> if (other !== first) first.addSuppressed(other) }
    }
}

/** The verdict the grouped runner reached for a test, carried by the [GroupedTestFailure] it reports for that test. */
internal enum class GroupedTestVerdict {
    /** The test reported a failure. */
    FAILED,

    /** The test started and never reported a result: it most likely took its VM down. */
    CRASHED,

    /** The test reported no result at all. */
    MISSING,

    /** A driver-linked VM printed no result block at all. */
    NO_RESULT_BLOCK,

    /** A driver-linked VM exited cleanly with its result block left open. */
    INCOMPLETE_RESULT_BLOCK,
}

/** A grouped test's failure. Its message explains [verdict] and keeps the evidence behind it. */
internal class GroupedTestFailure(val verdict: GroupedTestVerdict, message: String) : AssertionError(message)

/** The stdout a failed VM captured before failing, if any: the results printed before a crash are still evidence. */
private fun Throwable.capturedVmOutput(): String? =
    generateSequence(this) { it.cause }.filterIsInstance<WasmVMException>().firstOrNull()?.output
