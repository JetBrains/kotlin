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
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
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
     * Determines whether to use
     * - box-export mode: call `box()` directly and expect "OK" return value or
     * - unit-test mode: run the batch via the result-collecting driver and parse the structured
     *   [GroupedTestsResultProtocol] block from VM stdout.
     */
    protected abstract fun shouldUseBoxExportMode(): Boolean

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
        outputCollector: MutableList<String>?,
    ): List<Throwable>

    override fun processArtifact(artifact: BinaryArtifacts.Wasm) {
        val inputs = testServices.groupingStageInputs

        if (shouldUseBoxExportMode()) {
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
            val collectedOutputs = mutableListOf<String>()
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
        collectedOutputs: List<String>,
        exceptions: List<Throwable>,
    ) {
        // A VM-failure message embeds the stdout captured before the crash, so a partial block is recovered too.
        val texts = buildList {
            addAll(collectedOutputs)
            exceptions.forEach { throwable -> addAll(collectExceptionTexts(throwable)) }
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
            testServices.groupingStageInputs.forEach { input ->
                input.failWithCollectedOutputs(
                    texts,
                    "Sanity check failed: the grouped batch printed no '${GroupedTestsResultProtocol.BEGIN}' block, " +
                            "so not a single test reported a result. The launcher's result-collecting driver was " +
                            "never invoked — most likely its exported entry point " +
                            "(`runGroupedTests` on wasm-js, `startTest` on wasm-wasi) was missing or renamed, which " +
                            "means no test of this batch actually ran.",
                )
            }
            return
        }

        // A driverless batch is a single isolated test: nothing to demux, and any VM failure is that test's own.
        if (exceptions.isNotEmpty()) {
            testServices.groupingStageInputs.forEach { it.failWith(exceptions.firstWithOthersSuppressed()) }
        }
    }

    /** Attributes each per-test result to its grouping input, by the test's stable `ProxyLauncher_<hash>` id. */
    private fun attributeStructuredResults(
        parsedBatchResult: GroupedTestsResultProtocol.ParsedBatchResult,
        exceptions: List<Throwable>,
        texts: List<String>,
    ) {
        val testReport = parsedBatchResult.toTestReport()
        val expectedIds = testServices.groupingStageInputs.map { input ->
            computeProxyLauncherClassName(input.testServices.testInfo)
        }

        // Checked before anything is attributed, since a test that cannot report a result invalidates the whole batch.
        testServices.groupingStageInputs.firstOrNull { !hasBoxMethod(it) }?.let { input ->
            testInfraError(
                "Test ${input.testInfo} does not have a box() method, so its execution status cannot be reported " +
                        "via the grouped result protocol. Please isolate this test using either existing ways in " +
                        "WasmGroupingTestIsolator or add a new rule there."
            )
        }

        val excessiveIds = TestRunChecks.findExcessiveResults(expectedIds, testReport)
        // The driver is generated from this batch's own launcher names, so an unexpected id is nobody's test failure.
        checkTestInfrastructure(excessiveIds.isEmpty()) {
            "Grouped batch reported results for tests that are not part of it: $excessiveIds. Expected: $expectedIds"
        }

        // There is no batch-level failure sink, so an empty report is prepended to every missing test below.
        val emptyReportReason = (TestRunChecks.checkNonEmpty(testReport) as? TestRunChecks.Result.Failed)?.reason
        val missingIds = TestRunChecks.findMissingResults(expectedIds, testReport).toSet()
        val crashAttributedIds = mutableSetOf<String>()

        for (input in testServices.groupingStageInputs) {
            val id = computeProxyLauncherClassName(input.testServices.testInfo)
            when {
                id in missingIds -> {
                    input.failWithCollectedOutputs(
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
                        // Failed on one VM and took another down: the assertion alone would hide that it crashes an
                        // engine, the crash alone what it asserted.
                        input.failWithCollectedOutputs(texts, reportedFailure, crashDiagnosis(id, "another VM"))
                        crashAttributedIds += id
                    } else {
                        input.failWith(AssertionError(reportedFailure))
                    }
                }
                // Ran to completion on one VM but took another down: without this the surviving VM's result would
                // report it as passing.
                parsedBatchResult.crashedInProgress(id) -> {
                    input.failWithCollectedOutputs(texts, crashDiagnosis(id, "another VM"))
                    crashAttributedIds += id
                }
            }
        }

        // An exception no attributed crash explains is surfaced even when other tests of the batch already failed:
        // keying this on "some test failed" would let an engine-specific crash hide behind an unrelated assertion.
        val unexplainedExceptions = exceptions.filter { exception ->
            val crashedThere = GroupedTestsResultProtocol.parseMerged(collectExceptionTexts(exception)).crashedIds
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

    /** Fails this specific test with [lines] (`null` ones are dropped) and every text the batch collected. */
    private fun NonGroupingStageOutput.failWithCollectedOutputs(texts: List<String>, vararg lines: String?) {
        failWith(AssertionError((lines.filterNotNull() + "Collected outputs:" + texts).joinToString("\n")))
    }

    /** Routes [error] into this test's own failure sink, so the test engine attributes it to this test. */
    private fun NonGroupingStageOutput.failWith(error: Throwable) {
        catchingExecutor.executeWithCatching({ WrappedException.FromGroupingHandler(it, this@AbstractWasmGroupingStageBoxRunner) }) {
            throw error
        }
    }

    /** Several VMs run the same batch, so reporting only the first exception would drop what the others saw. */
    private fun List<Throwable>.firstWithOthersSuppressed(): Throwable = first().also { first ->
        // `addSuppressed` rejects the throwable itself, and the same instance may well be reported by two VMs.
        drop(1).forEach { other -> if (other !== first) first.addSuppressed(other) }
    }

    /** The message of [throwable] and of its causes; a VM-failure message embeds the stdout captured before the crash. */
    private fun collectExceptionTexts(throwable: Throwable): List<String> {
        val texts = mutableListOf<String>()
        var current: Throwable? = throwable
        while (current != null) {
            current.message?.let { message ->
                if (message !in texts) {
                    texts += message
                }
            }
            current = current.cause
        }
        return texts
    }

    /**
     * A test without a `box()` (e.g. a `// FILE: entry.mjs` driven size test) runs through a custom JS entry point
     * rather than through its `ProxyLauncher_<hash>`, so it cannot report a per-test result line.
     */
    protected fun hasBoxMethod(input: NonGroupingStageOutput): Boolean {
        val moduleStructure = input.testServices.moduleStructure
        for (module in moduleStructure.modules) {
            for (file in module.files) {
                if (MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(file.originalContent)) {
                    return true
                }
            }
        }
        return false
    }
}
