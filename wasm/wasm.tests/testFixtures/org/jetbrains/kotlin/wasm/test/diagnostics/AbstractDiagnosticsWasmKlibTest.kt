/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.diagnostics

import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.handlers.KlibBackendDiagnosticsHandler
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.configuration.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.services.AbstractEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfiguratorJs
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfiguratorWasi
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.wasm.test.converters.FirWasmKlibSerializerFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmPreSerializationLoweringFacade
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestPhase
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives

abstract class AbstractDiagnosticsWasmKlibTestBase(
    val parser: FirParser,
    private val targetPlatform: TargetPlatform,
    private val wasmEnvironmentConfigurator: Constructor<AbstractEnvironmentConfigurator>,
) : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = this@AbstractDiagnosticsWasmKlibTestBase.targetPlatform
            dependencyKind = DependencyKind.Source
            targetBackend = TargetBackend.WASM
        }

        configureFirParser(parser)

        useAfterAnalysisCheckers(
            ::PhasedPipelineChecker.bind(TestPhase.FRONTEND),
            ::BlackBoxCodegenSuppressor,
        )

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

        facadeStep(::Fir2IrResultsConverter)
        irHandlersStep()

        facadeStep(::WasmPreSerializationLoweringFacade)
        loweredIrHandlersStep()

        configureLoweredIrHandlersStep {
            useHandlers(::IrDiagnosticsHandler)
        }
        facadeStep { FirWasmKlibSerializerFacade(it, true) }

        klibArtifactsHandlersStep {
            useHandlers(::KlibBackendDiagnosticsHandler)
        }

        defaultDirectives {
            TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE with TestPhase.BACKEND
        }

        forTestsMatching("compiler/testData/diagnostics/wasmTests/multiplatform/*") {
            defaultDirectives {
                LanguageSettingsDirectives.LANGUAGE + "+MultiPlatformProjects"
            }
        }
    }
}

abstract class AbstractDiagnosticsFirWasmKlibTest : AbstractDiagnosticsWasmKlibTestBase(
    FirParser.Psi,
    WasmPlatforms.wasmJs,
    ::WasmEnvironmentConfiguratorJs
)

abstract class AbstractDiagnosticsFirWasmWasiKlibTest : AbstractDiagnosticsWasmKlibTestBase(
    FirParser.Psi,
    WasmPlatforms.wasmWasi,
    ::WasmEnvironmentConfiguratorWasi
)
