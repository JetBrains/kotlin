/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.js.test.JsAdditionalSourceProvider
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.*
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.test.services.JsLibraryProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractFirJsDiagnosticTest : AbstractKotlinCompilerTest() {
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

        enableMetaInfoHandler()
        configurationForClassicAndFirTestsAlongside()

        useAfterAnalysisCheckers(
            ::TestPassesNotifier,
        )

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsEnvironmentConfigurator,
        )

        useAdditionalSourceProviders(
            ::JsAdditionalSourceProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        useAdditionalService(::JsLibraryProvider)

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

class TestPassesNotifier(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    companion object {
        const val SUPPRESS_FAILING = true
    }

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val testFile = testServices.moduleStructure.originalTestDataFiles.first()

        val failedNonSuppressibleAssertions = failedAssertions.filter {
            when (it) {
                is WrappedException.FromFacade -> it.facade !is FirFrontendFacade && it.facade !is Fir2IrResultsConverter
                is WrappedException.FromHandler -> it.handler.artifactKind != FrontendKinds.FIR
                is WrappedException.FromMetaInfoHandler -> false
                else -> true
            }
        }

        if (failedAssertions.size == failedNonSuppressibleAssertions.size) {
            val lines = testFile.readLines()
            val clearedLines = lines.filter { it.trim() != "// FIR_IGNORE" }
            val hasFirIgnore = lines.size != clearedLines.size

            if (hasFirIgnore) {
                testFile.writeText(clearedLines.joinToString("\n"))
            }
        }

        return if (SUPPRESS_FAILING) {
            failedNonSuppressibleAssertions
        } else {
            failedAssertions
        }
    }
}
