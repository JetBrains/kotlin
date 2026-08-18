/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWasmFacade
import org.jetbrains.kotlin.js.test.klib.customWasmJsCompilerSettings
import org.jetbrains.kotlin.js.test.klib.defaultLanguageVersion
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.configuration.commonCodegenConfiguration
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.grouping.AbstractTwoStageKotlinCompilerWasmTest
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageTestSuppressor
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestSuppressor
import org.jetbrains.kotlin.test.klib.setupCustomLVForKlibForwardCompatibilityTest
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
import org.jetbrains.kotlin.wasm.test.handlers.WasmFolderGroupingStageBoxWithV8OnlyRunner
import org.jetbrains.kotlin.wasm.test.preprocessors.WasmJsExportBoxPreprocessor
import org.jetbrains.kotlin.wasm.test.providers.WasmJsLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.wasm.test.setupStepsForWasmFirstStageUpToSerialization
import org.jetbrains.kotlin.wasm.test.utils.configureIgnoredTestSuppressor
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("custom-second-stage")
open class AbstractCustomWasmJsCompilerSecondStageTest(val testDataRoot: String = "compiler/testData/codegen/") :
    AbstractTwoStageKotlinCompilerWasmTest() {

    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return if (customWasmJsCompilerSettings.defaultLanguageVersion >= LanguageVersion.LATEST_STABLE)
            super.createKotlinStandardLibrariesPathProvider()
        else
            object : KotlinStandardLibrariesPathProvider by StandardLibrariesPathProviderForKotlinProject {
                override fun fullWasmStdlib(target: WasmTarget): File {
                    checkTestInfrastructure(target == WasmTarget.JS) { "Unexpected target $target" }
                    return customWasmJsCompilerSettings.stdlib
                }

                override fun kotlinTestWasmKLib(target: WasmTarget): File {
                    checkTestInfrastructure(target == WasmTarget.JS) { "Unexpected target $target" }
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
                setupCustomLVForKlibForwardCompatibilityTest(customWasmJsCompilerSettings.defaultLanguageVersion)
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
                useHandlers(::WasmFolderGroupingStageBoxWithV8OnlyRunner)
            }
        }
    }
}
