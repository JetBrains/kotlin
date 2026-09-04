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
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider.Companion.containsBoxMethod
import org.jetbrains.kotlin.test.services.sourceProviders.SourceContentView
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.wasm.test.blackbox.computeProxyLauncherClassName
import java.security.MessageDigest

private const val MAX_GROUPED_DIAGNOSTIC_LENGTH = 16 * 1024
private const val MAX_GROUPED_DIAGNOSTIC_LINE_LENGTH = 2 * 1024
private const val MAX_GROUPED_DIAGNOSTIC_LINE_COUNT = 32

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
        outputCollector: MutableList<WasmVMOutput>?,
    ): List<Throwable>

    override fun processArtifact(artifact: BinaryArtifacts.Wasm) {
        val inputs = testServices.groupingStageInputs
        // The artifact contract is authoritative: a result-collecting driver must be invoked even when a
        // driver-linked singleton happens to satisfy a subclass's historical box-export heuristic.
        val useBoxExportMode = !artifact.hasGroupedTestsDriver && shouldUseBoxExportMode()

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
                // Several VMs run the same test, and their failures usually differ; keep them all on the report.
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

        // A VM-failure message embeds the stdout captured before the crash, so a partial block is recovered too.
        // Keep these texts because they are both parser input and, when a crash needs explaining, diagnostic input.
        val exceptionTexts = exceptions.map(::collectExceptionTexts)
        val exceptionExecutionNames = exceptions.map(::executionNameOf)
        val namedOutputs = buildList {
            collectedOutputs.forEach { output ->
                add(
                    GroupedTestsResultProtocol.ExecutionOutput(
                        executionName = output.executionName,
                        output = output.output,
                    )
                )
            }
            exceptions.forEachIndexed { index, _ ->
                val executionName = exceptionExecutionNames[index] ?: return@forEachIndexed
                exceptionTexts[index].forEach { text ->
                    add(GroupedTestsResultProtocol.ExecutionOutput(executionName, text))
                }
            }
        }
        val unnamedExceptionTexts = exceptions.indices
            .filter { exceptionExecutionNames[it] == null }
            .flatMap { exceptionTexts[it] }
        val parsedBatchResult = GroupedTestsResultProtocol.parseMergedWithExecutionNames(
            outputs = namedOutputs,
            additionalOutputs = unnamedExceptionTexts,
        )
        val parsedCollectedOutputs = parsedBatchResult.executions.take(collectedOutputs.size)
        val parsedExceptionOutputs = associateParsedExceptionOutputs(
            parsedBatchResult = parsedBatchResult,
            collectedOutputCount = collectedOutputs.size,
            exceptions = exceptions,
            exceptionTexts = exceptionTexts,
            exceptionExecutionNames = exceptionExecutionNames,
        )
        if (parsedBatchResult.malformedLines.isNotEmpty()) {
            failBatchWithMalformedLines(
                parsedBatchResult,
                exceptions,
                collectedOutputs,
                parsedCollectedOutputs,
                exceptionTexts,
                parsedExceptionOutputs,
            )
            return
        }

        if (artifact.hasGroupedTestsDriver) {
            // Successful VM invocations retain their own output boundary. A VM that failed to start or crashed is
            // represented by an exception and remains covered by the crash/unexplained-exception handling below.
            val outputsWithoutStructuredBlock = collectedOutputs.zip(parsedCollectedOutputs)
                .filter { !it.second.sawStructuredBlock }
                .map { it.first }
            if (outputsWithoutStructuredBlock.isNotEmpty()) {
                val missingBlockVms = outputsWithoutStructuredBlock
                    .map { it.executionName }
                    .distinct()
                    .joinToString(", ")
                testServices.groupingStageInputs.forEach { input ->
                    input.failWithCollectedOutputs(
                        emptyList(),
                        "Sanity check failed: the grouped batch did not print a " +
                                "'${GroupedTestsResultProtocol.BEGIN}' block for every driver-enabled VM. " +
                                "Missing from: $missingBlockVms. A VM exited successfully without invoking the " +
                                "launcher's result-collecting driver; not a single test reported a result on that " +
                                "VM, so the results from the other VMs cannot establish complete test coverage.",
                    )
                }
                // Nothing was attributed, so no VM failure the batch collected is accounted for yet.
                failWithUnexplainedExceptions(exceptions, parsedExceptionOutputs, crashAttributedIds = emptySet())
                return
            }

            val outputsWithIncompleteStructuredBlock = collectedOutputs.zip(parsedCollectedOutputs)
                .filter { !it.second.hasCompleteStructuredBlock }
                .map { it.first }
            if (outputsWithIncompleteStructuredBlock.isNotEmpty()) {
                val incompleteBlockVms = outputsWithIncompleteStructuredBlock
                    .map { it.executionName }
                    .distinct()
                    .joinToString(", ")
                testServices.groupingStageInputs.forEach { input ->
                    input.failWithCollectedOutputs(
                        emptyList(),
                        "Sanity check failed: the grouped batch did not print a complete " +
                                "'${GroupedTestsResultProtocol.BEGIN}'/'${GroupedTestsResultProtocol.END}' block for " +
                                "every driver-enabled VM. Incomplete on: $incompleteBlockVms. A VM exited " +
                                "successfully before the launcher's result-collecting driver completed, so the " +
                                "results from the other VMs cannot establish complete test coverage.",
                    )
                }
                failWithUnexplainedExceptions(exceptions, parsedExceptionOutputs, crashAttributedIds = emptySet())
                return
            }
        }

        if (parsedBatchResult.sawStructuredBlock) {
            attributeStructuredResults(
                parsedBatchResult,
                exceptions,
                collectedOutputs,
                parsedCollectedOutputs,
                exceptionTexts,
                parsedExceptionOutputs,
                driverOutputs = if (artifact.hasGroupedTestsDriver) collectedOutputs else emptyList(),
                parsedDriverOutputs = if (artifact.hasGroupedTestsDriver) parsedCollectedOutputs else emptyList(),
            )
            return
        }

        // A driver-linked batch reports every verdict through the driver, so no block at all means it was never
        // invoked: `test.mjs` fell back to `startUnitTests()`, which finds nothing to run (the launcher classes carry
        // no `@kotlin.test.Test`) and exits cleanly — the batch would be green with no test having run.
        if (artifact.hasGroupedTestsDriver) {
            testServices.groupingStageInputs.forEach { input ->
                input.failWithCollectedOutputs(
                    emptyList(),
                    "Sanity check failed: the grouped batch printed no '${GroupedTestsResultProtocol.BEGIN}' block, " +
                            "so not a single test reported a result. The launcher's result-collecting driver was " +
                            "never invoked — most likely its exported entry point " +
                            "(`runGroupedTests` on wasm-js, `startTest` on wasm-wasi) was missing or renamed, which " +
                            "means no test of this batch actually ran.",
                )
            }
            failWithUnexplainedExceptions(exceptions, parsedExceptionOutputs, crashAttributedIds = emptySet())
            return
        }

        // A driverless batch is a single isolated test: nothing to demux, and any VM failure is that test's own.
        if (exceptions.isNotEmpty()) {
            testServices.groupingStageInputs.forEach { it.failWithAll(exceptions) }
        }
    }

    private fun failBatchWithMalformedLines(
        parsedBatchResult: GroupedTestsResultProtocol.ParsedBatchResult,
        exceptions: List<Throwable>,
        collectedOutputs: List<WasmVMOutput>,
        parsedCollectedOutputs: List<GroupedTestsResultProtocol.ParsedExecution>,
        exceptionTexts: List<List<String>>,
        parsedExceptionOutputs: List<List<GroupedTestsResultProtocol.ParsedExecution>>,
    ) {
        val expectedIds = testServices.groupingStageInputs.map { input ->
            computeProxyLauncherClassName(input.testServices.testInfo)
        }
        val analysis = parsedBatchResult.analyze(expectedIds)
        val malformedLines = formatMalformedLines(analysis.malformedLines)
        val batchReason =
            "Sanity check failed: malformed structured result protocol line(s) were emitted inside a " +
                    "'${GroupedTestsResultProtocol.BEGIN}'/'${GroupedTestsResultProtocol.END}' block:\n" +
                    "$malformedLines. The result block cannot be trusted; this indicates a problem in the " +
                    "grouped-test driver or in the test output."

        val crashAttributedIds = analysis.crashedIds
        for (input in testServices.groupingStageInputs) {
            val id = computeProxyLauncherClassName(input.testServices.testInfo)
            input.failWithCollectedOutputs(
                diagnosticTextsForTest(
                    id,
                    analysis.failures[id],
                    collectedOutputs,
                    parsedCollectedOutputs,
                    exceptionTexts,
                    parsedExceptionOutputs,
                ),
                batchReason,
                untrustedReportFor(analysis, id),
            )
        }

        failWithUnexplainedExceptions(exceptions, parsedExceptionOutputs, crashAttributedIds)
    }

    private fun untrustedReportFor(
        analysis: GroupedTestsResultProtocol.ParsedBatchResult.Analysis,
        id: String,
    ): String? {
        val (outcomes, crashEvidence, malformedLineCarriesId, malformedLineCarriesIdInCrashOutput) =
            analysis.testResults.getValue(id)
        val crashDiagnosis = crashEvidence?.let { evidence ->
            crashDiagnosis(id, evidence.executionNames, fallback = "a VM")
        }
        val malformedLineIsLikelyCause = malformedLineCarriesId &&
                (crashEvidence == null || malformedLineCarriesIdInCrashOutput)
        val text = when {
            malformedLineIsLikelyCause -> listOfNotNull(
                crashDiagnosis,
                "A malformed line carrying this test's id is part of the rejected block, so the output was cut off " +
                        "while this test was reporting: it is the likely cause of the rejection.",
            ).joinToString(" ")
            crashDiagnosis != null && crashEvidence?.isInMalformedOutput == true ->
                crashDiagnosis +
                        " The same VM also emitted a malformed line, but it did not carry this test's id, so it is " +
                        "not necessarily related."
            crashDiagnosis != null && malformedLineCarriesId -> crashDiagnosis +
                    " A malformed line carrying this test's id came from a different VM's output, so it is not " +
                    "necessarily related."
            crashDiagnosis != null -> crashDiagnosis +
                    " The malformed line did not identify this test, so it is not necessarily related."
            else -> outcomes
                .takeIf { it.isNotEmpty() }
                ?.let { outcomes ->
                    "Before the block was rejected, this test reported: ${outcomes.joinToString { it.status.name }}."
                }
        }

        return text
    }

     /** Attributes each per-test result to its grouping input, by the test's stable `ProxyLauncher_<encoded-package>` id. */
    private fun attributeStructuredResults(
        parsedBatchResult: GroupedTestsResultProtocol.ParsedBatchResult,
        exceptions: List<Throwable>,
        collectedOutputs: List<WasmVMOutput>,
        parsedCollectedOutputs: List<GroupedTestsResultProtocol.ParsedExecution>,
        exceptionTexts: List<List<String>>,
        parsedExceptionOutputs: List<List<GroupedTestsResultProtocol.ParsedExecution>>,
        driverOutputs: List<WasmVMOutput>,
        parsedDriverOutputs: List<GroupedTestsResultProtocol.ParsedExecution>,
    ) {
        val expectedIds = testServices.groupingStageInputs.map { input ->
            computeProxyLauncherClassName(input.testServices.testInfo)
        }
        val analysis = parsedBatchResult.analyze(
            expectedIds,
            executionOutputs = driverOutputs.zip(parsedDriverOutputs).map { entry ->
                GroupedTestsResultProtocol.ExecutionOutput(
                    executionName = entry.first.executionName,
                    output = entry.first.output,
                    parsed = entry.second,
                )
            },
        )

        // Checked before anything is attributed, since a test that cannot report a result invalidates the whole batch.
        testServices.groupingStageInputs.firstOrNull { !hasBoxMethod(it) }?.let { input ->
            testInfraError(
                "Test ${input.testInfo} does not have a box() method, so its execution status cannot be reported " +
                        "via the grouped result protocol. Please isolate this test using either existing ways in " +
                        "WasmGroupingTestIsolator or add a new rule there."
            )
        }

        // The driver is generated from this batch's own launcher names, so an unexpected id is nobody's test failure.
        checkTestInfrastructure(analysis.excessiveIds.isEmpty()) {
            "Grouped batch reported results for tests that are not part of it: ${analysis.excessiveIds}. Expected: $expectedIds"
        }

        // There is no batch-level failure sink, so an empty report is prepended to every missing test below.
        val emptyReportReason = (TestReportChecks.checkNonEmpty(analysis.testReport) as? TestReportChecks.Result.Failed)?.reason
        val crashAttributedIds = analysis.crashedIds

        for (input in testServices.groupingStageInputs) {
            val id = computeProxyLauncherClassName(input.testServices.testInfo)
            val failure = analysis.failures[id]
            val missingVmNames = analysis.missingExecutionNamesById[id].orEmpty()
            if (missingVmNames.isNotEmpty()) {
                // A result from *some* VM does not make the diagnosis below it redundant: a FAILED outcome can still
                // carry no message, and a globally MISSING one still deserves the "silently skipped" hint even when
                // it was inferred from a subset of VMs here rather than from every one of them in the `when` below.
                val reportedFailure = when (failure?.kind) {
                    FailureKind.FAILED -> failure.reportedFailure
                        ?: "Test '$id' reported a '${GroupedTestsResultProtocol.FAILED}' line carrying neither a " +
                                "message nor details."
                    else -> failure?.reportedFailure
                }
                val kindDiagnosis = when (failure?.kind) {
                    FailureKind.CRASHED -> crashDiagnosis(
                        id,
                        failure.crashExecutionNames,
                        fallback = if (failure.isCrashOnly) "the VM" else "another VM",
                    )
                    FailureKind.MISSING -> missingTestDiagnosis()
                    else -> null
                }
                input.failWithCollectedOutputs(
                    diagnosticTextsForTest(
                        id,
                        failure,
                        collectedOutputs,
                        parsedCollectedOutputs,
                        exceptionTexts,
                        parsedExceptionOutputs,
                    ),
                    emptyReportReason,
                    "Sanity check failed: test '$id' did not report a terminal result in every successful " +
                            "driver-enabled VM execution. Missing from complete result block(s) produced by: " +
                            "${missingVmNames.joinToString()}. Results from other executions cannot establish " +
                            "complete coverage for this test; no per-test result was reported for '$id' in " +
                            "the missing execution(s).",
                    reportedFailure,
                    kindDiagnosis,
                )
                continue
            }

            when (failure) {
                null -> Unit
                else -> when (failure.kind) {
                    FailureKind.MISSING -> {
                        input.failWithCollectedOutputs(
                            diagnosticTextsForTest(
                                id,
                                failure,
                                collectedOutputs,
                                parsedCollectedOutputs,
                                exceptionTexts,
                                parsedExceptionOutputs,
                            ),
                            emptyReportReason,
                            "Sanity check failed: no per-test result was reported for '$id' in the grouped batch.",
                            missingTestDiagnosis(),
                        )
                    }
                    FailureKind.CRASHED -> {
                        input.failWithCollectedOutputs(
                            diagnosticTextsForTest(
                                id,
                                failure,
                                collectedOutputs,
                                parsedCollectedOutputs,
                                exceptionTexts,
                                parsedExceptionOutputs,
                            ),
                            if (failure.isCrashOnly) {
                                "Sanity check failed: no per-test result was reported for '$id' in the grouped batch."
                            } else {
                                null
                            },
                            failure.reportedFailure,
                            crashDiagnosis(
                                id,
                                failure.crashExecutionNames,
                                fallback = if (failure.isCrashOnly) "the VM" else "another VM",
                            ),
                        )
                    }
                    FailureKind.FAILED -> input.failWithCollectedOutputs(
                        diagnosticTextsForTest(
                            id,
                            failure,
                            collectedOutputs,
                            parsedCollectedOutputs,
                            exceptionTexts,
                            parsedExceptionOutputs,
                        ),
                        failure.reportedFailure
                            ?: "Test '$id' reported a '${GroupedTestsResultProtocol.FAILED}' line carrying neither " +
                            "a message nor details.",
                    )
                }
            }
        }

        failWithUnexplainedExceptions(exceptions, parsedExceptionOutputs, crashAttributedIds)
    }

    private fun failWithUnexplainedExceptions(
        exceptions: List<Throwable>,
        parsedExceptionOutputs: List<List<GroupedTestsResultProtocol.ParsedExecution>>,
        crashAttributedIds: Set<String>,
    ) {
        val unexplainedExceptions = exceptions.filterIndexed { index, _ ->
            val crashedThere = parsedExceptionOutputs[index].flatMapTo(mutableSetOf()) { it.crashedIds }
            crashedThere.none { it in crashAttributedIds }
        }
        if (unexplainedExceptions.isNotEmpty()) {
            testServices.assertions.failAll(unexplainedExceptions)
        }
    }

    /**
     * Maps the parser's execution list back to exceptions without reparsing their messages. Named exception texts are
     * placed before unnamed ones by [handleRunResult], matching the two input groups passed to the protocol parser.
     */
    private fun associateParsedExceptionOutputs(
        parsedBatchResult: GroupedTestsResultProtocol.ParsedBatchResult,
        collectedOutputCount: Int,
        exceptions: List<Throwable>,
        exceptionTexts: List<List<String>>,
        exceptionExecutionNames: List<String?>,
    ): List<List<GroupedTestsResultProtocol.ParsedExecution>> {
        val parsedExceptionOutputs = Array(exceptions.size) { emptyList<GroupedTestsResultProtocol.ParsedExecution>() }
        var parsedIndex = collectedOutputCount

        for (hasExecutionName in listOf(true, false)) {
            exceptions.forEachIndexed { index, _ ->
                if ((exceptionExecutionNames[index] != null) != hasExecutionName) return@forEachIndexed
                val count = exceptionTexts[index].size
                checkTestInfrastructure(parsedIndex + count <= parsedBatchResult.executions.size) {
                    "Grouped result parser returned fewer executions than the runner supplied"
                }
                parsedExceptionOutputs[index] = parsedBatchResult.executions.subList(parsedIndex, parsedIndex + count)
                parsedIndex += count
            }
        }

        checkTestInfrastructure(parsedIndex == parsedBatchResult.executions.size) {
            "Grouped result parser returned executions that were not associated with a VM output"
        }
        return parsedExceptionOutputs.toList()
    }

    /**
     * Returns raw VM text only when the structured result cannot already explain a test failure. In particular, a
     * message-bearing failure has its message and details in [Failure.reportedFailure], while a crash needs the
     * captured output to explain what happened. Keeping this selection per test avoids copying every VM's full batch
     * output into every attributed failure.
     */
    private fun diagnosticTextsForTest(
        id: String,
        failure: GroupedTestsResultProtocol.ParsedBatchResult.Analysis.Failure?,
        collectedOutputs: List<WasmVMOutput>,
        parsedCollectedOutputs: List<GroupedTestsResultProtocol.ParsedExecution>,
        exceptionTexts: List<List<String>>,
        parsedExceptionOutputs: List<List<GroupedTestsResultProtocol.ParsedExecution>>,
    ): List<String> {
        val includeFailedOutput = failure?.kind == FailureKind.FAILED && failure.reportedFailure == null
        val includeCrashOutput = failure?.kind == FailureKind.CRASHED
        if (!includeFailedOutput && !includeCrashOutput) return emptyList()

        fun GroupedTestsResultProtocol.ParsedExecution.isRelevant(): Boolean {
            return (includeFailedOutput && outcomes.any { it.id == id && it.status == GroupedTestsResultProtocol.Outcome.Status.FAILED }) ||
                    (includeCrashOutput && id in crashedIds)
        }

        return buildList {
            collectedOutputs.forEachIndexed { index, output ->
                if (parsedCollectedOutputs.getOrNull(index)?.isRelevant() == true) add(output.output)
            }
            exceptionTexts.forEachIndexed { index, texts ->
                if (parsedExceptionOutputs[index].any { it.isRelevant() }) addAll(texts)
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

    private fun executionNameOf(throwable: Throwable): String? {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is WasmVMException) return current.executionName
            current = current.cause
        }
        return null
    }

    /** Fails this specific test with [lines] (`null` ones are dropped) and optional diagnostic VM output. */
    private fun NonGroupingStageOutput.failWithCollectedOutputs(texts: List<String>, vararg lines: String?) {
        val diagnosticLines = buildList {
            lines.forEach { line -> line?.let(::add) }
            if (texts.isNotEmpty()) {
                add("Collected outputs:")
                addAll(texts)
            }
        }
        failWith(AssertionError(buildBoundedDiagnostic(diagnosticLines)))
    }

    /** Routes [error] into this test's own failure sink, so the test engine attributes it to this test. */
    private fun NonGroupingStageOutput.failWith(error: Throwable) {
        executeWithFailureCatching {
            throw error
        }
    }

    /**
     * Fails this specific test with every exception in [exceptions]: as itself if there is only one, or as a
     * `MultipleFailuresError` otherwise — the same reporting [failWithUnexplainedExceptions] uses at batch level, so
     * several VMs failing the same test for different reasons are all visible rather than one hiding the rest.
     */
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

    private fun formatMalformedLines(lines: List<String>): String = buildString {
        var displayedLineCount = 0
        var omittedLineCount = 0
        var canAppend = true

        for (line in lines) {
            if (!canAppend || displayedLineCount >= MAX_GROUPED_DIAGNOSTIC_LINE_COUNT) {
                omittedLineCount++
                continue
            }

            val entry = "  <${line.toBoundedDiagnostic(MAX_GROUPED_DIAGNOSTIC_LINE_LENGTH)}>"
            val separatorLength = if (isEmpty()) 0 else 1
            if (length + separatorLength + entry.length > MAX_GROUPED_DIAGNOSTIC_LENGTH) {
                omittedLineCount++
                canAppend = false
                continue
            }

            if (separatorLength != 0) append('\n')
            append(entry)
            displayedLineCount++
        }

        if (omittedLineCount != 0) {
            val omission = "... $omittedLineCount more malformed line(s) omitted ..."
            val separatorLength = if (isEmpty()) 0 else 1
            val contentLimit = (MAX_GROUPED_DIAGNOSTIC_LENGTH - separatorLength - omission.length).coerceAtLeast(0)
            if (length > contentLimit) delete(contentLimit, length)
            if (separatorLength != 0 && isNotEmpty()) append('\n')
            append(omission)
        }
    }

    /** Builds a bounded message while retaining an explicit summary for every discarded text line. */
    private fun buildBoundedDiagnostic(lines: Iterable<String>): String = buildString {
        var displayedLineCount = 0
        var omittedLineCount = 0
        var canAppend = true

        for (line in lines) {
            if (!canAppend || displayedLineCount >= MAX_GROUPED_DIAGNOSTIC_LINE_COUNT) {
                omittedLineCount++
                continue
            }

            val boundedLine = line.toBoundedDiagnostic(MAX_GROUPED_DIAGNOSTIC_LENGTH)
            val separatorLength = if (isEmpty()) 0 else 1
            if (length + separatorLength + boundedLine.length > MAX_GROUPED_DIAGNOSTIC_LENGTH) {
                omittedLineCount++
                canAppend = false
                continue
            }

            if (separatorLength != 0) append('\n')
            append(boundedLine)
            displayedLineCount++
        }

        if (omittedLineCount != 0) {
            val omission = "... $omittedLineCount more diagnostic line(s) omitted ..."
            val separatorLength = if (isEmpty()) 0 else 1
            val contentLimit = (MAX_GROUPED_DIAGNOSTIC_LENGTH - separatorLength - omission.length).coerceAtLeast(0)
            if (length > contentLimit) delete(contentLimit, length)
            if (separatorLength != 0 && isNotEmpty()) append('\n')
            append(omission)
        }
    }

    /**
     * Returns whether unit-test mode may intentionally run one input without the grouped result-collecting driver.
     *
     * A `RUN_UNIT_TESTS` test is isolated and uses the ordinary unit-test runner, while a test without `box()` uses a
     * custom entry point. Every other unit-test-mode singleton must carry the grouped driver, even when its batch has
     * only one input: a non-isolated singleton still reaches `box()` through the generated driver.
     */
    protected open fun allowsDriverlessSingleTest(): Boolean {
        val input = testServices.groupingStageInputs.singleOrNull() ?: return false
        return RUN_UNIT_TESTS in input.testServices.moduleStructure.allDirectives || !hasBoxMethod(input)
    }

    /**
     * A test without a `box()` (e.g. a `// FILE: entry.mjs` driven size test) runs through a custom JS entry point
     * rather than through its `ProxyLauncher_<encoded-package>`, so it cannot report a per-test result line.
     */
    protected fun hasBoxMethod(input: NonGroupingStageOutput): Boolean = containsBoxMethod(
        input.testServices.moduleStructure,
        SourceContentView.TRANSFORMED,
        input.testServices.sourceFileProvider,
    )
}

private fun String.toBoundedDiagnostic(maxLength: Int): String {
    if (length <= maxLength) return this

    val suffix = "... [truncated; original length=$length chars; SHA-256=${sha256Hex()}]"
    val prefixLength = (maxLength - suffix.length).coerceAtLeast(0)
    return take(prefixLength) + suffix.take(maxLength - prefixLength)
}

private fun String.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    var offset = 0
    while (offset < length) {
        var end = minOf(offset + 4096, length)
        if (end < length && Character.isHighSurrogate(this[end - 1])) end--
        if (end == offset) end++
        digest.update(substring(offset, end).toByteArray(Charsets.UTF_8))
        offset = end
    }

    val hexDigits = "0123456789abcdef"
    return buildString(64) {
        for (byte in digest.digest()) {
            val value = byte.toInt() and 0xFF
            append(hexDigits[value ushr 4])
            append(hexDigits[value and 0x0F])
        }
    }
}
