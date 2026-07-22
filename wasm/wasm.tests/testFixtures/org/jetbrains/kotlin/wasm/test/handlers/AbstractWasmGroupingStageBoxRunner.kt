/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.GroupingStageHandler
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol
import org.jetbrains.kotlin.test.grouping.TestRunChecks
import org.jetbrains.kotlin.wasm.test.blackbox.computeProxyLauncherClassName

/**
 * Shared base class for grouping stage handlers in WASM test infrastructure.
 *
 * Encapsulates code common to JS and WASI folder-based grouped runs:
 *   - dispatching test execution to VMs and collecting their outputs/exceptions;
 *   - re-attributing per-test results to the individual grouping inputs via their
 *     [NonGroupingStageOutput.catchingExecutor], using the structured result block printed by the launcher's
 *     result-collecting driver (see [GroupedTestsResultProtocol]) instead of scraping TeamCity service messages.
 *     A test whose stable `ProxyLauncher_<hash>` id is missing from the reported results is failed with a
 *     sanity error, which also guards against a test being silently skipped.
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
     * Holder for a single VM-execution result: the captured stdout (if the run succeeded)
     * and any exception thrown by the VM wrapper (if the run failed or detected a failure
     * in the output).
     */
    protected data class RunResult(
        val collectedOutputs: List<String>,
        val exceptions: List<Throwable>,
    )

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
                input.catchingExecutor.executeWithCatching({ WrappedException.FromGroupingHandler(it, this) }) {
                    throw exceptions.first()
                }
            }
        } else {
            // Unit test mode: run the batch and parse the structured GroupedTestsResultProtocol block from stdout.
            val collectedOutputs = mutableListOf<String>()
            val exceptions = runTestCode(
                artifact,
                useUnitTestRunnerOnly = true,
                outputCollector = collectedOutputs,
            )
            handleRunResult(RunResult(collectedOutputs, exceptions))
        }
    }

    protected fun handleRunResult(runResult: RunResult) {
        val (collectedOutputs, exceptions) = runResult

        // Collect every text that may carry the structured result block: stdout of VMs that finished normally
        // (collectedOutputs) plus any VM-failure exception messages, which embed the captured stdout — so a
        // partial block from a VM that crashed mid-batch is still recovered.
        val texts = buildList {
            addAll(collectedOutputs)
            exceptions.forEach { throwable -> addAll(collectExceptionTexts(throwable)) }
        }

        val parsedBatchResult = GroupedTestsResultProtocol.parseMerged(texts)

        if (parsedBatchResult.sawStructuredBlock) {
            attributeStructuredResults(parsedBatchResult, exceptions, texts)
            return
        }

        // Legacy single-test unit-test batch: no result-collecting driver was generated (the launcher exports no
        // `runGroupedTests`/`startTest`, so the VM fell back to the compiler's `startUnitTests()`). There is
        // exactly one test in such a batch, so any failure is unambiguously attributed to it — no demux needed.
        if (exceptions.isNotEmpty()) {
            val input = testServices.groupingStageInputs.first()
            input.catchingExecutor.executeWithCatching({ WrappedException.FromGroupingHandler(it, this) }) {
                throw exceptions.first()
            }
        }
    }


    /**
     * Attributes structured per-test results back to the individual grouping inputs via their
     * [NonGroupingStageOutput.catchingExecutor], so JUnit reports each failure against the specific test rather
     * than against the whole batch. A test whose stable `ProxyLauncher_<hash>` id is absent from [results] is
     * failed with a sanity error: it was expected to run in the batch but produced no result line. The message
     * distinguishes a test that crashed the VM mid-run (a `STARTED` line with no terminal result — see
     * [GroupedTestsResultProtocol.ParsedBatchResult.crashedInProgress]) from one that never ran at all (neither a
     * start nor a result, e.g. a stripped launcher) — which also prevents a silently-skipped test from
     * masquerading as passing.
     */
    private fun attributeStructuredResults(
        parsedBatchResult: GroupedTestsResultProtocol.ParsedBatchResult,
        exceptions: List<Throwable>,
        texts: List<String>,
    ) {
        val results = parsedBatchResult.outcomes
        val testReport = parsedBatchResult.toTestReport()
        var anyFailureAttributed = false
        val expectedIds = testServices.groupingStageInputs.map { input ->
            computeProxyLauncherClassName(input.testServices.testInfo)
        }
        val nonEmptyCheck = TestRunChecks.checkNonEmpty(testReport)
        val nonEmptyFailureReason = (nonEmptyCheck as? TestRunChecks.Result.Failed)?.reason
        val excessiveIds = TestRunChecks.findExcessiveResults(expectedIds, testReport)

        if (excessiveIds.isNotEmpty()) {
            testServices.groupingStageInputs.first().catchingExecutor.executeWithCatching({ WrappedException.FromGroupingHandler(it, this) }) {
                throw AssertionError(
                    """
                    Sanity check failed: grouped batch reported unexpected test ids: $excessiveIds.
                    Expected ids: $expectedIds
                    This indicates protocol/report desynchronization (results for tests that were not part of this batch).
                    Collected outputs:
                    """.trimIndent() + "\n" + texts.joinToString("\n")
                )
            }
            anyFailureAttributed = true
        }

        val missingIds = TestRunChecks.findMissingResults(expectedIds, testReport).toSet()
        for (input in testServices.groupingStageInputs) {
            // Every grouped test is driven via the launcher's `ProxyLauncher_*.runTest()` (asserting `box() == "OK"`),
            // so it must have a `box()`. Tests without one must be isolated — they are driven by a custom JS entry
            // point and cannot report a structured result line.
            checkTestInfrastructure(hasBoxMethod(input)) {
                "Test ${input.testInfo} does not have a box() method, so its execution status cannot be reported " +
                        "via the grouped result protocol. Please isolate this test using either existing ways in " +
                        "WasmGroupingTestIsolator or add a new rule there."
            }

            val id = computeProxyLauncherClassName(input.testServices.testInfo)
            val outcome = results[id]
            when {
                id in missingIds -> {
                    val missingDiagnosis = if (parsedBatchResult.crashedInProgress(id)) {
                        "The VM printed a '${GroupedTestsResultProtocol.STARTED}' line for '$id' but no terminal " +
                                "'${GroupedTestsResultProtocol.PASSED}'/'${GroupedTestsResultProtocol.FAILED}' result — " +
                                "this test most likely crashed the VM (a hard trap, OOM, or process exit) while executing."
                    } else {
                        "The test was expected to run as part of the batch, but produced no " +
                                "'${GroupedTestsResultProtocol.LINE_PREFIX}' line (not even a " +
                                "'${GroupedTestsResultProtocol.STARTED}' one). This typically indicates the test was silently " +
                                "skipped (e.g. a stripped ProxyLauncher class), or a VM crashed before this test's launcher was reached."
                    }
                    input.catchingExecutor.executeWithCatching({ WrappedException.FromGroupingHandler(it, this) }) {
                        throw AssertionError(
                            """
                            ${nonEmptyFailureReason?.let { "$it\n" } ?: ""}
                            Sanity check failed: no per-test result was reported for '$id' in the grouped batch.
                            $missingDiagnosis
                            Collected outputs:
                            """.trimIndent() + "\n" + texts.joinToString("\n")
                        )
                    }
                    anyFailureAttributed = true
                }
                id in testReport.failedTests && outcome != null -> {
                    input.catchingExecutor.executeWithCatching({ WrappedException.FromGroupingHandler(it, this) }) {
                        throw AssertionError(listOfNotNull(outcome.message, outcome.details).joinToString("\n"))
                    }
                    anyFailureAttributed = true
                }
            }
        }

        // A VM may have thrown (e.g. a hard trap) without that surfacing as a per-test failure above. If nothing
        // was attributed to a specific test, surface the raw VM exception so the batch does not pass silently.
        if (!anyFailureAttributed && exceptions.isNotEmpty()) {
            throw exceptions.first()
        }
    }

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
     * Tests without a `box()` (e.g. `// FILE: entry.mjs` driven Wasm/JS size tests) are
     * executed via a custom JS entry point — not via the synthetic `ProxyLauncher_<hash>`
     * launcher classes — so they cannot report a structured per-test result line.
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
