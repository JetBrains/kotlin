/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class WasmBackendFacade(
    private val testServices: TestServices
) : AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Wasm>() {
    override val inputKind = ArtifactKinds.KLib
    override val outputKind = ArtifactKinds.Wasm

    private val deserializerFacade = WasmDeserializerFacade(testServices)

    private val loweringFacade = WasmLoweringFacade(testServices)

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return deserializerFacade.shouldRunAnalysis(module) && loweringFacade.shouldRunAnalysis(module)
    }

    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): BinaryArtifacts.Wasm? =
        deserializerFacade.transform(module, inputArtifact)?.let {
            loweringFacade.transform(module, it)
        }
}
