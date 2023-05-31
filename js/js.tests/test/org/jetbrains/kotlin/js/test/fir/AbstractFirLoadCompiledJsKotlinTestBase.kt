/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.js.test.converters.FirJsKlibBackendFacade
import org.jetbrains.kotlin.js.test.ir.commonConfigurationForJsCodegenTest
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureKlibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_PARSER
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest

abstract class AbstractFirLoadCompiledJsKotlinTestBase<F : ResultingArtifact.FrontendOutput<F>> :
    AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JS_IR)
{
    protected abstract val frontendKind: FrontendKind<F>
    protected abstract val frontendFacade: Constructor<FrontendFacade<F>>
    protected abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<F, IrBackendInput>>

    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForJsCodegenTest(
            frontendKind,
            frontendFacade,
            frontendToBackendConverter,
            ::FirJsKlibBackendFacade,
        )

        configureKlibArtifactsHandlersStep {
            useHandlers(::KlibLoadedMetadataDumpHandler)
        }

        useAfterAnalysisCheckers(::FirMetadataLoadingTestSuppressor)
    }
}

open class AbstractFirLoadK2CompiledJsKotlinTest : AbstractFirLoadCompiledJsKotlinTestBase<FirOutputArtifact>() {
    override val frontendKind: FrontendKind<FirOutputArtifact>
        get() = FrontendKinds.FIR
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade
    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrResultsConverter

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            FIR_PARSER with FirParser.LightTree
        }
    }
}
