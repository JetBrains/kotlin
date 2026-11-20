/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.js.test.JsAdditionalSourceProvider
import org.jetbrains.kotlin.js.test.handlers.JsArtifactsDumpHandler
import org.jetbrains.kotlin.js.test.handlers.JsBoxRunner
import org.jetbrains.kotlin.js.test.handlers.JsSourceMapPathRewriter
import org.jetbrains.kotlin.js.test.handlers.JsSourceMapValidator
import org.jetbrains.kotlin.js.test.handlers.JsTypeScriptCompilationHandler
import org.jetbrains.kotlin.js.test.handlers.NodeJsGeneratorHandler
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
 * KLIB backward-compatibility test:
 * - the non-grouping (first) stage compiles every test into a KLIB with a previously released Kotlin/JS compiler invoked via CLI,
 * - and the grouping (second) stage links batches of such KLIBs into executables with the current in-process compiler
 * - and runs executables.
 */
@Tag("custom-first-stage")
open class AbstractCustomJsCompilerFirstStageTest(val testDataRoot: String = "compiler/testData/codegen/") :
    AbstractTwoStageKotlinCompilerJsTest() {

    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        commonConfiguration {
            globalDefaults {
                targetBackend = TargetBackend.JS_IR
                frontend = FrontendKinds.FIR
                targetPlatform = JsPlatforms.defaultJsPlatform
                dependencyKind = DependencyKind.Binary
            }
            defaultDirectives {
                setupCustomLanguageVersionForKlibCompatibilityTest(customJsCompilerSettings.defaultLanguageVersion)
                +WITH_STDLIB
            }

            useMetaTestConfigurators(::UnsupportedFeaturesTestConfigurator)
            useConfigurators(
                ::CommonEnvironmentConfigurator,
                ::JsFirstStageEnvironmentConfigurator,
                ::CustomJsCompilerSecondStageEnvironmentConfigurator,
            )
        }
        nonGroupingStage {
            useGroupingTestIsolators({ JsGroupingTestIsolator(it, customCompilerStage = CompilationStage.FIRST) })
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
                +JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
                LANGUAGE with "+JsAllowValueClassesInExternals"
            }
            setUpDefaultDirectivesForJsBoxTest(parser = /* Does not matter */ FirParser.LightTree)

            useFailureSuppressors(
                ::CustomKlibCompilerFirstStageTestSuppressor.bind(customJsCompilerSettings.defaultLanguageVersion),
                ::CustomKlibCompilerTestSuppressor,

                // Point the JS stack traces of an isolated test at its dumped artifacts (see `JsGroupingStageArtifactsDumper`).
                // The failure of a test of a grouped batch carries no `ScriptExecutionException`, so its paths stay as they are.
                JsArtifactsDumpHandler::Suppressor,
            )
        }
        groupingStage {
            facadeStep(JsInProcessSecondStageFacade::Grouping)
            handlersStep(ArtifactKinds.Js, CompilationStage.SECOND) {
                useHandlers(
                    // An isolated test is checked as in the one-stage pipeline. `JsArtifactsDumpHandler.Checker` is run
                    // for it, as for a grouped batch, by `JsGroupingStageArtifactsDumper`.
                    { testServices ->
                        JsGroupingStageBoxRunner(
                            testServices,
                            isolatedTestHandlers = listOf(
                                ::JsSourceMapPathRewriter,
                                ::JsTypeScriptCompilationHandler,
                                ::NodeJsGeneratorHandler,
                                ::JsBoxRunner,
                                ::JsSourceMapValidator,
                            ),
                        )
                    },
                    ::JsGroupingStageArtifactsDumper,
                )
            }
        }
    }
}
