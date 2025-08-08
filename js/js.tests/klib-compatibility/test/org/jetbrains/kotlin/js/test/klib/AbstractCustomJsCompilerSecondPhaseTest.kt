/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWebFacade
import org.jetbrains.kotlin.js.test.fir.setupDefaultDirectivesForFirJsBoxTest
import org.jetbrains.kotlin.js.test.ir.commonConfigurationForJsTest
import org.jetbrains.kotlin.js.test.ir.configureJsBoxHandlers
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.jsArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.junit.jupiter.api.Tag

@Tag("custom-second-phase")
open class AbstractCustomJsCompilerSecondPhaseTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JS_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        setupDefaultDirectivesForFirJsBoxTest(FirParser.LightTree)

        commonConfigurationForJsTest(
            targetFrontend = FrontendKinds.FIR,
            frontendFacade = ::FirCliWebFacade,
            frontendToIrConverter = ::Fir2IrCliWebFacade,
            serializerFacade = ::FirKlibSerializerCliWebFacade,
        )

        configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }

        facadeStep(::CustomJsCompilerSecondPhaseFacade)

        jsArtifactsHandlersStep()
        configureJsBoxHandlers()

        useAfterAnalysisCheckers(
            // ... or failed on the second phase, but they are anyway marked as "IGNORE_BACKEND*".
            ::BlackBoxCodegenSuppressor,
        )
    }
}