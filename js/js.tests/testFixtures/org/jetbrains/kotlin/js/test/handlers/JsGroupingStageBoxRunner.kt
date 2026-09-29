/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.js.engine.ScriptExecutionException
import org.jetbrains.kotlin.js.test.blackbox.JsGroupedBatchArtifact
import org.jetbrains.kotlin.js.test.blackbox.RUN_GROUPED_TESTS_FUNCTION_NAME
import org.jetbrains.kotlin.js.test.blackbox.computeJsProxyLauncherClassName
import org.jetbrains.kotlin.js.test.converters.augmentWithModuleName
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
import org.jetbrains.kotlin.test.model.GroupingStageHandler
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

/**
 * Runs the executable the grouping stage has linked, on V8.
 *
 * The executable of an isolated test is processed by the handlers of the one-stage pipeline, exactly as it is there.
 * The executable of a grouped batch is run once per translation mode by calling its result-collecting driver; what
 * the driver prints is parsed by [GroupedTestsResultProtocol], and every failure is reported against the test it
 * belongs to, so a failed test does not fail the rest of its batch.
 *
 * @param isolatedTestHandlers The handlers of the one-stage pipeline to process the executable of an isolated test
 *   with. They are of no use for a grouped batch, which has no executable of a single test to look at, so a test
 *   that relies on one of them to check something has to be isolated.
 */
class JsGroupingStageBoxRunner(
    testServices: TestServices,
    private val isolatedTestHandlers: List<Constructor<JsBinaryArtifactHandler>>,
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
        val inputsById = inputs.associateBy { computeJsProxyLauncherClassName(it.testInfo) }
        checkTestInfrastructure(inputsById.size == inputs.size) { "Tests of a grouped batch have clashing launcher names" }

        val executions = mutableListOf<GroupedTestsResultProtocol.ExecutionOutput>()
        val vmFailures = mutableListOf<VmFailure>()
        val launcherServices = inputs.first().testServices
        for ([mode, outputs] in artifact.delegate.compilerResult.entries) {
            val outputFile = getModeOutputFilePath(launcherServices, artifact.launcherModule, mode)
            val jsFiles = outputs.dependencies.map { outputFile.augmentWithModuleName(it.artifactConfiguration.moduleName) } + outputFile
            val output = try {
                V8JsTestChecker.run(jsFiles) {
                    runTestFunction(
                        testModuleName = artifact.launcherModule.name,
                        testPackageName = FqName.ROOT,
                        testFunctionName = RUN_GROUPED_TESTS_FUNCTION_NAME,
                        withModuleSystem = false,
                    )
                }
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

        val analysis = parsedBatchResult.analyze(inputsById.keys, executions)
        checkTestInfrastructure(analysis.excessiveIds.isEmpty()) {
            "The grouped batch reported results of tests that are not a part of it: ${analysis.excessiveIds}. Expected: ${inputsById.keys}"
        }

        var someTestFailed = false
        for ([id, input] in inputsById) {
            val failure = analysis.failures[id]
            val executionsWithoutResult = analysis.missingExecutionNamesById[id].orEmpty()
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
            }
            if (reasons.isNotEmpty()) {
                someTestFailed = true
                input.failWith(reasons.joinToString("\n"), vmFailures)
            }
        }

        // A failure of the VM that no test accounts for must not be lost.
        if (!someTestFailed && vmFailures.isNotEmpty()) {
            testServices.assertions.failAll(vmFailures.map { AssertionError(it.describe(), it.exception) })
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
