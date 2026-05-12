/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWasmFacade
import org.jetbrains.kotlin.js.test.klib.customWasmJsCompilerSettings
import org.jetbrains.kotlin.js.test.klib.defaultLanguageVersion
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.TwoStageTestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.builders.configureLoweredIrHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonCodegenConfiguration
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.grouping.AbstractTwoStageKotlinCompilerTest
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageTestSuppressor
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestSuppressor
import org.jetbrains.kotlin.test.klib.setupCustomLanguageVersionForKlibCompatibilityTest
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.jetbrains.kotlin.test.services.configuration.UnsupportedFeaturesTestConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlin.wasm.test.blackbox.CustomWasmSecondStageFacade
import org.jetbrains.kotlin.wasm.test.blackbox.WasmGroupingTestIsolator
import org.jetbrains.kotlin.wasm.test.commonConfigurationForWasmFirstStageTest
import org.jetbrains.kotlin.wasm.test.commonConfigurationForWasmSecondStageTest
import org.jetbrains.kotlin.wasm.test.handlers.WasmFolderBoxRunnerGroupingStageWithV8Only
import org.jetbrains.kotlin.wasm.test.preprocessors.WasmJsExportBoxPreprocessor
import org.jetbrains.kotlin.wasm.test.providers.WasmJsLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.wasm.test.setupStepsForWasmFirstStageUpToSerialization
import org.jetbrains.kotlin.wasm.test.utils.configureIgnoredTestSuppressor
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("custom-second-stage")
open class AbstractCustomWasmJsCompilerSecondStageTest(val testDataRoot: String = "compiler/testData/codegen/") :
    AbstractTwoStageKotlinCompilerTest() {

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

    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        commonConfiguration {
            globalDefaults {
                targetBackend = TargetBackend.WASM_JS
            }
            commonConfigurationForWasmFirstStageTest(
                targetFrontend = FrontendKinds.FIR,
                targetPlatform = WasmPlatforms.wasmJs,
                wasmTarget = WasmTarget.JS,
                additionalSourceProvider = null,
                customIgnoreDirective = null,
                additionalIgnoreDirectives = null,
            )

            useMetaTestConfigurators(::UnsupportedFeaturesTestConfigurator)
            useDirectives(WasmEnvironmentConfigurationDirectives)

            // Isolated (single-test) batches are executed via the box-export model (the runner calls
            // `jsModule.box()` and asserts `"OK"`), exactly like the in-process codegen runner. The CLI
            // second-stage compiler has no test hook to export `box()`, so it is marked `@JsExport` in the
            // source during the first stage (mirrors what `WasmLoweringFacade` does for the in-process path).
            useSourcePreprocessor(::WasmJsExportBoxPreprocessor)

            defaultDirectives {
                if (customWasmJsCompilerSettings.defaultLanguageVersion < LanguageVersion.LATEST_STABLE) {
                    // We need to set the custom LV to let `UnsupportedFeaturesTestConfigurator` skip tests with
                    // the language features that are not supported in the given custom LV.
                    setupCustomLanguageVersionForKlibCompatibilityTest(customWasmJsCompilerSettings.defaultLanguageVersion)

                    LANGUAGE with "+ExportKlibToOlderAbiVersion"
                }

                +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
                DiagnosticsDirectives.DIAGNOSTICS with listOf("-infos")
                FirDiagnosticsDirectives.FIR_PARSER with FirParser.LightTree
            }

            useConfigurators(::WasmSecondStageEnvironmentConfigurator.bind(WasmTarget.JS))
        }
        nonGroupingStage {
            useGroupingTestIsolators(::WasmGroupingTestIsolator)
            useAdditionalSourceProviders(::WasmJsLauncherAdditionalSourceProvider)
            commonCodegenConfiguration()

            setupStepsForWasmFirstStageUpToSerialization(
                includeBasicFirHandlers = true,
                // Due to package escaping, various dumps for grouping mode would be different from the regular one,
                // so we don't want all the frontend handlers to be set up, only some specific ones.
                includeDumpFirHandlers = false,
            )
            configureFirHandlersStep {
                commonFirHandlersForCodegenTest()
            }
            configureIrHandlersStep {
                useHandlers(::NoIrCompilationErrorsHandler)
            }

            configureLoweredIrHandlersStep {
                useHandlers(::NoIrCompilationErrorsHandler)
            }

            facadeStep(::FirKlibSerializerCliWasmFacade)
            klibArtifactsHandlersStep()

            commonConfigurationForWasmSecondStageTest(
                pathToTestDir = testDataRoot,
                testGroupOutputDirPrefix = this@AbstractCustomWasmJsCompilerSecondStageTest::class.java.simpleName +
                        customWasmJsCompilerSettings.defaultLanguageVersion,
            )
            configureIgnoredTestSuppressor()
            useFailureSuppressors(
                // Suppress all tests that failed on the first stage if they are anyway marked as "IGNORE_BACKEND*".
                ::CustomKlibCompilerTestSuppressor,
                // Suppress failed tests having `// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: X.Y.Z`,
                // where `X.Y.Z` matches to `customWasmJsCompilerSettings.version`
                ::CustomKlibCompilerSecondStageTestSuppressor.bind(customWasmJsCompilerSettings.defaultLanguageVersion),
            )

            forTestsNotMatching(
                "compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*" or
                        "compiler/testData/diagnostics/*"
            ) {
                defaultDirectives {
                    DIAGNOSTICS with "-warnings"
                }
            }
        }
        groupingStage {
            facadeStep(CustomWasmSecondStageFacade::Grouping.bind(customWasmJsCompilerSettings))
            handlersStep(ArtifactKinds.Wasm, CompilationStage.SECOND) {
                useHandlers(::WasmFolderBoxRunnerGroupingStageWithV8Only)
            }
        }
    }

    /**
     * Drives both compilation stages synchronously for a single test, mirroring what
     * [org.jetbrains.kotlin.test.grouping.CompilerTestGroupingTestEngine] does for a single-sized batch.
     *
     * Generated box/boxInline tests are executed by the grouping test engine via
     * `initTestRunnerAndCreateModuleStructure`; this helper is used by the sanity tests that need to assert
     * synchronously on the outcome of a single test.
     */
    fun runTest(@TestDataFile filePath: String) {
        initTestRunnerAndCreateModuleStructure(filePath)
        try {
            nonGroupingRunner.runTestPreprocessing()
            nonGroupingRunner.runSteps()

            // Report first-stage failures first (and throw on a real, non-suppressed failure). If the first stage
            // failed or was muted/ignored, the grouping (second) stage must be skipped, exactly like the grouping
            // test engine excludes such tests from the batch. Otherwise both stages would contribute failures and
            // they'd be aggregated into a `MultipleFailuresError` instead of the single expected exception.
            val hadIgnoredFailuresOnFirstStage = nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = false)
            if (hadIgnoredFailuresOnFirstStage) return

            val nonGroupingStageOutput = NonGroupingStageOutput(
                testServices = nonGroupingRunner.testServices,
                catchingExecutor = { wrapper, block ->
                    nonGroupingRunner.failuresInterceptor.withAssertionCatching(wrapper, block)
                },
            )
            groupingStageRunner.run(listOf(nonGroupingStageOutput))

            // Exceptions from grouped facades were reported to the grouping runner's failures interceptor,
            // but failure suppressors must be run from the non-grouping runner, as they need access to the
            // real module structure of the specific test to extract directives from there.
            nonGroupingRunner.failuresInterceptor += groupingStageRunner.failuresInterceptor
            nonGroupingRunner.failuresInterceptor.reportFailures(checkForUnmuting = true)
        } finally {
            nonGroupingRunner.finalizeAndDispose()
        }
    }
}
