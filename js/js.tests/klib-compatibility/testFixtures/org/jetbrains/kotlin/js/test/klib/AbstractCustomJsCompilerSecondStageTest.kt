/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.js.test.preprocessors.JsExportBoxPreprocessor
import org.jetbrains.kotlin.js.test.runners.commonConfigurationForJsTest
import org.jetbrains.kotlin.js.test.runners.configureJsBoxHandlers
import org.jetbrains.kotlin.js.test.runners.setUpDefaultDirectivesForJsBoxTest
import org.jetbrains.kotlin.test.services.configuration.UnsupportedFeaturesTestConfigurator
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.jsArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_MULTIPLE_API_VERSIONS_SETTING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.API_VERSION
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE_VERSION
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageTestSuppressor
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestSuppressor
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("custom-second-stage")
open class AbstractCustomJsCompilerSecondStageTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JS_IR) {
    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return if (customJsCompilerSettings.defaultLanguageVersion >= LanguageVersion.LATEST_STABLE)
            createKotlinStandardLibrariesPathProvider()
        else
            object : KotlinStandardLibrariesPathProvider by StandardLibrariesPathProviderForKotlinProject {
                override fun fullJsStdlib(): File = customJsCompilerSettings.stdlib
                override fun defaultJsStdlib(): File = customJsCompilerSettings.stdlib
                override fun kotlinTestJsKLib(): File = customJsCompilerSettings.kotlinTest
            }
    }

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        // KT-47200: TODO export `box()` by means of CLI configuration, and not using `JsExportBoxPreprocessor` hack.
        useSourcePreprocessor(::JsExportBoxPreprocessor)
        useMetaTestConfigurators(::UnsupportedFeaturesTestConfigurator)
        defaultDirectives {
            if (customJsCompilerSettings.defaultLanguageVersion < LanguageVersion.LATEST_STABLE) {
                +ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
                LANGUAGE_VERSION with customJsCompilerSettings.defaultLanguageVersion
                +ALLOW_MULTIPLE_API_VERSIONS_SETTING
                API_VERSION with ApiVersion.createByLanguageVersion(customJsCompilerSettings.defaultLanguageVersion)
                LANGUAGE with "+ExportKlibToOlderAbiVersion"
            }
            // `js-ir-minimal-for-test` must not be used in this test at all, so need to use `kotlin-test` library via `WITH_STDLIB` directive
            // Note: attempt to use `js-ir-minimal-for-test` on 1st stage will cause unresolved symbol `assertEquals(0:0;0:0){0§<kotlin.Any?>}`
            // on 2nd stage, since this symbol is absent in `kotlin-test` library.
            +WITH_STDLIB
        }

        setUpDefaultDirectivesForJsBoxTest(FirParser.LightTree)

        commonConfigurationForJsTest()

        configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }

        facadeStep(::CustomJsCompilerSecondStageFacade)

        jsArtifactsHandlersStep()
        configureJsBoxHandlers()

        useAfterAnalysisCheckers(
            // Suppress all tests that failed on the first stage if they are anyway marked as "IGNORE_BACKEND*".
            ::CustomKlibCompilerTestSuppressor,
            // Suppress failed tests having `// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: X.Y.Z`,
            // where `X.Y.Z` matches to `customJsCompilerSettings.version`
            ::CustomKlibCompilerSecondStageTestSuppressor.bind(customJsCompilerSettings.defaultLanguageVersion),
        )
    }
}
