/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.RUN_UNIT_TESTS
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.WasmCompilationSetsBinaryArtifact
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.targetPlatformProvider
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.platform.wasm.isWasmWasi
import org.jetbrains.kotlin.wasm.test.converters.WasmBackendFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmInProcessSecondStageFacade

/**
 * A Wasm variant of the grouping stage handler for the IN_PROCESS second-stage mode.
 * Handles [WasmCompilationSetsBinaryArtifact] artifacts produced by [WasmBackendFacade] or [WasmInProcessSecondStageFacade]
 * by delegating to [WasmBoxRunner.runWasmCode] which invokes the relevant VMs.
 *
 * @see org.jetbrains.kotlin.wasm.test.blackbox.SecondStageInvocationMode.IN_PROCESS
 */
class WasmCompilationSetsBoxRunnerGroupingStage(
    testServices: TestServices
) : AbstractWasmBoxRunnerGroupingStage(testServices) {
    override val artifactKind: TestArtifactKind<BinaryArtifacts.Wasm>
        get() = ArtifactKinds.Wasm
    private val firstNonGroupingTestServices: TestServices
        get() = testServices.groupingStageInputs.first().testServices
    private val wasmBoxRunner: WasmBoxRunner
        get() = WasmBoxRunner(firstNonGroupingTestServices, executeWithV8Only = false)
    private val wasiBoxRunner: WasiBoxRunner
        get() = WasiBoxRunner(firstNonGroupingTestServices)

    // WASI box tests cannot be executed on the JS engines (V8/SpiderMonkey/JSC) used by
    // [WasmBoxRunner]: the emitted `index.mjs` does `import { WASI } from 'wasi'`, which only the
    // WASI VMs (NodeJs/WasmEdge/Wasmtime) driven by [WasiBoxRunner] can resolve. So for the WASI
    // target we delegate to [WasiBoxRunner], mirroring how the CLI path uses
    // [WasmWasiFolderBoxRunnerGroupingStage] instead of [WasmFolderBoxRunnerGroupingStage].
    //
    // The target is detected from the module target platforms rather than from a compiler
    // configuration: building a second-stage configuration eagerly requires a registered `KLib`
    // artifact for the queried module, which is absent for the `common`/metadata module of HMPP
    // tests (`modules.first()`), causing `Artifact with kind KLib is not registered`.
    private val isWasiTarget: Boolean
        get() = firstNonGroupingTestServices.moduleStructure.modules.any {
            firstNonGroupingTestServices.targetPlatformProvider.getTargetPlatform(it).isWasmWasi()
        }

    private fun runWasmCode(
        artifact: WasmCompilationSetsBinaryArtifact,
        useUnitTestRunnerOnly: Boolean,
        throwOnExceptions: Boolean,
        outputCollector: MutableList<String>?,
    ): List<Throwable> = if (isWasiTarget) {
        wasiBoxRunner.runWasmCode(artifact, useUnitTestRunnerOnly, outputCollector, throwOnExceptions)
    } else {
        wasmBoxRunner.runWasmCode(artifact, useUnitTestRunnerOnly, outputCollector, throwOnExceptions)
    }

    override fun processArtifact(artifact: BinaryArtifacts.Wasm) {
        check(artifact is WasmCompilationSetsBinaryArtifact) {
            "Unexpected artifact type: ${artifact::class}"
        }

        val inputs = testServices.groupingStageInputs
        // Global invariant: a batch of a single test is always run as a standalone box-export test,
        // regardless of why it ended up alone in the batch (isolated, or merely a unique batch token).
        val isSingleTestBatch = inputs.map { it.testServices.testInfo }.distinct().size == 1

        // A single-test box test that does not exercise the unit-test runner is compiled without the
        // `@Test` launcher (see `WasmJsLauncherAdditionalSourceProvider`), so it must be executed the
        // same way `WasmBoxRunner` runs `FirWasmJsCodegenBoxTestGenerated`: call `jsModule.box()` and
        // assert `"OK"`, rather than driving the unit-test runner and verifying TeamCity suite markers.
        if (isSingleTestBatch &&
            RUN_UNIT_TESTS !in firstNonGroupingTestServices.moduleStructure.allDirectives &&
            hasBoxMethod(inputs.first())
        ) {
            val input = inputs.first()
            val exceptions = runWasmCode(
                artifact,
                useUnitTestRunnerOnly = false,
                throwOnExceptions = false,
                outputCollector = null,
            )
            if (exceptions.isNotEmpty()) {
                input.catchingExecutor.executeWithCatching({ WrappedException.FromGroupingHandler(it, this) }) {
                    throw exceptions.first()
                }
            }
            return
        }

        val collectedOutputs = mutableListOf<String>()
        val exceptions = runWasmCode(
            artifact,
            useUnitTestRunnerOnly = true,
            throwOnExceptions = false,
            outputCollector = collectedOutputs,
        )
        handleRunResult(RunResult(collectedOutputs, exceptions))
    }
}
