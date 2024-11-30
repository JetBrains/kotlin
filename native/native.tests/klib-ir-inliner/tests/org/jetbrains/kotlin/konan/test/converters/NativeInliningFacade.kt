/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.converters

import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.cli.common.runPreSerializationLoweringPhases
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.ir.inline.PreSerializationLoweringPhasesProvider
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.IrInliningFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

class NativeInliningFacade(
    testServices: TestServices,
    private val preSerializationLoweringPhasesProvider: PreSerializationLoweringPhasesProvider<PreSerializationLoweringContext>?,
) : IrInliningFacade<IrBackendInput>(testServices, BackendKinds.IrBackend, BackendKinds.IrBackend) {
    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return module.languageVersionSettings.supportsFeature(LanguageFeature.IrInlinerBeforeKlibSerialization)
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        require(module.languageVersionSettings.languageVersion.usesK2)
        require(inputArtifact is IrBackendInput.NativeAfterFrontendBackendInput) {
            "inputArtifact must be IrBackendInput.NativeAfterFrontendBackendInput"
        }

        return preSerializationLoweringPhasesProvider?.let {
            val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
            val transformedModule = PhaseEngine(
                PhaseConfig(),
                PhaserState(),
                PreSerializationLoweringContext(inputArtifact.irPluginContext.irBuiltIns, configuration)
            ).runPreSerializationLoweringPhases(
                inputArtifact.irModuleFragment,
                preSerializationLoweringPhasesProvider,
                configuration
            )

            inputArtifact.copy(irModuleFragment = transformedModule)
        } ?: inputArtifact
    }
}

