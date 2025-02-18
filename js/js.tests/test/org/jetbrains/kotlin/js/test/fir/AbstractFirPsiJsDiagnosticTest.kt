/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.js.test.JsAdditionalSourceProvider
import org.jetbrains.kotlin.js.test.converters.FirJsKlibSerializerFacade
import org.jetbrains.kotlin.js.test.converters.JsIrInliningFacade
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.KlibBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.inlinedIrHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.configuration.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractFirJsDiagnosticTestBase(val parser: FirParser) : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JsPlatforms.defaultJsPlatform
            targetBackend = TargetBackend.JS_IR
            dependencyKind = DependencyKind.Source
        }

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)

        defaultDirectives {
            +ConfigurationDirectives.WITH_STDLIB
        }

        configureFirParser(parser)

        enableMetaInfoHandler()
        configurationForClassicAndFirTestsAlongside()

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsEnvironmentConfigurator,
        )

        useAdditionalSourceProviders(
            ::JsAdditionalSourceProvider,
            ::CoroutineHelpersSourceFilesProvider,
            ::AdditionalDiagnosticsSourceFilesProvider,
        )

        useAdditionalService(::LibraryProvider)

        facadeStep(::FirFrontendFacade)

        firHandlersStep {
            useHandlers(
                ::FirDiagnosticsHandler,
                ::FirDumpHandler,
                ::FirCfgDumpHandler,
                ::FirCfgConsistencyHandler,
                ::FirResolvedTypesVerifier,
                ::FirScopeDumpHandler,
            )
        }
    }
}

abstract class AbstractFirJsDiagnosticWithBackendTestBase(parser: FirParser) : AbstractFirJsDiagnosticTestBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)

        facadeStep(::Fir2IrResultsConverter)
        defaultDirectives {
            LANGUAGE with "+${LanguageFeature.IrInlinerBeforeKlibSerialization.name}"
        }

        facadeStep(::JsIrInliningFacade)
        inlinedIrHandlersStep { useHandlers(::IrDiagnosticsHandler) }

        facadeStep(::FirJsKlibSerializerFacade)

        // TODO: Currently do not run lowerings, because they don't report anything;
        //      see KT-61881, KT-61882
        // facadeStep { JsIrBackendFacade(it, firstTimeCompilation = true) }

        klibArtifactsHandlersStep {
            useHandlers(::KlibBackendDiagnosticsHandler)
        }
    }
}

abstract class AbstractFirJsDiagnosticWithIrInlinerTestBase(parser: FirParser) : AbstractFirJsDiagnosticTestBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        defaultDirectives {
            LANGUAGE with "+${LanguageFeature.IrInlinerBeforeKlibSerialization.name}"
        }

        facadeStep(::Fir2IrResultsConverter)
        facadeStep(::JsIrInliningFacade)

        irHandlersStep {
            useHandlers(
                ::IrDiagnosticsHandler
            )
        }
    }
}

abstract class AbstractFirPsiJsDiagnosticTest : AbstractFirJsDiagnosticTestBase(FirParser.Psi)
abstract class AbstractFirLightTreeJsDiagnosticTest : AbstractFirJsDiagnosticTestBase(FirParser.LightTree)

abstract class AbstractFirJsDiagnosticWithIrInlinerTest : AbstractFirJsDiagnosticWithIrInlinerTestBase(FirParser.LightTree)

abstract class AbstractFirPsiJsDiagnosticWithBackendTest : AbstractFirJsDiagnosticWithBackendTestBase(FirParser.Psi)
abstract class AbstractFirLightTreeJsDiagnosticWithBackendTest : AbstractFirJsDiagnosticWithBackendTestBase(FirParser.LightTree)
