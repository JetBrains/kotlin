/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices

class WasmBackendFacade(
    private val testServices: TestServices
) : AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Wasm>() {
    override val inputKind: ArtifactKinds.KLib get() = ArtifactKinds.KLib
    override val outputKind: ArtifactKinds.Wasm get() = ArtifactKinds.Wasm

    private val deserializerFacade = WasmDeserializerFacade(testServices)

    private val loweringFacade = WasmLoweringFacade(testServices)

    override val additionalServices: List<ServiceRegistrationData>
        get() = deserializerFacade.additionalServices + loweringFacade.additionalServices

    override val directiveContainers: List<DirectivesContainer>
        get() = deserializerFacade.directiveContainers + loweringFacade.directiveContainers

    override fun shouldTransform(module: TestModule): Boolean {
        return deserializerFacade.shouldTransform(module) && loweringFacade.shouldTransform(module)
    }

    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): BinaryArtifacts.Wasm? =
        deserializerFacade.transform(module, inputArtifact)?.let {
            loweringFacade.transform(module, it)
        }
}
