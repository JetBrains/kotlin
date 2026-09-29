/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.js.test.JsAdditionalSourceProvider
import org.jetbrains.kotlin.js.test.blackbox.JsGroupingTestIsolator
import org.jetbrains.kotlin.js.test.blackbox.JsTestHelpersModuleTransformer
import org.jetbrains.kotlin.js.test.converters.JsInProcessSecondStageFacade
import org.jetbrains.kotlin.js.test.handlers.JsGroupingStageBoxRunner
import org.jetbrains.kotlin.js.test.runners.setUpDefaultDirectivesForJsBoxTest
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TwoStageTestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.grouping.AbstractTwoStageKotlinCompilerJsTest
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerFirstStageTestSuppressor
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestSuppressor
import org.jetbrains.kotlin.test.klib.setupCustomLanguageVersionForKlibCompatibilityTest
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.UnsupportedFeaturesTestConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.Tag

/**
 * KLIB backward-compatibility test: the non-grouping (first) stage compiles every test into a KLIB with a previously
 * released Kotlin/JS compiler invoked via CLI, and the grouping (second) stage links batches of such KLIBs into
 * executables with the current in-process compiler and runs them.
 */
@Tag("custom-first-stage")
open class AbstractCustomJsCompilerFirstStageTest(val testDataRoot: String = "compiler/testData/codegen/") :
    AbstractTwoStageKotlinCompilerJsTest() {

    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        commonConfiguration {
            globalDefaults {
                targetBackend = TargetBackend.JS_IR
                // Note: Need to specify the concrete FE kind because this affects the choice of IGNORE_BACKEND_* directive.
                frontend = FrontendKinds.FIR
                targetPlatform = JsPlatforms.defaultJsPlatform
                dependencyKind = DependencyKind.Binary
            }
            defaultDirectives {
                // We need to set the custom LV to let `UnsupportedFeaturesTestConfigurator` skip tests with
                // the language features that are not supported in the given custom LV.
                setupCustomLanguageVersionForKlibCompatibilityTest(customJsCompilerSettings.defaultLanguageVersion)

                // `js-ir-minimal-for-test` must not be used in this test at all, so need to use `kotlin-test` library on 2nd stage via `WITH_STDLIB` directive
                // Note: on 1st stage, compilation is done against not `js-ir-minimal-for-test` (it's not bundled to stdlib Maven artifact),
                // but against `kotlin-test` library, which has assert functions of different signatures.
                // So, an attempt to use `js-ir-minimal-for-test` on 2nd stage will cause unresolved symbols like `kotlin.test/assertEquals|assertEquals(0:0;0:0;kotlin.String?){0§<kotlin.Any?>}[0]`
                +WITH_STDLIB
            }

            useMetaTestConfigurators(::UnsupportedFeaturesTestConfigurator)
            useConfigurators(
                ::CommonEnvironmentConfigurator,
                ::JsFirstStageEnvironmentConfigurator,
                // And this configurator is necessary to relax the second compilation stage, since the old compiler could produce IR
                // which would not pass new improved IR validation rules
                ::CustomJsCompilerSecondStageEnvironmentConfigurator,
            )
        }
        nonGroupingStage {
            useGroupingTestIsolators(::JsGroupingTestIsolator)
            useAdditionalSourceProviders(
                ::CoroutineHelpersSourceFilesProvider,
                ::JsAdditionalSourceProvider,
                ::AdditionalDiagnosticsSourceFilesProvider,
            )
            // A grouped batch links several per-test KLIBs at once, so the sources every test gets a copy of must live
            // in a single shared KLIB instead of being duplicated in every per-test KLIB.
            @OptIn(TestInfrastructureInternals::class)
            useModuleStructureTransformers(JsTestHelpersModuleTransformer)

            facadeStep(::CustomWebCompilerFirstStageFacade)

            defaultDirectives {
                JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR with
                        (System.getProperty("kotlin.js.test.root.out.dir") ?: testInfraError("'kotlin.js.test.root.out.dir' is not set"))
                JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with testDataRoot
                JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with
                        this@AbstractCustomJsCompilerFirstStageTest::class.java.simpleName + customJsCompilerSettings.defaultLanguageVersion
                LANGUAGE with "+JsAllowValueClassesInExternals"
            }
            setUpDefaultDirectivesForJsBoxTest(parser = /* Does not matter */ FirParser.LightTree)

            useFailureSuppressors(
                // Suppress all tests that have not been successfully compiled by the first stage.
                // And the limited number of tests where KLIBs generated by a specific compiler version
                // are known to have some problems that cause crash on the second stage.
                ::CustomKlibCompilerFirstStageTestSuppressor.bind(customJsCompilerSettings.defaultLanguageVersion),

                // Suppress all tests that failed on the second stage if they are anyway marked as "IGNORE_BACKEND*".
                ::CustomKlibCompilerTestSuppressor,
            )
        }
        groupingStage {
            facadeStep(JsInProcessSecondStageFacade::Grouping)
            handlersStep(ArtifactKinds.Js, CompilationStage.SECOND) {
                useHandlers(::JsGroupingStageBoxRunner)
            }
        }
    }
}
