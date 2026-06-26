/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWasmFacade
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.TwoStageTestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.builders.configureLoweredIrHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonCodegenConfiguration
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.grouping.AbstractTwoStageKotlinCompilerTest
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.GroupingStageHandler
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.SplittingModuleTransformerForBoxTests
import org.jetbrains.kotlin.test.services.SplittingTestConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.enableByConfigurationKey
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys.WASM_GENERATE_CLOSED_WORLD_MULTIMODULE
import org.jetbrains.kotlin.wasm.test.blackbox.WasmGroupingTestIsolator
import org.jetbrains.kotlin.wasm.test.converters.WasmInProcessSecondStageFacade
import org.jetbrains.kotlin.wasm.test.handlers.WasmCompilationSetsGroupingStageBoxRunner
import org.jetbrains.kotlin.wasm.test.handlers.WasmJsCoroutinesStackSwitchingBoxRunner
import org.jetbrains.kotlin.wasm.test.providers.WasmJsLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.wasm.test.utils.configureIgnoredTestSuppressor

abstract class AbstractWasmCodegenBoxTest(
    val backend: TargetBackend,
    val platform: TargetPlatform,
    val wasmTarget: WasmTarget,
    val pathToTestDir: String = "compiler/testData/codegen/",
): AbstractTwoStageKotlinCompilerTest() {
    abstract val additionalSourceProviders: List<Constructor<AdditionalSourceProvider>>
    open val wasmCompilationSetsBoxRunner: Constructor<GroupingStageHandler<BinaryArtifacts.Wasm>> = ::WasmCompilationSetsGroupingStageBoxRunner

    // Allow subclasses to contribute ignore directives for BlackBoxCodegenSuppressor,
    // mirroring the single-stage base `AbstractWasmBlackBoxCodegenTestBase`.
    open val customIgnoreDirective: ValueDirective<TargetBackend>? = null
    open val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>? = null

    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        commonConfiguration {
            globalDefaults {
                targetBackend = backend
            }
            commonConfigurationForWasmFirstStageTest(
                targetFrontend = FrontendKinds.FIR,
                targetPlatform = platform,
                wasmTarget = wasmTarget,
                additionalSourceProvider = null,
                customIgnoreDirective,
                additionalIgnoreDirectives,
            )
            defaultDirectives {
                +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
                FirDiagnosticsDirectives.FIR_PARSER with FirParser.LightTree
                DIAGNOSTICS with listOf("-infos")
            }
            useConfigurators(::WasmSecondStageEnvironmentConfigurator.bind(wasmTarget))
            configureIgnoredTestSuppressor()
            useFailureSuppressors(
                ::FirMetaInfoDiffSuppressor,
            )
        }
        nonGroupingStage {
            enableMetaInfoHandler()
            useGroupingTestIsolators(::WasmGroupingTestIsolator)
            additionalSourceProviders.forEach { useAdditionalSourceProviders(it) }
            commonCodegenConfiguration()

            setupStepsForWasmFirstStageUpToSerialization(
                includeBasicFirHandlers = true,
                // Due to package escaping, various dumps for grouping mode would be different from the regular one,
                // so we don't want all the frontend handlers to be set up, only some specific ones.
                includeDumpFirHandlers = false,
            )
            configureCodegenFirHandlerSteps()

            configureIrHandlersStep {
                useHandlers(::NoIrCompilationErrorsHandler)
            }

            configureLoweredIrHandlersStep {
                useHandlers(::NoIrCompilationErrorsHandler)
            }

            facadeStep(::FirKlibSerializerCliWasmFacade)
            klibArtifactsHandlersStep()

            commonConfigurationForWasmSecondStageTest(
                pathToTestDir,
                testGroupOutputDirPrefix = this@AbstractWasmCodegenBoxTest::class.java.simpleName,
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
            facadeStep(WasmInProcessSecondStageFacade::Grouping)
            handlersStep(ArtifactKinds.Wasm, CompilationStage.SECOND) {
                useHandlers(wasmCompilationSetsBoxRunner)
            }
        }
    }
}

abstract class AbstractWasmJsCodegenBoxTest(pathToTestDir: String = "compiler/testData/codegen/") : AbstractWasmCodegenBoxTest(
    TargetBackend.WASM_JS,
    WasmPlatforms.wasmJs,
    WasmTarget.JS,
    pathToTestDir,
) {
    override val additionalSourceProviders: List<Constructor<AdditionalSourceProvider>> = listOf(::WasmJsLauncherAdditionalSourceProvider)
}

abstract class AbstractWasmWasiCodegenBoxTest : AbstractWasmCodegenBoxTest(TargetBackend.WASM_WASI, WasmPlatforms.wasmWasi, WasmTarget.WASI) {
    override val additionalSourceProviders: List<Constructor<AdditionalSourceProvider>> = listOf(
        ::WasmJsLauncherAdditionalSourceProvider,
        ::WasmWasiBoxTestHelperSourceProvider,
    )
}

abstract class AbstractWasmJsCodegenBoxInlinedTest(pathToTestDir: String = "compiler/testData/codegen/") :
    AbstractWasmJsCodegenBoxTest(pathToTestDir)
{
    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(this)
        commonConfiguration {
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}",
                )
            }
        }
    }
}

abstract class AbstractWasmWasiCodegenBoxInlinedTest : AbstractWasmWasiCodegenBoxTest() {
    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(this)
        commonConfiguration {
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}",
                )
            }
        }
    }
}

abstract class AbstractWasmJsCodegenSplittingTest : AbstractWasmJsCodegenBoxInlinedTest() {
    // Splitting multi-module runs must respect K2 multi-module ignore directives present
    // in testdata. Support `IGNORE_BACKEND_K2_MULTI_MODULE` just like the single-stage base.
    override val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>?
        get() = listOf(CodegenTestDirectives.IGNORE_BACKEND_K2_MULTI_MODULE)

    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(this)
        @OptIn(TestInfrastructureInternals::class)
        nonGroupingStageBuilder.useModuleStructureTransformers(
            ::SplittingModuleTransformerForBoxTests
        )
        nonGroupingStageBuilder.useMetaTestConfigurators(::SplittingTestConfigurator)
    }
}

abstract class AbstractWasmJsSyntheticAccessorsBoxTest : AbstractWasmJsCodegenBoxInlinedTest("compiler/testData/klib/syntheticAccessors")

abstract class AbstractWasmJsTranslatorTest : AbstractWasmJsCodegenBoxTest("js/js.translator/testData/box/")

abstract class AbstractWasmJsCodegenCoroutinesStackSwitchingTest : AbstractWasmJsCodegenBoxTest() {
    override val wasmCompilationSetsBoxRunner: Constructor<GroupingStageHandler<BinaryArtifacts.Wasm>> = ::WasmJsCoroutinesStackSwitchingBoxRunner

    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(this)
        commonConfiguration {
            defaultDirectives {
                +WasmEnvironmentConfigurationDirectives.USE_STACK_SWITCHING_PROPOSAL
            }
        }
    }
}

abstract class AbstractWasmJsCodegenMultiModuleTest : AbstractWasmJsCodegenBoxTest() {
    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(this)
        nonGroupingStageBuilder.enableByConfigurationKey(WASM_GENERATE_CLOSED_WORLD_MULTIMODULE)
        commonConfiguration {
            defaultDirectives {
                // Closed-world multi-module compilation is incompatible with the current grouped-batch path: the grouped runner uses `startUnitTests`
                // (unit-test runner) for multi-test batches, but these are box tests that export `box()` and have no `startUnitTests`.
                // WASM_STANDALONE directive routes tests through the standalone box-export path, matching the non-grouping runner behavior.
                +WasmEnvironmentConfigurationDirectives.WASM_STANDALONE
            }
        }
    }
}

abstract class AbstractWasmJsCodegenCoroutinesStackSwitchingMultiModuleTest : AbstractWasmJsCodegenMultiModuleTest() {
    override val wasmCompilationSetsBoxRunner: Constructor<GroupingStageHandler<BinaryArtifacts.Wasm>> = ::WasmJsCoroutinesStackSwitchingBoxRunner

    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(this)
        commonConfiguration {
            defaultDirectives {
                +WasmEnvironmentConfigurationDirectives.USE_STACK_SWITCHING_PROPOSAL
            }
        }
    }
}
