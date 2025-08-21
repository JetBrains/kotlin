/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.pipeline.web.JsFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebFir2IrPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WebFrontendPipelineArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliFacade
import org.jetbrains.kotlin.test.services.TestServices

class Fir2IrCliWebFacade(
    testServices: TestServices,
) : Fir2IrCliFacade<WebFir2IrPipelinePhase, WebFrontendPipelineArtifact, JsFir2IrPipelineArtifact>(testServices, WebFir2IrPipelinePhase)
