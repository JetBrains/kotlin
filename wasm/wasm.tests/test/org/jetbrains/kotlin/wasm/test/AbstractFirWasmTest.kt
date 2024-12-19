/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgConsistencyHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolvedTypesVerifier
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.codegen.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfiguratorJs
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfiguratorWasi
import org.jetbrains.kotlin.wasm.test.converters.FirWasmKlibSerializerFacade
import org.jetbrains.kotlin.wasm.test.converters.WasmBackendFacade
import org.jetbrains.kotlin.wasm.test.handlers.WasiBoxRunner
import org.jetbrains.kotlin.wasm.test.handlers.WasmBoxRunner
import org.jetbrains.kotlin.wasm.test.handlers.WasmDebugRunner
import org.jetbrains.kotlin.wasm.test.providers.WasmJsSteppingTestAdditionalSourceProvider

abstract class AbstractFirWasmTest(
    targetPlatform: TargetPlatform,
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) : AbstractWasmBlackBoxCodegenTestBase<FirOutputArtifact, IrBackendInput, BinaryArtifacts.KLib>(
    FrontendKinds.FIR, TargetBackend.WASM, targetPlatform, pathToTestDir, testGroupOutputDirPrefix
) {
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrResultsConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::FirWasmKlibSerializerFacade

    override val afterBackendFacade: Constructor<AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Wasm>>
        get() = ::WasmBackendFacade

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
                DiagnosticsDirectives.DIAGNOSTICS with listOf("-infos")
                FirDiagnosticsDirectives.FIR_PARSER with FirParser.Psi
            }

            firHandlersStep {
                useHandlers(
                    ::FirDumpHandler,
                    ::FirCfgDumpHandler,
                    ::FirCfgConsistencyHandler,
                    ::FirResolvedTypesVerifier,
                )
            }
        }
    }
}

open class AbstractFirWasmJsTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) : AbstractFirWasmTest(WasmPlatforms.wasmJs, pathToTestDir, testGroupOutputDirPrefix) {
    override val wasmBoxTestRunner: Constructor<AnalysisHandler<BinaryArtifacts.Wasm>>
        get() = ::WasmBoxRunner

    override val wasmEnvironmentConfigurator: Constructor<EnvironmentConfigurator>
        get() = ::WasmEnvironmentConfiguratorJs
}


open class AbstractFirWasmJsCodegenBoxTest : AbstractFirWasmJsTest(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = "codegen/firBox/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }

        builder.useAfterAnalysisCheckers(
            ::FirMetaInfoDiffSuppressor
        )
    }
}

open class AbstractFirWasmJsCodegenBoxInlineTest : AbstractFirWasmJsTest(
    "compiler/testData/codegen/boxInline/",
    "codegen/firBoxInline/"
)

open class AbstractFirWasmJsCodegenInteropTest : AbstractFirWasmJsTest(
    "compiler/testData/codegen/wasmJsInterop",
    "codegen/firWasmJsInterop"
)

open class AbstractFirWasmJsTranslatorTest : AbstractFirWasmJsTest(
    "js/js.translator/testData/box/",
    "js.translator/firBox"
)

open class AbstractFirWasmJsSteppingTest : AbstractFirWasmJsTest(
    "compiler/testData/debug/stepping/",
    "debug/stepping/firBox"
) {
    override val wasmBoxTestRunner: Constructor<AnalysisHandler<BinaryArtifacts.Wasm>>
        get() = ::WasmDebugRunner

    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForWasmBlackBoxCodegenTest()
        useAdditionalSourceProviders(::WasmJsSteppingTestAdditionalSourceProvider)
        defaultDirectives {
            +WasmEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP
            +WasmEnvironmentConfigurationDirectives.SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES
        }
    }
}

open class AbstractFirWasmWasiTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) : AbstractFirWasmTest(WasmPlatforms.wasmWasi, pathToTestDir, testGroupOutputDirPrefix) {
    override val wasmBoxTestRunner: Constructor<AnalysisHandler<BinaryArtifacts.Wasm>>
        get() = ::WasiBoxRunner

    override val wasmEnvironmentConfigurator: Constructor<EnvironmentConfigurator>
        get() = ::WasmEnvironmentConfiguratorWasi

    override val additionalSourceProvider: Constructor<AdditionalSourceProvider>?
        get() = ::WasmWasiBoxTestHelperSourceProvider
}

open class AbstractFirWasmWasiCodegenBoxTest : AbstractFirWasmWasiTest(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = "codegen/firWasiBox/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }

        builder.useAfterAnalysisCheckers(
            ::FirMetaInfoDiffSuppressor
        )
    }
}