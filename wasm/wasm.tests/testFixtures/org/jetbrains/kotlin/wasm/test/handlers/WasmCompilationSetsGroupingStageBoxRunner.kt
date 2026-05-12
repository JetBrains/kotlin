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
import org.jetbrains.kotlin.platform.wasm.isWasmWasi
import org.jetbrains.kotlin.test.isSingleTestBatch
import org.jetbrains.kotlin.wasm.test.converters.WasmBackendFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmInProcessSecondStageFacade

/**
 * A Wasm variant of the grouping stage handler for the IN_PROCESS second-stage mode.
 * Handles [WasmCompilationSetsBinaryArtifact] artifacts produced by [WasmBackendFacade] or [WasmInProcessSecondStageFacade]
 * by delegating to [WasmBoxRunner.runWasmCode] which invokes the relevant VMs.
 */
open class WasmCompilationSetsGroupingStageBoxRunner(
    testServices: TestServices
) : AbstractWasmGroupingStageBoxRunner(testServices) {
    override val artifactKind: TestArtifactKind<BinaryArtifacts.Wasm>
        get() = ArtifactKinds.Wasm
    protected val firstNonGroupingTestServices: TestServices
        get() = testServices.groupingStageInputs.first().testServices
    open val wasmBoxRunner: WasmBoxRunner
        get() = WasmBoxRunner(firstNonGroupingTestServices, executeWithV8Only = false)
    private val wasiBoxRunner: WasiBoxRunner
        get() = WasiBoxRunner(firstNonGroupingTestServices)

    // The target is detected from the module target platforms rather than from a compiler
    // configuration: building a second-stage configuration eagerly requires a registered `KLib`
    // artifact for the queried module, which is absent for the `common`/metadata module of HMPP
    // tests (`modules.first()`), causing `Artifact with kind KLib is not registered`.
    private val isWasiTarget: Boolean
        get() = firstNonGroupingTestServices.moduleStructure.modules.any {
            firstNonGroupingTestServices.targetPlatformProvider.getTargetPlatform(it).isWasmWasi()
        }

    override fun shouldUseBoxExportMode(): Boolean {
        val inputs = testServices.groupingStageInputs
        // Global invariant: a batch of a single test is always run as a standalone box-export test,
        // regardless of why it ended up alone in the batch (isolated, or merely a unique batch token).
        // Single-test box tests without RUN_UNIT_TESTS are compiled without the `@Test` launcher
        // (see `WasmJsLauncherAdditionalSourceProvider`), so they must be executed by calling
        // `jsModule.box()` and asserting "OK", rather than driving the unit-test runner.
        val isSingleTestBatch = testServices.isSingleTestBatch()
        return isSingleTestBatch &&
                RUN_UNIT_TESTS !in firstNonGroupingTestServices.moduleStructure.allDirectives &&
                hasBoxMethod(inputs.first())
    }

    override fun runTestCode(
        artifact: BinaryArtifacts.Wasm,
        useUnitTestRunnerOnly: Boolean,
        outputCollector: MutableList<String>?,
    ): List<Throwable> {
        check(artifact is WasmCompilationSetsBinaryArtifact) {
            "Unexpected artifact type: ${artifact::class}"
        }
        return if (isWasiTarget) {
            wasiBoxRunner.runWasmCode(artifact, useUnitTestRunnerOnly, outputCollector, throwOnExceptions = false)
        } else {
            wasmBoxRunner.runWasmCode(artifact, useUnitTestRunnerOnly, outputCollector, throwOnExceptions = false)
        }
    }
}

class WasmJsCoroutinesStackSwitchingBoxRunner(
    testServices: TestServices
) : WasmCompilationSetsGroupingStageBoxRunner(testServices) {
    override val wasmBoxRunner: WasmBoxRunner
        get() = WasmStackSwitchingRunner(firstNonGroupingTestServices)
}
