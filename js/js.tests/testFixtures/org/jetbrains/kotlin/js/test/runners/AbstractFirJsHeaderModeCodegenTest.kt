/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners

import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliJsFacade
import org.jetbrains.kotlin.js.test.converters.JsIrPreSerializationLoweringFacade
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpAfterInliningVerifyingHandler
import org.jetbrains.kotlin.test.backend.handlers.KlibBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_HEADER_MODE
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.HEADER_MODE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.utils.bind

abstract class AbstractFirJsHeaderModeCodegenTestBase(
    val parser: FirParser
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JS_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        configureFirParser(parser)

        commonConfigurationForJsHeaderModeTest(
            ::FirCliWebFacade,
            ::Fir2IrCliWebFacade,
            ::JsIrPreSerializationLoweringFacade,
            ::FirKlibSerializerCliJsFacade,
        )

        configureJsHeaderModeHandlers(
            ::FirDiagnosticsHandler,
            ::NoIrCompilationErrorsHandler,
            ::KlibBackendDiagnosticsHandler,
            ::KlibAbiDumpAfterInliningVerifyingHandler,
        )

        defaultDirectives {
            +HEADER_MODE
            +WITH_STDLIB
        }

        useFailureSuppressors(
            ::BlackBoxCodegenSuppressor.bind(IGNORE_HEADER_MODE, null),
        )
    }
}

fun TestConfigurationBuilder.commonConfigurationForJsHeaderModeTest(
    frontendFacade: Constructor<AbstractTestFacade<ResultingArtifact.Source, FirOutputArtifact>>,
    fir2IrFacade: Constructor<AbstractTestFacade<FirOutputArtifact, IrBackendInput>>,
    loweringFacade: Constructor<AbstractTestFacade<IrBackendInput, IrBackendInput>>,
    serializerFacade: Constructor<AbstractTestFacade<IrBackendInput, BinaryArtifacts.KLib>>,
) {
    commonServicesConfigurationForJsCodegenTest()

    facadeStep(frontendFacade)
    facadeStep(fir2IrFacade)
    facadeStep(loweringFacade)
    facadeStep(serializerFacade)
}

fun TestConfigurationBuilder.configureJsHeaderModeHandlers(
    firDiagnosticsHandler: Constructor<FirAnalysisHandler>,
    irCompilationErrorsHandler: Constructor<BackendInputHandler<IrBackendInput>>,
    klibDiagnosticsHandler: Constructor<BinaryArtifactHandler<BinaryArtifacts.KLib>>,
    klibAbiDumpHandler: Constructor<BinaryArtifactHandler<BinaryArtifacts.KLib>>,
) {
    firHandlersStep {
        useHandlers(firDiagnosticsHandler)
    }

    irHandlersStep {
        useHandlers(irCompilationErrorsHandler)
    }

    loweredIrHandlersStep {
        useHandlers(irCompilationErrorsHandler)
    }

    klibArtifactsHandlersStep {
        useHandlers(klibDiagnosticsHandler, klibAbiDumpHandler)
    }
}

open class AbstractFirJsLightTreeHeaderModeCodegenTest : AbstractFirJsHeaderModeCodegenTestBase(FirParser.LightTree)
