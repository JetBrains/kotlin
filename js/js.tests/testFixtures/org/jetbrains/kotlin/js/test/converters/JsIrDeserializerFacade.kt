/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.pipeline.web.WebLoadedIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.js.JsIrLoadingPipelinePhase
import org.jetbrains.kotlin.js.test.utils.JsIrIncrementalDataProvider
import org.jetbrains.kotlin.test.backend.ir.IrDeserializerCliFacade
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.service

class JsIrDeserializerFacade(testServices: TestServices) :
    IrDeserializerCliFacade<JsIrLoadingPipelinePhase, WebLoadedIrPipelineArtifact>(testServices, JsIrLoadingPipelinePhase) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(
            service(::JsIrIncrementalDataProvider),
        )
}
