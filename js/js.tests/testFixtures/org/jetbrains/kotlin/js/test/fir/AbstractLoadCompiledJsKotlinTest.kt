/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWebFacade
import org.jetbrains.kotlin.js.test.ir.commonConfigurationForJsTest
import org.jetbrains.kotlin.test.FirMetadataLoadingTestSuppressor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.KlibJsLoadedMetadataDumpHandler
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureKlibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_PARSER
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.utils.bind

abstract class AbstractLoadCompiledJsKotlinTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JS_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForJsTest(
            targetFrontend = FrontendKinds.FIR,
            frontendFacade = ::FirCliWebFacade,
            frontendToIrConverter = ::Fir2IrCliWebFacade,
            serializerFacade = ::FirKlibSerializerCliWebFacade,
        )

        configureKlibArtifactsHandlersStep {
            useHandlers(::KlibJsLoadedMetadataDumpHandler)
        }

        useAfterAnalysisCheckers(
            ::FirMetadataLoadingTestSuppressor.bind(CodegenTestDirectives.IGNORE_FIR_METADATA_LOADING_K2),
        )

        defaultDirectives {
            FIR_PARSER with FirParser.LightTree
        }
    }
}
