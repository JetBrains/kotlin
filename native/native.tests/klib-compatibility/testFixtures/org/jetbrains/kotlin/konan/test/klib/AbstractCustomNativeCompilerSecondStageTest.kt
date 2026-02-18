/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.NativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeCoreTest
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.configuration.commonConfigurationForNativeFirstStageUpToSerialization
import org.jetbrains.kotlin.konan.test.converters.NativePreSerializationLoweringFacade
import org.jetbrains.kotlin.konan.test.handlers.NativeBoxRunner
import org.jetbrains.kotlin.konan.test.services.sourceProviders.NativeLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.builders.nativeArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.RENDER_FIR_DECLARATION_ATTRIBUTES
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_MULTIPLE_API_VERSIONS_SETTING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.API_VERSION
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE_VERSION
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageTestSuppressor
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestSuppressor
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.TargetBackendTestSkipper
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.UnsupportedFeaturesTestConfigurator
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.Tag

@Tag("custom-second-stage")
open class AbstractCustomNativeCompilerSecondStageTest : AbstractNativeCoreTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        useMetaTestConfigurators(
            ::UnsupportedFeaturesTestConfigurator,
            ::TargetBackendTestSkipper,
        )
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

        val customNativeHome = customNativeCompilerSettings.nativeHome.absoluteFile.takeIf {
            customNativeCompilerSettings.defaultLanguageVersion < LanguageVersion.LATEST_STABLE
        }
        useConfigurators(::NativeEnvironmentConfigurator.bind(customNativeHome))
        useAdditionalSourceProviders(
            ::NativeLauncherAdditionalSourceProvider,
        )
        commonConfigurationForNativeFirstStageUpToSerialization(
            FrontendKinds.FIR,
            ::FirFrontendFacade,
            ::Fir2IrNativeResultsConverter,
            ::NativePreSerializationLoweringFacade,
        )
        facadeStep(::NativeKlibSerializerFacade)
        klibArtifactsHandlersStep {
            useHandlers(::KlibAbiDumpHandler)
        }
        configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }

        useDirectives(TestDirectives)
        facadeStep(::NativeCompilerSecondStageFacade.bind(customNativeCompilerSettings))

        nativeArtifactsHandlersStep {
            useHandlers(::NativeBoxRunner)
        }

        useAfterAnalysisCheckers(
            // Suppress all tests that failed on the first stage if they are anyway marked as "IGNORE_BACKEND*".
            ::CustomKlibCompilerTestSuppressor,
            // Suppress failed tests having `// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: X.Y.Z`,
            // where `X.Y.Z` matches to `customNativeCompilerSettings.version`
            ::CustomKlibCompilerSecondStageTestSuppressor.bind(customNativeCompilerSettings.defaultLanguageVersion),
        )
        forTestsMatching("compiler/testData/codegen/box/properties/backingField/*") {
            defaultDirectives {
                LANGUAGE with "+ExplicitBackingFields"
            }
        }
        forTestsMatching("compiler/testData/codegen/box/evaluate/*") {
            defaultDirectives {
                +FIR_DUMP
                +RENDER_FIR_DECLARATION_ATTRIBUTES
            }
        }
    }
}
