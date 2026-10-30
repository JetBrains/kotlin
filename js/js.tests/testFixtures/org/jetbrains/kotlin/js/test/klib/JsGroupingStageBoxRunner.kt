/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.js.engine.ScriptExecutionException
import org.jetbrains.kotlin.js.test.converters.augmentWithModuleName
import org.jetbrains.kotlin.js.test.handlers.JsBoxRunner
import org.jetbrains.kotlin.js.test.utils.getModeOutputFilePath
import org.jetbrains.kotlin.js.testOld.V8JsTestChecker
import org.jetbrains.kotlin.js.testOld.runTestFunction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol.ParsedBatchResult.Analysis.FailureKind
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.GroupingStageHandler
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.DFS.topologicalOrder

/**
 * Runs the executable the grouping stage has linked, on V8.
 *
 * The executable of an isolated test is processed by the handlers of the one-stage pipeline, exactly as it is there.
 * The executable of a grouped batch is run once per translation mode by calling its result-collecting driver; what
 * the driver prints is parsed by [GroupedTestsResultProtocol], and every failure is reported against the test it
 * belongs to, so a test whose `box()` fails does not fail the rest of its batch. A failed check of the executable of
 * the batch as a whole (see [groupedBatchHandlers]) cannot tell the test to blame, so it fails every test of the batch.
 *
 * @param isolatedTestHandlers The handlers of the one-stage pipeline to process the executable of an isolated test
 *   with. They check the executable of a single test, which a grouped batch does not have, so a test that relies on
 *   one of them for a check that none of [groupedBatchHandlers] makes has to be isolated.
 * @param groupedBatchHandlers The handlers of the one-stage pipeline that check something of every executable, whatever
 *   the test, so they process the executable of a grouped batch as well, as the one of its launcher module. They get
 *   the [JsGroupedBatchArtifact] itself, so a handler can tell it from the executable of a single test.
 */
class JsGroupingStageBoxRunner(
    testServices: TestServices,
    private val isolatedTestHandlers: List<Constructor<JsBinaryArtifactHandler>>,
    private val groupedBatchHandlers: List<Constructor<JsBinaryArtifactHandler>> = emptyList(),
) : GroupingStageHandler<BinaryArtifacts.Js>(
    testServices,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false,
) {
    /** Only runs `box()` of an isolated test. */
    constructor(testServices: TestServices) : this(testServices, listOf(::JsBoxRunner))

    override val artifactKind: TestArtifactKind<BinaryArtifacts.Js>
        get() = ArtifactKinds.Js

    override fun processArtifact(artifact: BinaryArtifacts.Js) {
        val inputs = testServices.groupingStageInputs
        if (artifact is JsGroupedBatchArtifact) {
            runGroupedBatch(artifact, inputs)
        } else {
            checkTestInfrastructure(inputs.size == 1) {
                "The executable of an isolated test is to run a batch of ${inputs.size} tests, but calling `box()` reports a single verdict"
            }
            runIsolated(artifact, inputs.single())
        }
    }

    private fun runIsolated(artifact: BinaryArtifacts.Js, input: NonGroupingStageOutput) {
        val services = input.testServices
        val mainModule = JsEnvironmentConfigurator.getMainModule(services)
        var someAssertionWasFailed = false
        // As in a handlers step of the one-stage pipeline: every handler gets its chance, and knows about the earlier failures.
        for (handler in isolatedTestHandlers.map { it(services) }) {
            for (action in listOf({ handler.processModule(mainModule, artifact) }, { handler.processAfterAllModules(someAssertionWasFailed) })) {
                try {
                    action()
                } catch (e: Throwable) {
                    someAssertionWasFailed = true
                    input.executeWithFailureCatching { throw e }
                }
            }
        }
    }

    private fun runGroupedBatch(artifact: JsGroupedBatchArtifact, inputs: List<NonGroupingStageOutput>) {
        val inputsById = inputs.associateBy { computeJsGroupedTestId(it.testInfo) }
        checkTestInfrastructure(inputsById.size == inputs.size) { "Tests of a grouped batch have clashing ids" }

        val executions = mutableListOf<GroupedTestsResultProtocol.ExecutionOutput>()
        val completedExecutionNames = mutableSetOf<String>()
        val vmFailures = mutableListOf<VmFailure>()
        val launcherServices = inputs.first().testServices
        runGroupedBatchHandlers(artifact, inputs)
        // The file of a module of a per-module mode is loaded after the files of its dependencies, as `getAllFilesForRunner`
        // does it: the libraries go first, then the modules of the tests. The helpers module is a dependency of every test.
        val testModuleIdsInLoadingOrder = inputs.flatMap { input ->
            topologicalOrder(input.testServices.moduleStructure.modules) { module ->
                module.allDependencies.map(DependencyDescription::dependencyModule)
            }.reversed().map { computeJsGroupedTestModuleId(input.testServices, it) }
        }.distinct()
        for ([mode, outputs] in artifact.delegate.compilerResult.entries) {
            // A batch is linked in the whole-program modes, and in the per-module ones if asked to (see `JsInProcessSecondStageFacade.Grouping`).
            checkTestInfrastructure(
                mode.granularity == JsGenerationGranularity.WHOLE_PROGRAM ||
                        mode.granularity == JsGenerationGranularity.PER_MODULE && artifact.linkedPerModule
            ) {
                "A grouped batch linked ${if (artifact.linkedPerModule) "" else "not "}per module was linked in $mode"
            }
            val outputFile = getModeOutputFilePath(launcherServices, artifact.launcherModule, mode)
            val dependencyIds = outputs.dependencies.map { it.artifactConfiguration.moduleName }
            val testModuleIds = testModuleIdsInLoadingOrder.filter { it in dependencyIds }
            val jsFiles = (dependencyIds.filter { it !in testModuleIds } + testModuleIds)
                .map { outputFile.augmentWithModuleName(it) } + outputFile
            val output = try {
                V8JsTestChecker.run(jsFiles) {
                    runTestFunction(
                        testModuleName = artifact.launcherModule.name,
                        testPackageName = FqName.ROOT,
                        testFunctionName = RUN_GROUPED_TESTS_FUNCTION_NAME,
                        withModuleSystem = false,
                    )
                }.also { completedExecutionNames += mode.name }
            } catch (e: ScriptExecutionException) {
                vmFailures += VmFailure(mode.name, e)
                // What was printed before the failure still carries the results of the tests that had finished.
                e.stdout
            }
            executions += GroupedTestsResultProtocol.ExecutionOutput(executionName = mode.name, output = output)
        }

        val parsedBatchResult = GroupedTestsResultProtocol.parseMergedWithExecutionNames(executions)
        if (parsedBatchResult.malformedLines.isNotEmpty()) {
            val reason = "The grouped batch printed malformed result line(s), so its results cannot be trusted:\n" +
                    parsedBatchResult.malformedLines.joinToString("\n") { it.bounded(MAX_MALFORMED_LINE_LENGTH) }
            inputs.forEach { it.failWith(reason, vmFailures) }
            return
        }

        // Every translation mode runs the whole batch, so each of the modes the VM completed must report a result for
        // every test. A mode the VM broke down in is not held to that: the results it printed before still count, and
        // the test that was running is reported as crashed, but the tests it never reached are not failed for it.
        val analysis = parsedBatchResult.analyze(
            inputsById.keys,
            parsedBatchResult.executions.filter { it.executionName in completedExecutionNames },
        )
        checkTestInfrastructure(analysis.excessiveIds.isEmpty()) {
            "The grouped batch reported results of tests that are not a part of it: ${analysis.excessiveIds}. Expected: ${inputsById.keys}"
        }

        // A VM failure is explained by the test that crashed it; an unexplained one, like a failure to load the
        // executable at all, means that no test of the batch has run in that mode, so every test is failed with it.
        val explainedExecutionNames = analysis.failures.values
            .filter { it.kind == FailureKind.CRASHED }
            .flatMapTo(mutableSetOf()) { it.crashExecutionNames }
        val unexplainedVmFailures = vmFailures.filter { it.executionName !in explainedExecutionNames }

        for ([id, input] in inputsById) {
            val failure = analysis.failures[id]
            val executionsWithoutResult = analysis.missingExecutionNamesById[id].orEmpty()
            // The VM failures the test may be a victim of: the one it crashed, and the unexplained ones.
            val relevantVmFailures = when (failure?.kind) {
                FailureKind.CRASHED -> vmFailures.filter { it.executionName in failure.crashExecutionNames } + unexplainedVmFailures
                FailureKind.FAILED -> unexplainedVmFailures
                FailureKind.MISSING, null -> vmFailures
            }
            val reasons = buildList {
                when (failure?.kind) {
                    FailureKind.FAILED -> add(
                        failure.reportedFailure ?: "The test reported a failure carrying neither a message nor details."
                    )
                    FailureKind.CRASHED -> {
                        failure.reportedFailure?.let(::add)
                        add(
                            "The test started but reported no result in ${failure.crashExecutionNames.joinToString()}: " +
                                    "it most likely broke the execution of the whole batch."
                        )
                    }
                    FailureKind.MISSING -> add(
                        "No result was reported for the test in the grouped batch: either the batch was broken " +
                                "before the test was reached, or the test was not linked into it."
                    )
                    null -> if (executionsWithoutResult.isNotEmpty()) {
                        add("The test reported no result in ${executionsWithoutResult.joinToString()}.")
                    }
                }
                if (unexplainedVmFailures.isNotEmpty()) {
                    add("The VM failed in ${unexplainedVmFailures.joinToString { it.executionName }} before any test could be blamed for it.")
                }
            }
            if (reasons.isNotEmpty()) {
                input.failWith(reasons.joinToString("\n"), relevantVmFailures)
            }
        }
    }

    /**
     * A check of the executable as a whole cannot tell the test to blame, so its failure fails every test of the batch.
     * Run alone, as the test engine retries them by default, the tests tell it.
     */
    private fun runGroupedBatchHandlers(artifact: JsGroupedBatchArtifact, inputs: List<NonGroupingStageOutput>) {
        val launcherServices = inputs.first().testServices
        for (handler in groupedBatchHandlers.map { it(launcherServices) }) {
            try {
                handler.processModule(artifact.launcherModule, artifact)
                handler.processAfterAllModules(someAssertionWasFailed = false)
            } catch (e: Throwable) {
                inputs.forEach { input ->
                    input.executeWithFailureCatching {
                        throw AssertionError("A check of the executable of the grouped batch has failed: $e", e)
                    }
                }
            }
        }
    }

    private class VmFailure(val executionName: String, val exception: ScriptExecutionException) {
        fun describe(): String = "V8 failed in $executionName:\n${exception.stderr.bounded(MAX_VM_ERROR_LENGTH)}"
    }

    private fun NonGroupingStageOutput.failWith(reason: String, vmFailures: List<VmFailure>) {
        val message = (listOf(reason) + vmFailures.map { it.describe() }).joinToString("\n")
        executeWithFailureCatching { throw AssertionError(message) }
    }

    /** Routes a failure of [block] into the failure sink of this test, so the test engine attributes it to this test. */
    private fun NonGroupingStageOutput.executeWithFailureCatching(block: () -> Unit) {
        catchingExecutor.executeWithCatching({ WrappedException.FromGroupingHandler(it, this@JsGroupingStageBoxRunner) }, block)
    }

    companion object {
        private const val MAX_VM_ERROR_LENGTH = 8 * 1024
        private const val MAX_MALFORMED_LINE_LENGTH = 1024

        private fun String.bounded(maxLength: Int): String =
            if (length <= maxLength) this else take(maxLength) + "... (${length - maxLength} more characters)"
    }
}
