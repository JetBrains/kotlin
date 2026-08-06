/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol
import org.jetbrains.kotlin.test.grouping.hasGroupedTestsDriver
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
 *
 * A test whose `ProxyLauncher_<hash>` id is missing from the reported results is failed with a sanity error, which
 * keeps a silently skipped test from being reported as passing.
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
            // Unit test mode: run the batch and parse the structured GroupedTestsResultProtocol block from stdout.
            val collectedOutputs = mutableListOf<String>()
            val exceptions = runTestCode(
                artifact,
                useUnitTestRunnerOnly = true,
                outputCollector = collectedOutputs,
            )
            handleRunResult(collectedOutputs = collectedOutputs, exceptions = exceptions)
        }
    }

    /**
     * @param collectedOutputs the stdout captured from every VM that finished normally
     * @param exceptions whatever the VM wrappers threw (a failed run, or a failure detected in the output)
     */
    private fun handleRunResult(collectedOutputs: List<String>, exceptions: List<Throwable>) {
        // Every text that may carry the structured block: the stdout of VMs that finished normally, plus the messages
        // of VM-failure exceptions, which embed the captured stdout — so a partial block of a VM that died mid-batch
        // is recovered too.
        val texts = buildList {
            addAll(collectedOutputs)
            exceptions.forEach { throwable -> addAll(collectExceptionTexts(throwable)) }
        }

        val parsedBatchResult = GroupedTestsResultProtocol.parseMerged(texts)
        if (parsedBatchResult.sawStructuredBlock) {
            attributeStructuredResults(parsedBatchResult, exceptions, texts)
            return
        }

        // A batch linked with the driver reports every verdict through it, so no block at all means the driver was never
        // invoked: `test.mjs` did not find its exported entry point and fell back to `startUnitTests()`, which now
        // finds nothing to run since the launcher classes carry no `@kotlin.test.Test`. That exits cleanly, so the
        // whole batch would be reported green without a single test having run — fail every test of it instead.
        //
        // Keyed on what the stage-2 facade actually generated, not on the batch size: a test that ended up alone in its
        // batch without being isolated is still driver-driven and has no `box()` export to fall back on, so trusting the
        // launcher glue for it would let exactly this silent-green case through.
        if (testServices.hasGroupedTestsDriver) {
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

        // A batch linked without the driver is a single isolated test, so a missing block is expected here and there is
        // nothing to demux: any VM failure is unambiguously that test's own, and its pass verdict comes from the
        // `box()`/`hasTestFailures` checks of the launcher glue, which make the VM exit non-zero on failure.
        if (exceptions.isNotEmpty()) {
            testServices.groupingStageInputs.forEach { it.failWith(exceptions.firstWithOthersSuppressed()) }
        }
    }

    /**
     * Attributes each per-test result to the grouping input it belongs to, by the test's stable `ProxyLauncher_<hash>`
     * id. A test the batch reported no result for is failed with a sanity error, so that a silently skipped test
     * cannot masquerade as passing.
     */
    private fun attributeStructuredResults(
        parsedBatchResult: GroupedTestsResultProtocol.ParsedBatchResult,
        exceptions: List<Throwable>,
        texts: List<String>,
    ) {
        val testReport = parsedBatchResult.toTestReport()
        val expectedIds = testServices.groupingStageInputs.map { input ->
            computeProxyLauncherClassName(input.testServices.testInfo)
        }

        // Every grouped test is run through its launcher's `runTest()`, which asserts `box() == "OK"`, so it must have a
        // `box()`. A test without one is driven by a custom JS entry point and cannot report a result line, so it has to
        // be isolated. Checked before anything is attributed, since it invalidates the whole batch rather than one test.
        testServices.groupingStageInputs.firstOrNull { !hasBoxMethod(it) }?.let { input ->
            testInfraError(
                "Test ${input.testInfo} does not have a box() method, so its execution status cannot be reported " +
                        "via the grouped result protocol. Please isolate this test using either existing ways in " +
                        "WasmGroupingTestIsolator or add a new rule there."
            )
        }

        val excessiveIds = TestRunChecks.findExcessiveResults(expectedIds, testReport)
        // The driver is generated from this batch's own launcher class names, so an unexpected id means the launcher
        // and the report went out of sync. No individual test is to blame for that, hence a test infrastructure error.
        checkTestInfrastructure(excessiveIds.isEmpty()) {
            "Grouped batch reported results for tests that are not part of it: $excessiveIds. Expected: $expectedIds"
        }

        // With not a single reported result the batch as a whole is broken, rather than one test being skipped. There
        // is no batch-level failure sink, so that reason is prepended to the report of every missing test below.
        val emptyReportReason = (TestRunChecks.checkNonEmpty(testReport) as? TestRunChecks.Result.Failed)?.reason
        val missingIds = TestRunChecks.findMissingResults(expectedIds, testReport).toSet()
        var anyFailureAttributed = false

        for (input in testServices.groupingStageInputs) {
            val id = computeProxyLauncherClassName(input.testServices.testInfo)
            when {
                id in missingIds -> {
                    input.failWithCollectedOutputs(
                        texts,
                        emptyReportReason,
                        "Sanity check failed: no per-test result was reported for '$id' in the grouped batch. " +
                                "The test was expected to run as part of the batch, but produced no " +
                                "'${GroupedTestsResultProtocol.LINE_PREFIX}' line. This typically indicates the test " +
                                "was silently skipped (e.g. a stripped ProxyLauncher class), or that a VM crashed " +
                                "before this test's launcher was reached.",
                    )
                    anyFailureAttributed = true
                }
                id in testReport.failedTests -> {
                    // The reported message and stack trace say everything, so the collected outputs are not repeated.
                    val outcome = parsedBatchResult.outcomes.getValue(id)
                    input.failWith(AssertionError(listOfNotNull(outcome.message, outcome.details).joinToString("\n")))
                    anyFailureAttributed = true
                }
            }
        }

        // A VM may have thrown (e.g. a hard trap) without that surfacing as a per-test failure above. Then surface the
        // raw VM exception, so that the batch does not pass silently.
        if (!anyFailureAttributed && exceptions.isNotEmpty()) {
            throw exceptions.firstWithOthersSuppressed()
        }
    }

    /**
     * Fails this specific test with [lines] (`null` ones are dropped) followed by every text the batch collected, so
     * that the test engine reports it against this test instead of against the whole batch.
     */
    private fun NonGroupingStageOutput.failWithCollectedOutputs(texts: List<String>, vararg lines: String?) {
        failWith(AssertionError((lines.filterNotNull() + "Collected outputs:" + texts).joinToString("\n")))
    }

    /** Routes [error] into this test's own failure sink, so that the test engine attributes it to this test. */
    private fun NonGroupingStageOutput.failWith(error: Throwable) {
        catchingExecutor.executeWithCatching({ WrappedException.FromGroupingHandler(it, this@AbstractWasmGroupingStageBoxRunner) }) {
            throw error
        }
    }

    /**
     * The first exception of this list, carrying the remaining ones as suppressed. Several VMs run the same batch, so
     * reporting only the first would drop what the others saw.
     */
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
     * Returns `true` if any module of [input] contains a file with a top-level `box()` function.
     *
     * Tests without a `box()` (e.g. `// FILE: entry.mjs` driven Wasm/JS size tests) are executed via a custom JS entry
     * point, not via the synthetic `ProxyLauncher_<hash>` classes, so they cannot report a per-test result line.
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
