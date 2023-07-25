/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class JsIrBackendFacade(
    testServices: TestServices,
    firstTimeCompilation: Boolean
) : AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Js>() {
    override val inputKind: ArtifactKinds.KLib get() = ArtifactKinds.KLib
    override val outputKind: ArtifactKinds.Js get() = ArtifactKinds.Js

    constructor(testServices: TestServices) : this(testServices, firstTimeCompilation = true)

    private val deserializerFacade = JsIrDeserializerFacade(testServices, firstTimeCompilation)

    private val loweringFacade = JsIrLoweringFacade(testServices, firstTimeCompilation)

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return deserializerFacade.shouldRunAnalysis(module) && loweringFacade.shouldRunAnalysis(module)
    }

    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): BinaryArtifacts.Js? {
        val deserializedIrBackendInput = deserializerFacade.transform(module, inputArtifact) ?: return null
        return loweringFacade.transform(module, deserializedIrBackendInput)
    }
}
