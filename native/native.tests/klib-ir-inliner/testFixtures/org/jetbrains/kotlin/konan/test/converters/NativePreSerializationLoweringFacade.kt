/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.converters

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.konan.NativePreSerializationLoweringContext
import org.jetbrains.kotlin.cli.common.runPreSerializationLoweringPhases
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.inline.konan.nativeLoweringsOfTheFirstPhase
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.IrPreSerializationLoweringFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime

class NativePreSerializationLoweringFacade(
    testServices: TestServices,
) : IrPreSerializationLoweringFacade<IrBackendInput>(testServices, BackendKinds.IrBackend, BackendKinds.IrBackend) {
    override fun shouldTransform(module: TestModule): Boolean {
        return module.languageVersionSettings.supportsFeature(LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization)
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        require(module.languageVersionSettings.languageVersion.usesK2)
        require(inputArtifact is IrBackendInput.NativeAfterFrontendBackendInput) {
            "inputArtifact must be IrBackendInput.NativeAfterFrontendBackendInput"
        }

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(configuration)
        val phaseConfig = PhaseConfig()
        val transformedModule = configuration.perfManager.tryMeasurePhaseTime(PhaseType.IrPreLowering) {
            PhaseEngine(
                phaseConfig,
                PhaserState(),
                NativePreSerializationLoweringContext(inputArtifact.irBuiltIns, configuration, irDiagnosticReporter)
            ).runPreSerializationLoweringPhases(
                nativeLoweringsOfTheFirstPhase(module.languageVersionSettings),
                inputArtifact.irModuleFragment,
            )
        }

        return inputArtifact.copy(irModuleFragment = transformedModule, diagnosticReporter = irDiagnosticReporter.diagnosticReporter)
    }
}

