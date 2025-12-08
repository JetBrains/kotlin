/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.diagnostics

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.KlibBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestPhase
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.fir.FirOldFrontendMetaConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlin.wasm.test.converters.FirWasmKlibSerializerFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmPreSerializationLoweringFacade

abstract class AbstractWasmDiagnosticTestBase(
    val parser: FirParser,
    private val targetPlatform: TargetPlatform,
    private val wasmTarget: WasmTarget,
) : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = this@AbstractWasmDiagnosticTestBase.targetPlatform
            dependencyKind = DependencyKind.Source
            targetBackend = TargetBackend.WASM
        }

        defaultDirectives {
            LATEST_PHASE_IN_PIPELINE with TestPhase.BACKEND
        }
        useAfterAnalysisCheckers(
            ::PhasedPipelineChecker.bind(TestPhase.FRONTEND),
        )
        configureFirParser(parser)

        useMetaTestConfigurators(::FirOldFrontendMetaConfigurator)
        enableMetaInfoHandler()

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::WasmFirstStageEnvironmentConfigurator.bind(wasmTarget),
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
                ::NoFirCompilationErrorsHandler,
            )
        }

        facadeStep(::Fir2IrResultsConverter)
        irHandlersStep()

        withIrInliner('-')
        facadeStep(::WasmPreSerializationLoweringFacade)
        loweredIrHandlersStep()

        configureLoweredIrHandlersStep {
            useHandlers(::IrDiagnosticsHandler)
        }
        facadeStep { FirWasmKlibSerializerFacade(it, true) }

        klibArtifactsHandlersStep {
            useHandlers(::KlibBackendDiagnosticsHandler)
        }

        forTestsMatching("compiler/testData/diagnostics/wasmTests/multiplatform/*") {
            defaultDirectives {
                LANGUAGE + "+MultiPlatformProjects"
            }
        }
    }
}

abstract class AbstractWasmJsDiagnosticTest : AbstractWasmDiagnosticTestBase(
    FirParser.LightTree,
    WasmPlatforms.wasmJs,
    WasmTarget.JS,
)

abstract class AbstractWasmJsDiagnosticWithIrInlinerTest : AbstractWasmJsDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(this)
        withIrInliner('+')
    }
}

abstract class AbstractWasmWasiDiagnosticTest : AbstractWasmDiagnosticTestBase(
    FirParser.LightTree,
    WasmPlatforms.wasmWasi,
    WasmTarget.WASI,
)

abstract class AbstractWasmWasiDiagnosticWithIrInlinerTestBase : AbstractWasmWasiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        withIrInliner('+')
    }
}

private fun TestConfigurationBuilder.withIrInliner(plusOrMinus: Char) {
    defaultDirectives {
        LANGUAGE with listOf(
            "$plusOrMinus${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
            "$plusOrMinus${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
        )
    }
}
