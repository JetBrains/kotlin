/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.js.test.klib.CustomWebCompilerFirstStageFacade
import org.jetbrains.kotlin.js.test.klib.CustomWebCompilerSettings
import org.jetbrains.kotlin.js.test.klib.customWasmJsCompilerSettings
import org.jetbrains.kotlin.js.test.klib.customWasmWasiCompilerSettings
import org.jetbrains.kotlin.js.test.klib.defaultLanguageVersion
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TwoStageTestConfigurationBuilder
import org.jetbrains.kotlin.test.grouping.AbstractTwoStageKotlinCompilerWasmTest
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerFirstStageTestSuppressor
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerTestSuppressor
import org.jetbrains.kotlin.test.klib.setupCustomLanguageVersionForKlibCompatibilityTest
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.UnsupportedFeaturesTestConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlin.wasm.test.WasmCoroutineHelpersModuleTransformer
import org.jetbrains.kotlin.wasm.test.WasmWasiBoxTestHelperSourceProvider
import org.jetbrains.kotlin.wasm.test.blackbox.WasmGroupingTestIsolator
import org.jetbrains.kotlin.wasm.test.commonConfigurationForWasmSecondStageTest
import org.jetbrains.kotlin.wasm.test.converters.WasmInProcessSecondStageFacade
import org.jetbrains.kotlin.wasm.test.handlers.WasmCompilationSetsGroupingStageBoxWithSingleVmRunner
import org.jetbrains.kotlin.wasm.test.providers.WasmJsLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.wasm.test.utils.configureIgnoredTestSuppressor
import org.junit.jupiter.api.Tag

/**
 * KLIB backward-compatibility test: the non-grouping (first) stage compiles every test into a KLIB with a previously
 * released Kotlin/Wasm compiler invoked via CLI, and the grouping (second) stage links batches of such KLIBs into
 * executables with the current in-process compiler and runs them.
 */
@Tag("custom-first-stage")
abstract class AbstractCustomWasmCompilerFirstStageTest(
    private val targetBackend: TargetBackend,
    private val wasmTargetPlatform: TargetPlatform,
    private val wasmTarget: WasmTarget,
    private val customWasmCompilerSettings: CustomWebCompilerSettings,
    val testDataRoot: String = "compiler/testData/codegen/",
) : AbstractTwoStageKotlinCompilerWasmTest() {

    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        commonConfiguration {
            globalDefaults {
                targetBackend = this@AbstractCustomWasmCompilerFirstStageTest.targetBackend
                frontend = FrontendKinds.FIR
                targetPlatform = wasmTargetPlatform
                dependencyKind = DependencyKind.Binary
            }

            defaultDirectives {
                // We need to set the custom LV to let `UnsupportedFeaturesTestConfigurator` skip tests with
                // the language features that are not supported in the given custom LV.
                setupCustomLanguageVersionForKlibCompatibilityTest(customWasmCompilerSettings.defaultLanguageVersion)
            }

            useMetaTestConfigurators(::UnsupportedFeaturesTestConfigurator)
            useConfigurators(
                ::CommonEnvironmentConfigurator,
                ::WasmFirstStageEnvironmentConfigurator.bind(wasmTarget),
                // And this configurator is necessary to relax the second compilation stage, since the old compiler could produce IR
                // which would not pass new improved IR validation rules
                ::CustomWasmCompilerSecondStageEnvironmentConfigurator.bind(wasmTarget),
            )
            // The first stage compiles against the custom compiler's own stdlib (see `CustomWebCompilerFirstStageFacade`).
            useReflectionPackageNameAnnotationIfSupported(customWasmCompilerSettings.defaultLanguageVersion)
        }
        nonGroupingStage {
            useGroupingTestIsolators(::WasmGroupingTestIsolator)
            useAdditionalSourceProviders(
                ::CoroutineHelpersSourceFilesProvider,
                ::AdditionalDiagnosticsSourceFilesProvider,
                ::WasmJsLauncherAdditionalSourceProvider,
            )
            // A grouped batch links several per-test KLIBs at once, so the coroutine helpers must live in a single
            // shared `helpers` KLIB instead of being duplicated in every per-test KLIB.
            @OptIn(TestInfrastructureInternals::class)
            useModuleStructureTransformers(WasmCoroutineHelpersModuleTransformer)

            facadeStep(::CustomWebCompilerFirstStageFacade)

            commonConfigurationForWasmSecondStageTest(
                pathToTestDir = testDataRoot,
                testGroupOutputDirPrefix = this@AbstractCustomWasmCompilerFirstStageTest::class.java.simpleName +
                        customWasmCompilerSettings.defaultLanguageVersion,
            )
            configureIgnoredTestSuppressor()
            useFailureSuppressors(
                // Suppress all tests that have not been successfully compiled by the first stage.
                // And the limited number of tests where KLIBs generated by a specific compiler version
                // are known to have some problems that cause crash on the second stage.
                ::CustomKlibCompilerFirstStageTestSuppressor.bind(customWasmCompilerSettings.defaultLanguageVersion),

                // Suppress all tests that failed on the second stage if they are anyway marked as "IGNORE_BACKEND*".
                ::CustomKlibCompilerTestSuppressor,
            )
        }
        groupingStage {
            facadeStep(WasmInProcessSecondStageFacade::Grouping)
            handlersStep(ArtifactKinds.Wasm, CompilationStage.SECOND) {
                useHandlers(::WasmCompilationSetsGroupingStageBoxWithSingleVmRunner)
            }
        }
    }
}

@Tag("custom-first-stage-wasm-js")
open class AbstractCustomWasmJsCompilerFirstStageTest(testDataRoot: String = "compiler/testData/codegen/") :
    AbstractCustomWasmCompilerFirstStageTest(
        targetBackend = TargetBackend.WASM_JS,
        wasmTargetPlatform = WasmPlatforms.wasmJs,
        wasmTarget = WasmTarget.JS,
        customWasmCompilerSettings = customWasmJsCompilerSettings,
        testDataRoot = testDataRoot,
    )

@Tag("custom-first-stage-wasm-wasi")
open class AbstractCustomWasmWasiCompilerFirstStageTest(testDataRoot: String = "compiler/testData/codegen/") :
    AbstractCustomWasmCompilerFirstStageTest(
        targetBackend = TargetBackend.WASM_WASI,
        wasmTargetPlatform = WasmPlatforms.wasmWasi,
        wasmTarget = WasmTarget.WASI,
        customWasmCompilerSettings = customWasmWasiCompilerSettings,
        testDataRoot = testDataRoot,
    ) {
    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(this)
        nonGroupingStage {
            useAdditionalSourceProviders(::WasmWasiBoxTestHelperSourceProvider)
        }
    }
}
