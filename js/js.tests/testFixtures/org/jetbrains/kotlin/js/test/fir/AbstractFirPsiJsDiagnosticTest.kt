/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWebFacade
import org.jetbrains.kotlin.js.test.ir.commonConfigurationForJsTest
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.KlibBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureLoweredIrHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.test.configuration.setupHandlersForDiagnosticTest
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestPhase
import org.jetbrains.kotlin.utils.bind

abstract class AbstractFirJsDiagnosticTestBase(val parser: FirParser) : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            targetBackend = TargetBackend.JS_IR
        }
        defaultDirectives {
            +ConfigurationDirectives.WITH_STDLIB
        }

        commonConfigurationForJsTest(
            targetFrontend = FrontendKinds.FIR,
            frontendFacade = ::FirCliWebFacade,
            frontendToIrConverter = ::Fir2IrCliWebFacade,
            serializerFacade = ::FirKlibSerializerCliWebFacade,
        )
        configureFirParser(parser)

        configureFirHandlersStep {
            setupHandlersForDiagnosticTest()
            useHandlers(::NoFirCompilationErrorsHandler)
        }

        useAfterAnalysisCheckers(
            ::PhasedPipelineChecker.bind(TestPhase.FRONTEND),
            ::BlackBoxCodegenSuppressor,
        )
        enableMetaInfoHandler()
        configurationForClassicAndFirTestsAlongside()
        useAdditionalService(::LibraryProvider)
    }
}

abstract class AbstractFirJsDiagnosticTestWithoutBackendTestBase(parser: FirParser) : AbstractFirJsDiagnosticTestBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        defaultDirectives {
            TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE with TestPhase.KLIB
        }
    }
}

abstract class AbstractFirJsDiagnosticWithBackendTestBase(parser: FirParser) : AbstractFirJsDiagnosticTestBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        configureLoweredIrHandlersStep {
            useHandlers(
                ::IrDiagnosticsHandler
            )
        }
        klibArtifactsHandlersStep {
            useHandlers(::KlibBackendDiagnosticsHandler)
        }
        defaultDirectives {
            TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE with TestPhase.BACKEND
        }
    }
}

abstract class AbstractFirJsDiagnosticWithBackendWithInlinedFunInKlibTestBase : AbstractFirJsDiagnosticWithBackendTestBase(FirParser.LightTree) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        defaultDirectives {
            LANGUAGE with listOf(
                "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
            )
        }
    }
}

abstract class AbstractFirJsDiagnosticWithIrInlinerTestBase(parser: FirParser) : AbstractFirJsDiagnosticTestWithoutBackendTestBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        defaultDirectives {
            LANGUAGE with listOf(
                "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
            )
        }

        configureLoweredIrHandlersStep {
            useHandlers(
                ::IrDiagnosticsHandler
            )
        }
    }
}

abstract class AbstractFirPsiJsDiagnosticTest : AbstractFirJsDiagnosticTestWithoutBackendTestBase(FirParser.Psi)
abstract class AbstractFirLightTreeJsDiagnosticTest : AbstractFirJsDiagnosticTestWithoutBackendTestBase(FirParser.LightTree)

abstract class AbstractFirJsDiagnosticWithIrInlinerTest : AbstractFirJsDiagnosticWithIrInlinerTestBase(FirParser.LightTree)

abstract class AbstractFirPsiJsDiagnosticWithBackendTest : AbstractFirJsDiagnosticWithBackendTestBase(FirParser.Psi)
abstract class AbstractFirLightTreeJsDiagnosticWithBackendTest : AbstractFirJsDiagnosticWithBackendTestBase(FirParser.LightTree)
