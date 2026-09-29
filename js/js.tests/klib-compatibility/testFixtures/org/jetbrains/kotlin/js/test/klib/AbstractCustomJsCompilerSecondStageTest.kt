/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.js.test.blackbox.JsGroupingTestIsolator
import org.jetbrains.kotlin.js.test.blackbox.JsTestHelpersModuleTransformer
import org.jetbrains.kotlin.js.test.handlers.JsGroupingStageBoxRunner
import org.jetbrains.kotlin.js.test.preprocessors.JsExportBoxPreprocessor
import org.jetbrains.kotlin.js.test.runners.commonConfigurationForJsTest
import org.jetbrains.kotlin.js.test.runners.setUpDefaultDirectivesForJsBoxTest
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TwoStageTestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.grouping.AbstractTwoStageKotlinCompilerJsTest
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageTestSuppressor
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestSuppressor
import org.jetbrains.kotlin.test.klib.setupCustomLVForKlibForwardCompatibilityTest
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.jetbrains.kotlin.test.services.configuration.UnsupportedFeaturesTestConfigurator
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.Tag
import java.io.File

/**
 * KLIB forward-compatibility test: the non-grouping (first) stage compiles every test into a KLIB with the current
 * compiler, and the grouping (second) stage links batches of such KLIBs into executables with a previously released
 * Kotlin/JS compiler invoked via CLI, and runs them.
 */
@Tag("custom-second-stage")
open class AbstractCustomJsCompilerSecondStageTest : AbstractTwoStageKotlinCompilerJsTest() {
    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return if (customJsCompilerSettings.defaultLanguageVersion >= LanguageVersion.LATEST_STABLE)
            super.createKotlinStandardLibrariesPathProvider()
        else
            object : KotlinStandardLibrariesPathProvider by StandardLibrariesPathProviderForKotlinProject {
                override fun fullJsStdlib(): File = customJsCompilerSettings.stdlib
                override fun defaultJsStdlib(): File = customJsCompilerSettings.stdlib
                override fun kotlinTestJsKLib(): File = customJsCompilerSettings.kotlinTest
            }
    }

    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        commonConfiguration {
            globalDefaults {
                targetBackend = TargetBackend.JS_IR
                frontend = FrontendKinds.FIR
                targetPlatform = JsPlatforms.defaultJsPlatform
                dependencyKind = DependencyKind.Binary
            }
            defaultDirectives {
                setupCustomLVForKlibForwardCompatibilityTest(customJsCompilerSettings.defaultLanguageVersion)

                // `js-ir-minimal-for-test` must not be used in this test at all, so need to use `kotlin-test` library via `WITH_STDLIB` directive
                // Note: attempt to use `js-ir-minimal-for-test` on 1st stage will cause unresolved symbol `assertEquals(0:0;0:0){0§<kotlin.Any?>}`
                // on 2nd stage, since this symbol is absent in `kotlin-test` library.
                +WITH_STDLIB
            }
            useMetaTestConfigurators(::UnsupportedFeaturesTestConfigurator)
        }
        nonGroupingStage {
            useGroupingTestIsolators(::JsGroupingTestIsolator)
            // KT-47200: TODO export `box()` by means of CLI configuration, and not using `JsExportBoxPreprocessor` hack.
            useSourcePreprocessor(::JsExportBoxPreprocessor)

            setUpDefaultDirectivesForJsBoxTest(FirParser.LightTree)

            commonConfigurationForJsTest()
            // A grouped batch links several per-test KLIBs at once, so the sources every test gets a copy of must live
            // in a single shared KLIB instead of being duplicated in every per-test KLIB.
            @OptIn(TestInfrastructureInternals::class)
            useModuleStructureTransformers(JsTestHelpersModuleTransformer)

            configureFirHandlersStep {
                commonFirHandlersForCodegenTest()
            }

            useFailureSuppressors(
                // Suppress all tests that failed on the first stage if they are anyway marked as "IGNORE_BACKEND*".
                ::CustomKlibCompilerTestSuppressor,
                // Suppress failed tests having `// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: X.Y.Z`,
                // where `X.Y.Z` matches to `customJsCompilerSettings.version`
                ::CustomKlibCompilerSecondStageTestSuppressor.bind(customJsCompilerSettings.defaultLanguageVersion),
            )
        }
        groupingStage {
            facadeStep(::CustomJsCompilerSecondStageGroupingFacade)
            handlersStep(ArtifactKinds.Js, CompilationStage.SECOND) {
                useHandlers(::JsGroupingStageBoxRunner)
            }
        }
    }
}
