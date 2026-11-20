/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

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
import org.jetbrains.kotlin.test.services.sourceProviders.hasBoxMethod
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
    protected open val wasiBoxRunner: WasiBoxRunner
        get() = WasiBoxRunner(firstNonGroupingTestServices)

    // The target is detected from the module target platforms rather than from a compiler
    // configuration: building a second-stage configuration eagerly requires a registered `KLib`
    // artifact for the queried module, which is absent for the `common`/metadata module of HMPP
    // tests (`modules.first()`), causing `Artifact with kind KLib is not registered`.
    private val isWasiTarget: Boolean
        get() = firstNonGroupingTestServices.moduleStructure.modules.any {
            firstNonGroupingTestServices.targetPlatformProvider.getTargetPlatform(it).isWasmWasi()
        }

    override fun shouldUseBoxExportModeWhenDriverless(): Boolean {
        val inputs = testServices.groupingStageInputs
        // A single-test batch is compiled without the `@Test` launcher (see `WasmJsLauncherAdditionalSourceProvider`),
        // whether it ended up alone through isolation or merely through a unique batch token, so without a driver
        // the only way to get its verdict is to call `box()` and assert "OK".
        val isSingleTestBatch = testServices.isSingleTestBatch()
        return isSingleTestBatch &&
                RUN_UNIT_TESTS !in firstNonGroupingTestServices.moduleStructure.allDirectives &&
                inputs.first().hasBoxMethod()
    }

    override fun runTestCode(
        artifact: BinaryArtifacts.Wasm,
        useUnitTestRunnerOnly: Boolean,
        outputCollector: MutableList<WasmVMOutput>?,
    ): List<Throwable> {
        check(artifact is WasmCompilationSetsBinaryArtifact) {
            "Unexpected artifact type: ${artifact::class}"
        }
        return if (isWasiTarget) {
            wasiBoxRunner.runWasmCode(
                artifact,
                useUnitTestRunnerOnly,
                outputCollector,
                throwOnExceptions = false,
            )
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
