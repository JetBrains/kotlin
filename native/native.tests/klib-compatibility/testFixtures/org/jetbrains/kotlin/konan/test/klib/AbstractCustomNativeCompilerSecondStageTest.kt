/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.services.configuration.UnsupportedFeaturesTestConfigurator
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.NativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.configuration.commonConfigurationForNativeFirstStage
import org.jetbrains.kotlin.konan.test.converters.NativePreSerializationLoweringFacade
import org.jetbrains.kotlin.konan.test.handlers.NativeRunner
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.nativeArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_MULTIPLE_API_VERSIONS_SETTING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.API_VERSION
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE_VERSION
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageTestSuppressor
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestSuppressor
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.Tag

@Tag("custom-second-stage")
open class AbstractCustomNativeCompilerSecondStageTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.NATIVE) {
    val targetFrontend = FrontendKinds.FIR
    val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade
    val frontendToIrConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrNativeResultsConverter
    open val irPreSerializationLoweringFacade: Constructor<IrPreSerializationLoweringFacade<IrBackendInput>>
        get() = ::NativePreSerializationLoweringFacade
    val serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::NativeKlibSerializerFacade

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        useMetaTestConfigurators(::UnsupportedFeaturesTestConfigurator)
        defaultDirectives {
            FirDiagnosticsDirectives.FIR_PARSER with FirParser.LightTree
            if (customNativeCompilerSettings.defaultLanguageVersion < LanguageVersion.LATEST_STABLE) {
                +ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
                LANGUAGE_VERSION with customNativeCompilerSettings.defaultLanguageVersion
                +ALLOW_MULTIPLE_API_VERSIONS_SETTING
                API_VERSION with ApiVersion.createByLanguageVersion(customNativeCompilerSettings.defaultLanguageVersion)
                LANGUAGE with "+ExportKlibToOlderAbiVersion"
            }
        }

        useConfigurators(::CustomNativeCompilerSecondStageEnvironmentConfigurator.bind(customNativeCompilerSettings))
        useAdditionalSourceProviders(
            ::NativeLauncherAdditionalSourceProvider,
            ::CoroutineHelpersSourceFilesProvider,
            ::AdditionalDiagnosticsSourceFilesProvider,
        )
        commonConfigurationForNativeFirstStage(
            targetFrontend,
            frontendFacade,
            frontendToIrConverter,
            irPreSerializationLoweringFacade,
            serializerFacade,
        )
        configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }

        facadeStep(::NativeCompilerSecondStageFacade.bind(customNativeCompilerSettings))

        nativeArtifactsHandlersStep {
            useHandlers(::NativeRunner)
        }

        useAfterAnalysisCheckers(
            // Suppress all tests that failed on the first stage if they are anyway marked as "IGNORE_BACKEND*".
            ::CustomKlibCompilerTestSuppressor,
            // Suppress failed tests having `// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: X.Y.Z`,
            // where `X.Y.Z` matches to `customNativeCompilerSettings.version`
            ::CustomKlibCompilerSecondStageTestSuppressor.bind(customNativeCompilerSettings.version),
        )
        forTestsMatching("compiler/testData/codegen/box/properties/backingField/*") {
            defaultDirectives {
                LANGUAGE with "+ExplicitBackingFields"
            }
        }
    }
}
