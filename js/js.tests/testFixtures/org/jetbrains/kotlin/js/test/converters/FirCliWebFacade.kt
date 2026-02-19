/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.pipeline.web.WebFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebFrontendPipelinePhase
import org.jetbrains.kotlin.test.frontend.fir.FirCliFacade
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices

class FirCliWebFacade(
    testServices: TestServices,
) : FirCliFacade<WebFrontendPipelinePhase, WebFrontendPipelineArtifact>(testServices, WebFrontendPipelinePhase)
