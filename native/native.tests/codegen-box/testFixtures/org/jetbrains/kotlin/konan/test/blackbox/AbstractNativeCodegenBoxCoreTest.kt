/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.NativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.configuration.commonConfigurationForNativeFirstStageUpToSerialization
import org.jetbrains.kotlin.konan.test.converters.NativePreSerializationLoweringFacade
import org.jetbrains.kotlin.konan.test.handlers.NativeRunner
import org.jetbrains.kotlin.konan.test.klib.NativeCompilerSecondStageFacade
import org.jetbrains.kotlin.konan.test.klib.NativeLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.konan.test.klib.currentCustomNativeCompilerSettings
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.FirInterpreterDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.NativeKlibInterpreterDumpHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind

abstract class AbstractNativeCodegenBoxCoreTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.NATIVE) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        configureFirParser(FirParser.LightTree)
        useConfigurators(::NativeEnvironmentConfigurator)
        enableMetaInfoHandler()
        useAfterAnalysisCheckers(::FirMetaInfoDiffSuppressor)

        // 1st stage (sources -> klibs)
        useAdditionalSourceProviders(
            ::NativeLauncherAdditionalSourceProvider,
        )
        commonConfigurationForNativeFirstStageUpToSerialization(
            FrontendKinds.FIR,
            ::FirFrontendFacade,
            ::Fir2IrNativeResultsConverter,
            ::NativePreSerializationLoweringFacade,
            IGNORE_BACKEND_K2
        )
        facadeStep(::NativeKlibSerializerFacade)

        // 2nd stage (klibs -> executable)
        facadeStep(::NativeCompilerSecondStageFacade.bind(currentCustomNativeCompilerSettings))
        nativeArtifactsHandlersStep {
            useHandlers(::NativeRunner)
        }

        defaultDirectives {
            LANGUAGE with listOf(
                "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
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

        forTestsMatching("compiler/testData/codegen/box/involvesIrInterpreter/*") {
            configureFirHandlersStep {
                useHandlers(::FirInterpreterDumpHandler)
            }
            configureKlibArtifactsHandlersStep {
                useHandlers(::NativeKlibInterpreterDumpHandler)
            }
        }
    }
}
