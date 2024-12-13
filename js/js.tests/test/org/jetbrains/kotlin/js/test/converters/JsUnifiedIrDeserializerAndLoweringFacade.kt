/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.ir.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

/**
 * The unified facade that runs internally [JsIrDeserializerFacade] and then [JsIrLoweringFacade].
 *
 * The goal of this facade is to avoid re-registering [IrBackendInput] artifact that otherwise
 * would be produced as a result of [JsIrDeserializerFacade] execution.
 */
class JsUnifiedIrDeserializerAndLoweringFacade(
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
        val configuration = deserializerFacade.testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        // Enforce PL with the ERROR log level to fail any tests where PL detected any incompatibilities.
        configuration.setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.ERROR))

        return deserializerFacade.transform(module, inputArtifact)?.let {
            loweringFacade.transform(module, it)
        }
    }
}
