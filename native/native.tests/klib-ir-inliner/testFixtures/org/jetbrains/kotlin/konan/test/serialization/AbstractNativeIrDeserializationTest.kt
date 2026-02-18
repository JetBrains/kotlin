/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.serialization

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.NativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.configuration.commonConfigurationForNativeFirstStageUpToSerialization
import org.jetbrains.kotlin.konan.test.converters.NativeDeserializerFacade
import org.jetbrains.kotlin.konan.test.converters.NativePreSerializationLoweringFacade
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.RENDER_FIR_DECLARATION_ATTRIBUTES
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.IGNORE_IR_DESERIALIZATION_TEST
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator

// Base class for IR serialization/deserialization test, configured with FIR frontend, in Native-specific way.
open class AbstractNativeIrDeserializationTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.NATIVE) {
    val targetFrontend = FrontendKinds.FIR
    val parser = FirParser.LightTree
    val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade
    val frontendToIrConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrNativeResultsConverter
    open val irPreSerializationLoweringFacade: Constructor<IrPreSerializationLoweringFacade<IrBackendInput>>
        get() = ::NativePreSerializationLoweringFacade
    val serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::NativeKlibSerializerFacade
    val deserializerFacade: Constructor<DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>>
        get() = ::NativeDeserializerFacade

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        useConfigurators(::NativeEnvironmentConfigurator)
        useAfterAnalysisCheckers(::FirMetaInfoDiffSuppressor)
        commonConfigurationForNativeFirstStageUpToSerialization(
            targetFrontend,
            frontendFacade,
            frontendToIrConverter,
            irPreSerializationLoweringFacade,
            IGNORE_IR_DESERIALIZATION_TEST,
        )
        irHandlersStep { useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = false) }) }

        facadeStep(serializerFacade)
        klibArtifactsHandlersStep {
            useHandlers(::KlibAbiDumpHandler)
        }

        facadeStep(deserializerFacade)
        klibArtifactsHandlersStep {
            useHandlers(::KlibBackendDiagnosticsHandler)
        }
        deserializedIrHandlersStep { useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = true) }) }

        configureFirParser(parser)

        defaultDirectives {
            LANGUAGE with listOf(
                "-${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
            )
        }
        forTestsNotMatching(
            "compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*" or
                    "compiler/testData/diagnostics/*"
        ) {
            defaultDirectives {
                DIAGNOSTICS with "-warnings"
            }
        }

        enableMetaInfoHandler()

        configureIrHandlersStep {
            useHandlers(::IrMangledNameAndSignatureDumpHandler)
        }

        forTestsMatching("compiler/testData/codegen/box/evaluate/*") {
            enableMetaInfoHandler()
            defaultDirectives {
                +FIR_DUMP
                +RENDER_FIR_DECLARATION_ATTRIBUTES
            }
        }
    }
}
