/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.js.test.klib.customWasmJsCompilerSettings
import org.jetbrains.kotlin.js.test.klib.defaultLanguageVersion
import org.jetbrains.kotlin.js.test.preprocessors.JsExportBoxPreprocessor
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.services.configuration.UnsupportedFeaturesTestConfigurator
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.wasmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_MULTIPLE_API_VERSIONS_SETTING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.API_VERSION
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE_VERSION
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageTestSuppressor
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestSuppressor
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.jetbrains.kotlin.test.services.configuration.WasmSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlin.wasm.test.blackbox.CustomWasmJsCompilerSecondStageFacade
import org.jetbrains.kotlin.wasm.test.commonConfigurationForWasmFirstStageTest
import org.jetbrains.kotlin.wasm.test.commonConfigurationForWasmSecondStageTest
import org.jetbrains.kotlin.wasm.test.converters.FirWasmKlibSerializerFacade
import org.jetbrains.kotlin.wasm.test.handlers.WasmFolderBoxRunner
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("custom-second-stage")
open class AbstractCustomWasmJsCompilerSecondStageTest(val testDataRoot: String = "compiler/testData/codegen/") :
    AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.WASM_JS)
{
    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return if (customWasmJsCompilerSettings.defaultLanguageVersion >= LanguageVersion.LATEST_STABLE)
            super.createKotlinStandardLibrariesPathProvider()
        else
            object : KotlinStandardLibrariesPathProvider by StandardLibrariesPathProviderForKotlinProject {
                override fun fullWasmStdlib(target: WasmTarget): File {
                    require(target == WasmTarget.JS)
                    return customWasmJsCompilerSettings.stdlib
                }

                override fun kotlinTestWasmKLib(target: WasmTarget): File {
                    require(target == WasmTarget.JS)
                    return customWasmJsCompilerSettings.kotlinTest
                }
            }
    }

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        useSourcePreprocessor(::JsExportBoxPreprocessor)
        useMetaTestConfigurators(::UnsupportedFeaturesTestConfigurator)
        useDirectives(WasmEnvironmentConfigurationDirectives)
        defaultDirectives {
            if (customWasmJsCompilerSettings.defaultLanguageVersion < LanguageVersion.LATEST_STABLE) {
                +ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
                LANGUAGE_VERSION with customWasmJsCompilerSettings.defaultLanguageVersion
                +ALLOW_MULTIPLE_API_VERSIONS_SETTING
                API_VERSION with ApiVersion.createByLanguageVersion(customWasmJsCompilerSettings.defaultLanguageVersion)
                LANGUAGE with "+ExportKlibToOlderAbiVersion"
            }
//            +WITH_STDLIB
        }

        defaultDirectives {
            +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
            DiagnosticsDirectives.DIAGNOSTICS with listOf("-infos")
            FirDiagnosticsDirectives.FIR_PARSER with FirParser.LightTree
        }

        commonConfigurationForWasmFirstStageTest(
            FrontendKinds.FIR,
            WasmPlatforms.wasmJs,
            WasmTarget.JS,
            ::FirFrontendFacade,
            ::Fir2IrResultsConverter,
            ::FirWasmKlibSerializerFacade,
            additionalSourceProvider = null,
            customIgnoreDirective = null,
            additionalIgnoreDirectives = null,
        )

        configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }
        commonConfigurationForWasmSecondStageTest(
            testDataRoot,
            testGroupOutputDirPrefix = this@AbstractCustomWasmJsCompilerSecondStageTest::class.java.simpleName +
                    customWasmJsCompilerSettings.defaultLanguageVersion,
        )
        useConfigurators(
            ::WasmSecondStageEnvironmentConfigurator.bind(WasmTarget.JS),
        )

        facadeStep(::CustomWasmJsCompilerSecondStageFacade)

        wasmArtifactsHandlersStep {
            useHandlers(::WasmFolderBoxRunner)
        }

        useAfterAnalysisCheckers(
            // Suppress all tests that failed on the first stage if they are anyway marked as "IGNORE_BACKEND*".
            ::CustomKlibCompilerTestSuppressor,
            // Suppress failed tests having `// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: X.Y.Z`,
            // where `X.Y.Z` matches to `customJsCompilerSettings.version`
            ::CustomKlibCompilerSecondStageTestSuppressor.bind(customWasmJsCompilerSettings.defaultLanguageVersion),
        )
    }
}
