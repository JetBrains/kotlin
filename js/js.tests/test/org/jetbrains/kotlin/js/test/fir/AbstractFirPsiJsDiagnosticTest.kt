/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.js.test.JsAdditionalSourceProvider
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractFirJsDiagnosticTestBase(val parser: FirParser) : AbstractKotlinCompilerTest() {
    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JsPlatforms.defaultJsPlatform
            dependencyKind = DependencyKind.Source
        }

        defaultDirectives {
            +ConfigurationDirectives.WITH_STDLIB
            DiagnosticsDirectives.DIAGNOSTICS with listOf("-warnings", "-infos")
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

abstract class AbstractFirPsiJsDiagnosticTest : AbstractFirJsDiagnosticTestBase(FirParser.Psi)
