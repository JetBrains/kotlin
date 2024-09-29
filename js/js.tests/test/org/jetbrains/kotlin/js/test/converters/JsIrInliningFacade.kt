/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.IrInliningFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

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
        // TODO: KT-71917: Invoke lowering prefix and IR Inliner

        // The returned artifact will be stored in dependencyProvider instead of `inputArtifact`, with same kind=BackendKinds.IrBackend
        // Later, third artifact of class `JsIrDeserializedFromKlibBackendInput` might replace it again during some test pipelines.
        return inputArtifact
    }
}
