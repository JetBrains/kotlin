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
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.builders.configureLoweredIrHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.test.configuration.setupHandlersForDiagnosticTest
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestPhase
import org.jetbrains.kotlin.utils.bind

abstract class AbstractJsDiagnosticTestBase(val parser: FirParser) : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            targetBackend = TargetBackend.JS_IR
        }
        defaultDirectives {
            +ConfigurationDirectives.WITH_STDLIB
            +FirDiagnosticsDirectives.FIR_IDENTICAL
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
        configureIrHandlersStep {
            useHandlers(::IrDiagnosticsHandler)
        }
        configureLoweredIrHandlersStep {
            useHandlers(::IrDiagnosticsHandler)
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

abstract class AbstractJsDiagnosticTestWithoutBackendTestBase(parser: FirParser) : AbstractJsDiagnosticTestBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        defaultDirectives {
            TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE with TestPhase.KLIB
        }
    }
}

abstract class AbstractJsDiagnosticWithBackendTestBase(parser: FirParser) : AbstractJsDiagnosticTestBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        klibArtifactsHandlersStep {
            useHandlers(::KlibBackendDiagnosticsHandler)
        }
        defaultDirectives {
            TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE with TestPhase.BACKEND
        }
    }
}

abstract class AbstractJsDiagnosticWithBackendWithInlinedFunInKlibTestBase : AbstractJsDiagnosticWithBackendTestBase(FirParser.LightTree) {
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

abstract class AbstractJsDiagnosticWithIrInlinerTestBase(parser: FirParser) : AbstractJsDiagnosticTestWithoutBackendTestBase(parser) {
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

abstract class AbstractPsiJsDiagnosticTest : AbstractJsDiagnosticTestWithoutBackendTestBase(FirParser.Psi)
abstract class AbstractLightTreeJsDiagnosticTest : AbstractJsDiagnosticTestWithoutBackendTestBase(FirParser.LightTree)

abstract class AbstractJsDiagnosticWithIrInlinerTest : AbstractJsDiagnosticWithIrInlinerTestBase(FirParser.LightTree)

abstract class AbstractPsiJsDiagnosticWithBackendTest : AbstractJsDiagnosticWithBackendTestBase(FirParser.Psi)
abstract class AbstractLightTreeJsDiagnosticWithBackendTest : AbstractJsDiagnosticWithBackendTestBase(FirParser.LightTree)
