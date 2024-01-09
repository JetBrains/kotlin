/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.js.test.JsAdditionalSourceProvider
import org.jetbrains.kotlin.js.test.converters.FirJsKlibBackendFacade
import org.jetbrains.kotlin.js.test.handlers.JsBackendDiagnosticsHandler
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrJsResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractFirJsDiagnosticTestBase(val parser: FirParser) : AbstractKotlinCompilerTest() {
    protected open fun configureTestBuilder(builder: TestConfigurationBuilder) = builder.apply {
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

    final override fun TestConfigurationBuilder.configuration() {
        configureTestBuilder(this@configuration)
    }
}

abstract class AbstractFirJsDiagnosticWithBackendTestBase(parser: FirParser) : AbstractFirJsDiagnosticTestBase(parser) {
    override fun configureTestBuilder(builder: TestConfigurationBuilder) = builder.apply {
        super.configureTestBuilder(builder)

        facadeStep(::Fir2IrJsResultsConverter)
        facadeStep { FirJsKlibBackendFacade(it, true) }

        // TODO: Currently do not run lowerings, because they don't report anything;
        //      see KT-61881, KT-61882
        // facadeStep { JsIrBackendFacade(it, firstTimeCompilation = true) }

        klibArtifactsHandlersStep {
            useHandlers(::JsBackendDiagnosticsHandler)
        }
    }
}

abstract class AbstractFirPsiJsDiagnosticTest : AbstractFirJsDiagnosticTestBase(FirParser.Psi)
abstract class AbstractFirLightTreeJsDiagnosticTest : AbstractFirJsDiagnosticTestBase(FirParser.LightTree)

abstract class AbstractFirPsiJsDiagnosticWithBackendTest : AbstractFirJsDiagnosticWithBackendTestBase(FirParser.Psi)
abstract class AbstractFirLightTreeJsDiagnosticWithBackendTest : AbstractFirJsDiagnosticWithBackendTestBase(FirParser.LightTree)
