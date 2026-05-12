/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.GroupingStageHandler
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.services.BatchingPackageInserter
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.wasm.test.providers.WasmJsLauncherAdditionalSourceProvider

/**
 * Shared base class for grouping stage handlers in WASM test infrastructure.
 *
 * Encapsulates code common to JS and WASI folder-based grouped runs:
 *   - dispatching test execution to VMs and collecting their outputs/exceptions;
 *   - on the failure path, parsing TeamCity `##teamcity[testFailed` lines from VM stdout
 *     and re-attributing failures to per-test grouping inputs via their
 *     [NonGroupingStageOutput.catchingExecutor];
 *   - on the success path, sanity-checking that every batched test produced its
 *     `##teamcity[testSuiteFinished` line via [verifyAllExpectedSuitesFinished].
 */
abstract class AbstractWasmBoxRunnerGroupingStage(
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

    protected fun handleRunResult(runResult: RunResult) {
        val (collectedOutputs, exceptions) = runResult

        if (exceptions.isEmpty()) {
            // Sanity check on the success path: every batched test must have its corresponding
            // `##teamcity[testSuiteFinished name='<...>'` line in at least one VM's stdout. A
            // missing line indicates that the per-test launcher class was optimized away, never
            // linked, or otherwise not picked up by the test runner — which would silently mask
            // a real test execution failure as "all tests passed".
            verifyAllExpectedSuitesFinished(collectedOutputs)
            return
        }

        val failuresBySuiteName = mutableMapOf<String, WasmTestFailure>()
        for (throwable in exceptions) {
            val message = throwable.message ?: continue
            val output = if (message.contains("OUTPUT:\n")) {
                message.substringAfter("OUTPUT:\n").substringBefore("\n---")
            } else if (message.contains("Output:\n")) {
                message.substringAfter("Output:\n")
            } else {
                continue
            }
            failuresBySuiteName.putAll(parseTeamCityFailures(output))
        }

        if (failuresBySuiteName.isEmpty()) {
            throw exceptions.first()
        }

        for (input in testServices.groupingStageInputs) {
            val expectedSuiteNames = computeExpectedSuiteNames(input)
            val failure = expectedSuiteNames.firstNotNullOfOrNull { failuresBySuiteName[it] }
            if (failure != null) {
                input.catchingExecutor.executeWithCatching({ WrappedException.FromGroupingHandler(it, this) }) {
                    throw AssertionError(failure.message + "\n" + failure.details)
                }
            }
        }
    }

    /**
     * Verifies that every test in the grouped batch produced a `##teamcity[testSuiteFinished
     * name='<expected>'` line in at least one VM's captured stdout. If a test's suite line is
     * missing across all collected outputs, the corresponding grouping input is failed via its
     * [NonGroupingStageOutput.catchingExecutor] so that JUnit attributes the failure to the
     * specific test rather than to the whole batch.
     *
     * The collector receives an entry per successful VM invocation; tests that fail in a VM
     * (returning a non-null [Throwable]) do not contribute output here — the failure-path code
     * above continues to handle them.
     */
    private fun verifyAllExpectedSuitesFinished(collectedOutputs: List<String>) {
        // For each VM output, collect suite names that were finished.
        val finishedSuitesPerOutput: List<Set<String>> = collectedOutputs.map { output ->
            val finished = mutableSetOf<String>()
            for (rawLine in output.lines()) {
                val line = rawLine.trim()
                if (!line.startsWith("##teamcity[testSuiteFinished")) continue
                val nameStart = line.indexOf("name='")
                if (nameStart < 0) continue
                val nameEnd = line.indexOf("'", nameStart + "name='".length)
                if (nameEnd < 0) continue
                finished += line.substring(nameStart + "name='".length, nameEnd)
            }
            finished
        }
        // A suite is considered finished if at least one VM reported it.
        val unionFinished: Set<String> = finishedSuitesPerOutput.fold(emptySet()) { acc, s -> acc + s }

        for (input in testServices.groupingStageInputs) {
            // Skip tests that have no `box()` function in any of their modules. Such tests are
            // driven by a custom JS entry point (e.g. `entry.mjs`) rather than by the unit-test
            // runner, so they do not produce `##teamcity[testSuiteFinished` lines. Their pass /
            // fail status is determined entirely by whether the VM throws when executing the
            // custom entry script (handled separately by the failure path above).
            if (!hasBoxMethod(input)) continue

            // Skip single-test batches. A batch that contains a single test is not executed via the
            // JUnit/unit-test runner (no batched `ProxyLauncher`/`Launcher` suite is driven for it);
            // instead it is run by directly invoking its `box()` function and asserting `"OK"`, exactly
            // like the standalone `FirWasmJsCodegenBoxTestGenerated` / `WasmBoxRunner` — regardless of
            // why it ended up alone in the batch (isolated, or merely a unique batch token). Such a run
            // does not emit `##teamcity[testSuiteFinished` markers, so the suite-finished sanity check
            // does not apply — its pass/fail status is determined solely by the `box()` result (handled
            // on the failure path above).
            if (testServices.groupingStageInputs.map { it.testServices.testInfo }.distinct().size == 1) continue

            val expectedSuiteNames = computeExpectedSuiteNames(input)
            if (expectedSuiteNames.any { it in unionFinished }) continue

            input.catchingExecutor.executeWithCatching({ WrappedException.FromGroupingHandler(it, this) }) {
                throw AssertionError(
                    "Sanity check failed: none of the expected '##teamcity[testSuiteFinished " +
                            "name=<...>' lines were found in the VM output of the grouped batch. " +
                            "Expected one of: $expectedSuiteNames. The test was " +
                            "expected to run as part of the batch, but its TeamCity suite was " +
                            "not finished. This typically indicates the test was silently " +
                            "skipped by the unit test runner (e.g. due to a missing @Test " +
                            "annotation, a stripped ProxyLauncher class, or a runtime error " +
                            "before this test's class was reached). Collected outputs:\n" + collectedOutputs
                )
            }
        }
    }

    /**
     * Returns `true` if any module of [input] contains a file with a top-level `box()` function.
     *
     * Tests without a `box()` (e.g. `// FILE: entry.mjs` driven Wasm/JS size tests) are
     * executed via a custom JS entry point — not via the synthetic `ProxyLauncher_<hash>` /
     * `Launcher_<hash>` unit-test classes — so they cannot be sanity-checked against
     * `##teamcity[testSuiteFinished` markers.
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

    /**
     * For a given grouping input, returns all suite names that could legitimately indicate that
     * its test was actually executed.
     *
     * Two flows produce different suite-name shapes:
     *  - The non-isolated (grouped) path uses `ProxyLauncher_<hashCode(additionalPackage)>`
     *    (see `WasmCompilerSecondStageFacade.Grouping.transform()`).
     *  - The friend-dependency isolated path keeps the per-test KLIB as the `-Xinclude` main
     *    module (so that `-Xfriend-modules` correctly preserves the friend relation across
     *    sibling KLIBs). In that path the `@Test`-annotated launcher class baked into the
     *    per-test KLIB by `WasmJsLauncherAdditionalSourceProvider` (named
     *    `Launcher_<hashCode(relativePath)>`) is what `GenerateWasmTests` registers — the
     *    synthetic `ProxyBatchLauncher.kt` is silently dropped because the linking pipeline
     *    skips frontend/Fir2Ir when `-Xinclude` is set.
     */
    private fun computeExpectedSuiteNames(input: NonGroupingStageOutput): List<String> {
        val result = mutableListOf<String>()
        result += BatchingPackageInserter.computeProxyLauncherClassName(input.testServices.testInfo)

        val moduleStructure = input.testServices.moduleStructure
        for (module in moduleStructure.modules) {
            for (file in module.files) {
                if (MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(file.originalContent)) {
                    result += WasmJsLauncherAdditionalSourceProvider.computeLauncherClassName(file)
                }
            }
        }
        return result
    }
}
