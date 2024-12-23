/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.converters

import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.IrInliningFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class NativeInliningFacade(
    testServices: TestServices,
) : IrInliningFacade<IrBackendInput>(testServices, BackendKinds.IrBackend, BackendKinds.IrBackend) {
    override fun shouldTransform(module: TestModule): Boolean {
        return module.languageVersionSettings.supportsFeature(LanguageFeature.IrInlinerBeforeKlibSerialization)
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        require(module.languageVersionSettings.languageVersion.usesK2)
        require(inputArtifact is IrBackendInput.NativeAfterFrontendBackendInput) {
            "inputArtifact must be IrBackendInput.NativeAfterFrontendBackendInput"
        }

        return inputArtifact
        /*return nativeLoweringsOfTheFirstPhase?.let {
            null // KT-73624 TODO Invoke lowering prefix
//            val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
//            val transformedModule = PhaseEngine(
//                PhaseConfig(),
//                PhaserState(),
//                NativePreSerializationLoweringContext(inputArtifact.irPluginContext.irBuiltIns, configuration)
//            ).runPreSerializationLoweringPhases(
//                inputArtifact.irModuleFragment,
//                preSerializationLoweringPhasesProvider,
//                configuration
//            )
//
//            inputArtifact.copy(irModuleFragment = transformedModule)
        } ?: inputArtifact */
    }
}

