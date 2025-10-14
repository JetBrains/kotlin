/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.FirMetadataLoadingTestSuppressor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.KlibWasmJsLoadedMetadataDumpHandler
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureKlibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_PARSER
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.wasm.test.converters.FirWasmKlibSerializerFacade

abstract class AbstractWasmJsLoadCompiledKotlinTest :
    AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.WASM_JS)
{
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForWasmFirstStageTest(
            targetFrontend = FrontendKinds.FIR,
            targetPlatform = WasmPlatforms.wasmJs,
            wasmTarget = WasmTarget.JS,
            pathToTestDir = "compiler/testData/codegen/box/",
            testGroupOutputDirPrefix = "codegen/loadCompiledWasm/",
            frontendFacade = ::FirFrontendFacade,
            frontendToBackendConverter = ::Fir2IrResultsConverter,
            backendFacade = ::FirWasmKlibSerializerFacade,
            additionalSourceProvider = null,
            customIgnoreDirective = null,
            additionalIgnoreDirectives = null,
        )
        builder.defaultDirectives {
            FIR_PARSER with FirParser.LightTree
        }

        configureKlibArtifactsHandlersStep {
            useHandlers(::KlibWasmJsLoadedMetadataDumpHandler)
        }

        useAfterAnalysisCheckers(
            { testServices -> FirMetadataLoadingTestSuppressor(testServices, CodegenTestDirectives.IGNORE_FIR_METADATA_LOADING_K2) }
        )

    }
}
