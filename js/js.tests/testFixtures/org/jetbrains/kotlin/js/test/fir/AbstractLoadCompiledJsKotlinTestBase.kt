/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWebFacade
import org.jetbrains.kotlin.js.test.ir.commonConfigurationForJsTest
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureKlibArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_PARSER
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest

abstract class AbstractLoadCompiledJsKotlinTestBase<F : ResultingArtifact.FrontendOutput<F>> :
    AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JS_IR)
{
    protected abstract val frontendKind: FrontendKind<F>
    protected abstract val frontendFacade: Constructor<FrontendFacade<F>>
    protected abstract val frontendToIrConverter: Constructor<Frontend2BackendConverter<F, IrBackendInput>>

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForJsTest(
            targetFrontend = frontendKind,
            frontendFacade = frontendFacade,
            frontendToIrConverter = frontendToIrConverter,
            serializerFacade = ::FirKlibSerializerCliWebFacade,
        )

        configureKlibArtifactsHandlersStep {
            useHandlers(::KlibJsLoadedMetadataDumpHandler)
        }

        useAfterAnalysisCheckers(
            { testServices -> FirMetadataLoadingTestSuppressor(testServices, CodegenTestDirectives.IGNORE_FIR_METADATA_LOADING_K2) }
        )
    }
}

open class AbstractLoadK2CompiledJsKotlinTest : AbstractLoadCompiledJsKotlinTestBase<FirOutputArtifact>() {
    override val frontendKind: FrontendKind<FirOutputArtifact>
        get() = FrontendKinds.FIR
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirCliWebFacade
    override val frontendToIrConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrCliWebFacade

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            FIR_PARSER with FirParser.LightTree
        }
    }
}
