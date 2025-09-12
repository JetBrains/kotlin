/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.diagnostics

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.loweredIrHandlersStep
import org.jetbrains.kotlin.test.configuration.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlin.wasm.test.converters.WasmPreSerializationLoweringFacade

abstract class AbstractFirWasmDiagnosticTestBase(
    val parser: FirParser,
    private val targetPlatform: TargetPlatform,
    private val wasmTarget: WasmTarget,
) : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
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
    WasmTarget.JS,
)

abstract class AbstractDiagnosticsFirWasmWasiTest : AbstractFirWasmDiagnosticTestBase(
    FirParser.Psi,
    WasmPlatforms.wasmWasi,
    WasmTarget.WASI,
)

abstract class AbstractDiagnosticsWasmJsWithIrInlinerTestBase : AbstractDiagnosticsFirWasmTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        commonConfigurationForWasmDiagnosticTestAfterFir2IR()
    }
}

abstract class AbstractDiagnosticsWasmWasiWithIrInlinerTestBase : AbstractDiagnosticsFirWasmWasiTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        commonConfigurationForWasmDiagnosticTestAfterFir2IR()
    }
}

fun <FO : ResultingArtifact.FrontendOutput<FO>> TestConfigurationBuilder.commonConfigurationForWasmDiagnosticTestAfterFir2IR() {
    defaultDirectives {
        LANGUAGE with listOf(
            "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
            "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
        )
    }

    facadeStep(::Fir2IrResultsConverter) // TODO Change for ::Fir2IrCliWebFacade in scope of KT-74671
    irHandlersStep {
        useHandlers(::IrDiagnosticsHandler)
    }

    facadeStep(::WasmPreSerializationLoweringFacade)
    loweredIrHandlersStep {
        useHandlers(::IrDiagnosticsHandler)
    }
}
