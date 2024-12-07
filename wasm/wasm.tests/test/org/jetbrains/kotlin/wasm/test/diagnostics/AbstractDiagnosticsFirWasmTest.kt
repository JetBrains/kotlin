/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.diagnostics

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.test.services.AbstractEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfiguratorJs
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfiguratorWasi
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractFirWasmDiagnosticTestBase(
    val parser: FirParser,
    private val targetPlatform: TargetPlatform,
    private val wasmEnvironmentConfigurator: Constructor<AbstractEnvironmentConfigurator>,
) : AbstractKotlinCompilerTest() {
    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = this@AbstractFirWasmDiagnosticTestBase.targetPlatform
            dependencyKind = DependencyKind.Source
            targetBackend = TargetBackend.WASM
        }

        configureFirParser(parser)

        enableMetaInfoHandler()
        configurationForClassicAndFirTestsAlongside()

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            wasmEnvironmentConfigurator,
        )

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )
        useAdditionalService(::LibraryProvider)
        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)

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

        forTestsMatching("compiler/testData/diagnostics/wasmTests/multiplatform/*") {
            defaultDirectives {
                LanguageSettingsDirectives.LANGUAGE + "+MultiPlatformProjects"
            }
        }
    }
}

abstract class AbstractDiagnosticsFirWasmTest : AbstractFirWasmDiagnosticTestBase(
    FirParser.Psi,
    WasmPlatforms.wasmJs,
    ::WasmEnvironmentConfiguratorJs
)

abstract class AbstractDiagnosticsFirWasmWasiTest : AbstractFirWasmDiagnosticTestBase(
    FirParser.Psi,
    WasmPlatforms.wasmWasi,
    ::WasmEnvironmentConfiguratorWasi
)
