/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.serialization

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.test.KlibSerializerNativeCliFacade
import org.jetbrains.kotlin.konan.test.configuration.commonConfigurationForNativeFirstStageUpToSerialization
import org.jetbrains.kotlin.konan.test.converters.NativeDeserializerFacade
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.KlibBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.handlers.SerializedIrDumpHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.deserializedIrHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.builders.loweredIrHandlersStep
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.IGNORE_IR_DESERIALIZATION_TEST
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator

// Base class for IR serialization/deserialization test, configured with FIR frontend, in Native-specific way.
open class AbstractNativeIrDeserializationTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.NATIVE) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        useConfigurators(::CommonEnvironmentConfigurator, ::NativeFirstStageEnvironmentConfigurator)
        useFailureSuppressors(::FirMetaInfoDiffSuppressor)
        commonConfigurationForNativeFirstStageUpToSerialization(
            customIgnoreDirective = IGNORE_IR_DESERIALIZATION_TEST,
        )
        loweredIrHandlersStep { useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = false) }) }

        facadeStep(::KlibSerializerNativeCliFacade)
        klibArtifactsHandlersStep {
            useHandlers(::KlibAbiDumpHandler)
        }

        facadeStep(::NativeDeserializerFacade)
        klibArtifactsHandlersStep {
            useHandlers(::KlibBackendDiagnosticsHandler)
        }
        deserializedIrHandlersStep { useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = true) }) }

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
    }
}
