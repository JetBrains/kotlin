/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.cli.common.runPreSerializationLoweringPhases
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.backend.js.JsPreSerializationLoweringContext
import org.jetbrains.kotlin.ir.backend.js.JsPreSerializationLoweringPhasesProvider
import org.jetbrains.kotlin.js.test.utils.createTestPhaseConfig
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.IrInliningFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

class JsIrInliningFacade(
    testServices: TestServices,
) : IrInliningFacade<IrBackendInput>(testServices, BackendKinds.IrBackend, BackendKinds.IrBackend) {
    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return module.languageVersionSettings.supportsFeature(LanguageFeature.IrInlinerBeforeKlibSerialization)
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        require(module.languageVersionSettings.languageVersion.usesK2)
        require(inputArtifact is IrBackendInput.JsIrAfterFrontendBackendInput) {
            "inputArtifact must be IrBackendInput.JsIrAfterFrontendBackendInput"
        }

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val phases = JsPreSerializationLoweringPhasesProvider.lowerings(configuration)
        val phaseConfig = createTestPhaseConfig(testServices, module, phases)

        val transformedModule = PhaseEngine(
            phaseConfig,
            PhaserState(),
            JsPreSerializationLoweringContext(inputArtifact.irPluginContext.irBuiltIns, configuration)
        ).runPreSerializationLoweringPhases(
            inputArtifact.irModuleFragment,
            JsPreSerializationLoweringPhasesProvider,
            configuration
        )

        // The returned artifact will be stored in dependencyProvider instead of `inputArtifact`, with same kind=BackendKinds.IrBackend
        // Later, third artifact of class `JsIrDeserializedFromKlibBackendInput` might replace it again during some test pipelines.
        return inputArtifact.copy(irModuleFragment = transformedModule)
    }
}
