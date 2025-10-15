/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.dump

import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.NativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.converters.NativeDeserializerFacade
import org.jetbrains.kotlin.konan.test.converters.NativePreSerializationLoweringFacade
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.FirMetadataLoadingTestSuppressor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator

abstract class AbstractNativeLoadCompiledKotlinTest :
    AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.NATIVE)
{
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        useConfigurators(
            ::NativeEnvironmentConfigurator,
        )
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = NativePlatforms.unspecifiedNativePlatform
            targetBackend = TargetBackend.NATIVE
            artifactKind = ArtifactKind.NoArtifact
            dependencyKind = DependencyKind.Binary
        }
        defaultDirectives {
            +WITH_STDLIB
        }

        configureFirParser(FirParser.LightTree)
        facadeStep(::FirFrontendFacade)
        firHandlersStep {
            useHandlers(::FirDiagnosticsHandler)
        }

        facadeStep(::Fir2IrNativeResultsConverter)
        facadeStep(::NativePreSerializationLoweringFacade)

        loweredIrHandlersStep {
            useHandlers(::IrDiagnosticsHandler)
            useHandlers(::NoIrCompilationErrorsHandler)
        }

        facadeStep(::NativeKlibSerializerFacade)
        facadeStep(::NativeDeserializerFacade)

        klibArtifactsHandlersStep {
            useHandlers(::KlibNativeLoadedMetadataDumpHandler)
        }
        useAfterAnalysisCheckers(
            { testServices -> FirMetadataLoadingTestSuppressor(testServices, CodegenTestDirectives.IGNORE_FIR_METADATA_LOADING_K2) }
        )
    }
}
