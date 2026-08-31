/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.ParsedBatchResult.Analysis.FailureKind
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.GroupingStageHandler
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.report.TestReportChecks
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
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
            // Box export mode: call box() directly and expect "OK". One `box()` call reports one verdict, so a batch
            // of several tests would attribute the first one and pass all the others unchecked.
            checkTestInfrastructure(inputs.size == 1) {
                "Box-export mode ran a batch of ${inputs.size} tests, but calling `box()` directly reports a single " +
                        "verdict: only the first test could be attributed, and the rest would pass unchecked."
            }
            val input = inputs.first()
            val exceptions = runTestCode(
                artifact,
                useUnitTestRunnerOnly = false,
                outputCollector = null,
            )
            if (exceptions.isNotEmpty()) {
                input.failWithAll(exceptions)
            }
        } else {
            // A unit-test-mode artifact must either carry the grouped driver or be an explicitly supported standalone
            // single-test execution. Validate this contract before starting any VM: otherwise a missing metadata bit
            // can turn a singleton grouped batch into a clean run with no structured result at all.
            checkTestInfrastructure(
                artifact.hasGroupedTestsDriver || inputs.size == 1 && allowsDriverlessSingleTest()
            ) {
                "A unit-test batch of ${inputs.size} tests is not linked with the result-collecting driver. " +
                        "Only an explicitly standalone single-test execution may run without it; either the stage-2 " +
                        "facade lost the driver metadata on `BinaryArtifacts.Wasm.hasGroupedTestsDriver`, or tests " +
                        "meant to be isolated were batched."
            }

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
        val run = BatchRun(collectedOutputs, exceptions)
        if (run.parsed.malformedLines.isNotEmpty()) {
            failWholeBatch(
                run,
                GroupedTestVerdict.UNTRUSTED_BATCH,
                "Sanity check failed: malformed structured result protocol line(s) were emitted inside a " +
                        "'${GroupedTestsResultProtocol.BEGIN}'/'${GroupedTestsResultProtocol.END}' block:\n" +
                        run.parsed.malformedLines.joinToString("\n") { "  <$it>" } +
                        "\nThe result block cannot be trusted; this indicates a problem in the grouped-test driver " +
                        "or in the test output.",
            )
            return
        }

        if (artifact.hasGroupedTestsDriver) {
            val vmsWithoutBlock = run.collectedExecutionNames { !it.sawStructuredBlock }
            if (vmsWithoutBlock.isNotEmpty()) {
                failWholeBatch(
                    run,
                    GroupedTestVerdict.NO_RESULT_BLOCK,
                    "Sanity check failed: the grouped batch did not print a " +
                            "'${GroupedTestsResultProtocol.BEGIN}' block for every driver-enabled VM. " +
                            "Missing from: ${vmsWithoutBlock.joinToString()}. A VM exited successfully without invoking the " +
                            "launcher's result-collecting driver; not a single test reported a result on that " +
                            "VM, so the results from the other VMs cannot establish complete test coverage.",
                )
                return
            }

            val vmsWithIncompleteBlock = run.collectedExecutionNames { !it.hasCompleteStructuredBlock }
            if (vmsWithIncompleteBlock.isNotEmpty()) {
                failWholeBatch(
                    run,
                    GroupedTestVerdict.INCOMPLETE_RESULT_BLOCK,
                    "Sanity check failed: the grouped batch did not print a complete " +
                            "'${GroupedTestsResultProtocol.BEGIN}'/'${GroupedTestsResultProtocol.END}' block for " +
                            "every driver-enabled VM. Incomplete on: ${vmsWithIncompleteBlock.joinToString()}. A VM exited " +
                            "successfully before the launcher's result-collecting driver completed, so the " +
                            "results from the other VMs cannot establish complete test coverage.",
                )
                return
            }
        }

        if (run.parsed.sawStructuredBlock) {
            attributeStructuredResults(run, isDriverLinked = artifact.hasGroupedTestsDriver)
            return
        }

        // A driver-linked batch reports every verdict through the driver, so no block at all means it was never
        // invoked: `test.mjs` fell back to `startUnitTests()`, which finds nothing to run (the launcher classes carry
        // no `@kotlin.test.Test`) and exits cleanly — the batch would be green with no test having run.
        if (artifact.hasGroupedTestsDriver) {
            failWholeBatch(
                run,
                GroupedTestVerdict.NO_RESULT_BLOCK,
                "Sanity check failed: the grouped batch printed no '${GroupedTestsResultProtocol.BEGIN}' block, " +
                        "so not a single test reported a result. The launcher's result-collecting driver was " +
                        "never invoked — most likely its exported entry point " +
                        "(`runGroupedTests` on wasm-js, `startTest` on wasm-wasi) was missing or renamed, which " +
                        "means no test of this batch actually ran.",
            )
            return
        }

        if (exceptions.isNotEmpty()) {
            testServices.groupingStageInputs.forEach { it.failWithAll(exceptions) }
        }
    }

    /**
     * Fails every test of a batch whose results cannot establish coverage at all. No VM failure is attributed to a
     * test here, so every one of them is reported as it is.
     */
    private fun failWholeBatch(run: BatchRun, verdict: GroupedTestVerdict, reason: String) {
        testServices.groupingStageInputs.forEach { input ->
            input.failWithVerdict(verdict, emptyList(), reason)
        }
        failWithUnexplainedExceptions(run, crashAttributedIds = emptySet())
    }

    private fun attributeStructuredResults(run: BatchRun, isDriverLinked: Boolean) {
        val expectedIds = expectedIds()
        val analysis = run.parsed.analyze(
            expectedIds,
            // A driver-linked VM that completed its block must report every test; a driverless batch has no driver
            // to report through, so no execution is held to full coverage there.
            executionsRequiringFullCoverage = if (isDriverLinked) run.parsedCollectedOutputs else emptyList(),
        )

        testServices.groupingStageInputs.firstOrNull { !it.hasBoxMethod() }?.let { input ->
            testInfraError(
                "Test ${input.testInfo} does not have a box() method, so its execution status cannot be reported " +
                        "via the grouped result protocol. Please isolate this test using either existing ways in " +
                        "WasmGroupingTestIsolator or add a new rule there."
            )
        }

        checkTestInfrastructure(analysis.excessiveIds.isEmpty()) {
            "Grouped batch reported results for tests that are not part of it: ${analysis.excessiveIds}. Expected: $expectedIds"
        }

        val emptyReportReason = TestReportChecks.emptyReportReason(analysis.testReport)
        for (input in testServices.groupingStageInputs) {
            val id = computeProxyLauncherClassName(input.testServices.testInfo)
            input.reportResult(id, analysis, emptyReportReason, run)
        }

        failWithUnexplainedExceptions(run, run.parsed.crashedIds)
    }

    /**
     * Fails [id]'s input with the verdict its results support, and with every reason that led to it. A test that passed
     * on every execution that completed its result block is left alone.
     */
    private fun NonGroupingStageOutput.reportResult(
        id: String,
        analysis: GroupedTestsResultProtocol.ParsedBatchResult.Analysis,
        emptyReportReason: String?,
        run: BatchRun,
    ) {
        val failure = analysis.failures[id]
        val kind = failure?.kind
        val missingVmNames = analysis.missingExecutionNamesById[id].orEmpty()
        if (kind == null && missingVmNames.isEmpty()) return

        val verdict = when {
            kind == FailureKind.MISSING -> GroupedTestVerdict.MISSING
            missingVmNames.isNotEmpty() -> GroupedTestVerdict.INCOMPLETE_COVERAGE
            kind == FailureKind.FAILED -> GroupedTestVerdict.FAILED
            else -> GroupedTestVerdict.CRASHED
        }
        val coverageReason = when {
            missingVmNames.isNotEmpty() ->
                "Sanity check failed: test '$id' did not report a terminal result in every successful " +
                        "driver-enabled VM execution. Missing from complete result block(s) produced by: " +
                        "${missingVmNames.joinToString()}. Results from other executions cannot establish " +
                        "complete coverage for this test; no per-test result was reported for '$id' in " +
                        "the missing execution(s)."
            kind == FailureKind.MISSING || failure?.isCrashOnly == true ->
                "Sanity check failed: no per-test result was reported for '$id' in the grouped batch."
            else -> null
        }
        val reportedFailure = when (kind) {
            FailureKind.FAILED -> failure.reportedFailure
                ?: ("Test '$id' reported a '${GroupedTestsResultProtocol.FAILED}' line carrying neither a " +
                        "message nor details.")
            FailureKind.CRASHED -> failure.reportedFailure
            else -> null
        }
        val diagnosis = when (kind) {
            FailureKind.CRASHED -> crashDiagnosis(
                id,
                failure.crashExecutionNames,
                fallback = if (failure.isCrashOnly) "the VM" else "another VM",
            )
            FailureKind.MISSING -> missingTestDiagnosis()
            else -> null
        }

        failWithVerdict(
            verdict,
            diagnosticTextsForTest(id, failure, run),
            emptyReportReason.takeIf { missingVmNames.isNotEmpty() || kind == FailureKind.MISSING },
            coverageReason,
            reportedFailure,
            diagnosis,
        )
    }

    private fun expectedIds(): List<String> = testServices.groupingStageInputs.map { input ->
        computeProxyLauncherClassName(input.testServices.testInfo)
    }

    /** Reports every VM failure that no test's crash accounts for: a failure must never be hidden behind a verdict. */
    private fun failWithUnexplainedExceptions(run: BatchRun, crashAttributedIds: Set<String>) {
        val unexplainedExceptions = run.exceptions.filterIndexed { index, _ ->
            run.parsedExceptionOutputs[index]?.crashedIds.orEmpty().none { it in crashAttributedIds }
        }
        if (unexplainedExceptions.isNotEmpty()) {
            testServices.assertions.failAll(unexplainedExceptions)
        }
    }

    /**
     * Returns raw VM output only when the structured result cannot already explain a test failure. In particular, a
     * message-bearing failure has its message and details in
     * [GroupedTestsResultProtocol.ParsedBatchResult.Analysis.Failure.reportedFailure], while a crash needs the
     * captured output to explain what happened. Keeping this selection per test avoids copying every VM's full batch
     * output into every attributed failure.
     */
    private fun diagnosticTextsForTest(
        id: String,
        failure: GroupedTestsResultProtocol.ParsedBatchResult.Analysis.Failure?,
        run: BatchRun,
    ): List<String> {
        val includeFailedOutput = failure?.kind == FailureKind.FAILED && failure.reportedFailure == null
        val includeCrashOutput = failure?.kind == FailureKind.CRASHED
        if (!includeFailedOutput && !includeCrashOutput) return emptyList()

        fun GroupedTestsResultProtocol.ParsedExecution.isRelevant(): Boolean {
            return (includeFailedOutput && outcomes.any { it.id == id && it.status == GroupedTestsResultProtocol.Outcome.Status.FAILED }) ||
                    (includeCrashOutput && id in crashedIds)
        }

        return buildList {
            run.collectedOutputs.forEachIndexed { index, output ->
                if (run.parsedCollectedOutputs[index].isRelevant()) add(output.output)
            }
            run.exceptionOutputs.forEachIndexed { index, output ->
                if (output != null && run.parsedExceptionOutputs[index]?.isRelevant() == true) add(output.output)
            }
        }.distinct()
    }

    private fun crashDiagnosis(id: String, executionNames: List<String>, fallback: String): String {
        val executionDetails = executionNames.takeIf { it.isNotEmpty() }
            ?.joinToString()
            ?.let { " Execution: $it." }
            .orEmpty()
        return "Test '$id' printed a '${GroupedTestsResultProtocol.STARTED}' line on $fallback with no terminal " +
                "'${GroupedTestsResultProtocol.PASSED}'/'${GroupedTestsResultProtocol.FAILED}' result — it most " +
                "likely crashed that VM (a hard trap, OOM, or process exit) while executing.$executionDetails"
    }

    private fun missingTestDiagnosis(): String =
        "The test was expected to run as part of the batch, but produced no " +
                "'${GroupedTestsResultProtocol.LINE_PREFIX}' line, not even a " +
                "'${GroupedTestsResultProtocol.STARTED}' one. This typically indicates the test was silently " +
                "skipped (e.g. a stripped ProxyLauncher class), or that a VM crashed before this test's launcher " +
                "was reached."

    private fun NonGroupingStageOutput.failWithVerdict(
        verdict: GroupedTestVerdict,
        texts: List<String>,
        vararg lines: String?,
    ) {
        val diagnosticLines = buildList {
            lines.forEach { line -> line?.let(::add) }
            if (texts.isNotEmpty()) {
                add("Collected outputs:")
                addAll(texts)
            }
        }
        failWith(GroupedTestFailure(verdict, diagnosticLines.joinToString("\n")))
    }

    private fun NonGroupingStageOutput.failWith(error: Throwable) {
        executeWithFailureCatching {
            throw error
        }
    }

    private fun NonGroupingStageOutput.failWithAll(exceptions: List<Throwable>) {
        executeWithFailureCatching {
            this@AbstractWasmGroupingStageBoxRunner.testServices.assertions.failAll(exceptions)
        }
    }

    private fun NonGroupingStageOutput.executeWithFailureCatching(block: () -> Unit) {
        catchingExecutor.executeWithCatching(
            { WrappedException.FromGroupingHandler(it, this@AbstractWasmGroupingStageBoxRunner) },
            block,
        )
    }

    protected open fun allowsDriverlessSingleTest(): Boolean {
        val input = testServices.groupingStageInputs.singleOrNull() ?: return false
        return RUN_UNIT_TESTS in input.testServices.moduleStructure.allDirectives || !input.hasBoxMethod()
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

    /** The test reported a result on some executions, but not on every other one that completed its result block. */
    INCOMPLETE_COVERAGE,

    /** The batch printed malformed result lines, so none of its results can be trusted. */
    UNTRUSTED_BATCH,

    /** A driver-linked VM printed no result block at all. */
    NO_RESULT_BLOCK,

    /** A driver-linked VM exited cleanly with its result block left open. */
    INCOMPLETE_RESULT_BLOCK,
}

/** A grouped test's failure. Its message explains [verdict] and keeps the evidence behind it. */
internal class GroupedTestFailure(val verdict: GroupedTestVerdict, message: String) : AssertionError(message)

/**
 * What the VMs of one grouped batch produced: the output of every VM that completed, and every VM failure together
 * with the output it captured before failing, parsed into one [GroupedTestsResultProtocol.ParsedBatchResult].
 */
private class BatchRun(val collectedOutputs: List<WasmVMOutput>, val exceptions: List<Throwable>) {
    /** The output each failure captured, index for index: the results printed before a crash are still evidence. */
    val exceptionOutputs: List<WasmVMOutput?> = exceptions.map { it.capturedVmOutput() }

    val parsed: GroupedTestsResultProtocol.ParsedBatchResult = GroupedTestsResultProtocol.parseMergedWithExecutionNames(
        (collectedOutputs + exceptionOutputs.filterNotNull()).map { (output, executionName) ->
            GroupedTestsResultProtocol.ExecutionOutput(executionName = executionName, output = output)
        }
    )

    val parsedCollectedOutputs: List<GroupedTestsResultProtocol.ParsedExecution> =
        parsed.executions.take(collectedOutputs.size)

    val parsedExceptionOutputs: List<GroupedTestsResultProtocol.ParsedExecution?> = run {
        val remaining = parsed.executions.drop(collectedOutputs.size).iterator()
        exceptionOutputs.map { output -> output?.let { remaining.next() } }
    }

    fun collectedExecutionNames(predicate: (GroupedTestsResultProtocol.ParsedExecution) -> Boolean): List<String> =
        collectedOutputs.filterIndexed { index, _ -> predicate(parsedCollectedOutputs[index]) }
            .map { it.executionName }
            .distinct()
}

private fun Throwable.capturedVmOutput(): WasmVMOutput? {
    val vmException = generateSequence(this) { it.cause }.filterIsInstance<WasmVMException>().firstOrNull() ?: return null
    val output = vmException.output ?: return null
    return WasmVMOutput(vmName = vmException.vmName, output = output, executionName = vmException.executionName)
}
