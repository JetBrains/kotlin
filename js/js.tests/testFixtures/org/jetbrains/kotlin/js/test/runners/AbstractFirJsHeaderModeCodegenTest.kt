/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners

import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliJsFacade
import org.jetbrains.kotlin.js.test.converters.JsIrPreSerializationLoweringFacade
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpAfterInliningVerifyingHandler
import org.jetbrains.kotlin.test.backend.handlers.KlibBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.builders.loweredIrHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_HEADER_MODE
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.HEADER_MODE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerJsTest
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.bind

abstract class AbstractFirJsHeaderModeCodegenTestBase(val parser: FirParser) : AbstractKotlinCompilerJsTest(TargetBackend.JS_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            +HEADER_MODE
            +WITH_STDLIB
        }
        configureFirParser(parser)

        commonServicesConfigurationForJsCodegenTest()
        facadeStep(::FirCliWebFacade)
        firHandlersStep {
            useHandlers(::FirDiagnosticsHandler)
        }
        facadeStep(::Fir2IrCliWebFacade)
        irHandlersStep {
            useHandlers(::NoIrCompilationErrorsHandler)
        }
        facadeStep(::JsIrPreSerializationLoweringFacade)
        loweredIrHandlersStep {
            useHandlers(::NoIrCompilationErrorsHandler)
        }
        facadeStep(::FirKlibSerializerCliJsFacade)
        klibArtifactsHandlersStep {
            useHandlers(
                ::KlibBackendDiagnosticsHandler,
                ::KlibAbiDumpAfterInliningVerifyingHandler,
            )
        }

        useFailureSuppressors(
            ::BlackBoxCodegenSuppressor.bind(IGNORE_HEADER_MODE, null),
        )
    }
}

open class AbstractFirJsLightTreeHeaderModeCodegenTest : AbstractFirJsHeaderModeCodegenTestBase(FirParser.LightTree)
