/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners

import org.jetbrains.kotlin.js.test.handlers.JsArtifactsDumpHandler
import org.jetbrains.kotlin.js.test.handlers.JsAstHandler
import org.jetbrains.kotlin.js.test.handlers.JsBoxRunner
import org.jetbrains.kotlin.js.test.handlers.JsIrRecompiledArtifactsIdentityHandler
import org.jetbrains.kotlin.js.test.handlers.JsSizeHandler
import org.jetbrains.kotlin.js.test.handlers.JsSourceMapPathRewriter
import org.jetbrains.kotlin.js.test.handlers.JsSourceMapValidator
import org.jetbrains.kotlin.js.test.handlers.JsTypeScriptCompilationHandler
import org.jetbrains.kotlin.js.test.handlers.NodeJsGeneratorHandler
import org.jetbrains.kotlin.js.test.klib.JsGroupedTestHelpersModuleTransformer
import org.jetbrains.kotlin.js.test.klib.JsGroupedTestModuleOutputNameConfigurator
import org.jetbrains.kotlin.js.test.klib.JsGroupingStageArtifactsDumper
import org.jetbrains.kotlin.js.test.klib.JsGroupingStageBoxRunner
import org.jetbrains.kotlin.js.test.klib.JsGroupingTestIsolator
import org.jetbrains.kotlin.js.test.klib.JsInProcessSecondStageFacade
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.TwoStageTestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.configuration.commonCodegenConfiguration
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.GENERATE_INLINE_ANONYMOUS_FUNCTIONS
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.IGNORE_WITH_INLINE_ANONYMOUS_FUNCTIONS
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.grouping.AbstractTwoStageKotlinCompilerJsTest
import org.jetbrains.kotlin.test.klib.KlibOnlyFirstStageStateReleaser
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind

abstract class AbstractJsTwoStageCodegenBoxTestBase(
    private val pathToTestDir: String,
    private val testGroupOutputDirPrefix: String,
    private val targetBackend: TargetBackend = TargetBackend.JS_IR,
) : AbstractTwoStageKotlinCompilerJsTest() {
    protected open val customIgnoreDirective: ValueDirective<TargetBackend>?
        get() = null

    protected open val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>?
        get() = null

    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        commonConfiguration {
            globalDefaults {
                targetBackend = this@AbstractJsTwoStageCodegenBoxTestBase.targetBackend
                frontend = FrontendKinds.FIR
                targetPlatform = JsPlatforms.defaultJsPlatform
                dependencyKind = DependencyKind.Binary
            }
            useFailureSuppressors(
                ::BlackBoxCodegenSuppressor.bind(customIgnoreDirective, additionalIgnoreDirectives),
                ::FirMetaInfoDiffSuppressor,
            )
        }

        nonGroupingStage {
            useGroupingTestIsolators(::createGroupingTestIsolator)
            @OptIn(TestInfrastructureInternals::class)
            useModuleStructureTransformers(::JsGroupedTestHelpersModuleTransformer)

            commonCodegenConfiguration()
            commonConfigurationForJsTest()
            useConfigurators(::JsGroupedTestModuleOutputNameConfigurator)
            setupCommonFirstStageHandlersForJsTest()
            enableMetaInfoHandler()

            defaultDirectivesForJsBackendSecondStageTest(pathToTestDir, testGroupOutputDirPrefix)
            defaultDiagnosticsDirectivesForJsCodegenTest()
            setUpFirstStageForJsBoxTest(FirParser.LightTree)
            configureFirHandlersStep {
                commonFirHandlersForCodegenTest()
            }

            useFailureSuppressors(JsArtifactsDumpHandler::Suppressor)
            useAfterAnalysisCheckers(::NonIncrementalFirstStageStateReleaser)
        }

        groupingStage {
            facadeStep { JsInProcessSecondStageFacade.Grouping(it, recompile = true, linkPerModule = true) }
            handlersStep(ArtifactKinds.Js, CompilationStage.SECOND) {
                useHandlers(
                    { testServices ->
                        JsGroupingStageBoxRunner(
                            testServices,
                            isolatedTestHandlers = listOf(
                                ::JsSourceMapPathRewriter,
                                ::JsTypeScriptCompilationHandler,
                                ::NodeJsGeneratorHandler,
                                ::JsBoxRunner,
                                ::JsAstHandler,
                                ::JsSourceMapValidator,
                                ::JsIrRecompiledArtifactsIdentityHandler,
                                ::JsSizeHandler,
                            ),
                            groupedBatchHandlers = listOf(
                                ::JsAstHandler,
                                ::JsSourceMapValidator,
                            ),
                            artifactsAreDumped = true,
                        )
                    },
                    ::JsGroupingStageArtifactsDumper,
                )
            }
        }
    }

    private fun createGroupingTestIsolator(testServices: TestServices): JsGroupingTestIsolator =
        JsGroupingTestIsolator(
            testServices,
            customCompilerStage = null,
            isolateMultiModuleTests = true,
            isolateTestsWithDumps = true,
            isolateTestsWithJsAstChecks = true,
            additionalIgnoreDirectives = listOfNotNull(customIgnoreDirective) + additionalIgnoreDirectives.orEmpty(),
        )
}

/** `RecompileModuleJsIrBackendFacade` starts from the first-stage IR, so the state of an incremental test is kept. */
private class NonIncrementalFirstStageStateReleaser(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    private val releaser = KlibOnlyFirstStageStateReleaser(testServices)

    override fun check(thereWereFailures: Boolean) {
        if (JsEnvironmentConfigurator.incrementalEnabled(testServices)) return
        releaser.check(thereWereFailures)
    }
}

abstract class AbstractJsCodegenBoxTest(
    testGroupOutputDirPrefix: String = "codegen/box/",
    pathToTestDir: String = "compiler/testData/codegen/box",
) : AbstractJsTwoStageCodegenBoxTestBase(
    pathToTestDir = pathToTestDir,
    testGroupOutputDirPrefix = testGroupOutputDirPrefix,
) {
    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(this)
        nonGroupingStage {
            configureLoweredIrDumpHandlers()
        }
    }
}

abstract class AbstractJsCodegenBoxWithInlineAnonymousFunctionsTest(
    testGroupOutputDirPrefix: String = "codegen/boxWithInlineAnonymousFunctions/",
) : AbstractJsCodegenBoxTest(testGroupOutputDirPrefix) {
    override val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>
        get() = listOf(IGNORE_WITH_INLINE_ANONYMOUS_FUNCTIONS)

    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(this)
        commonConfiguration {
            defaultDirectives {
                +GENERATE_INLINE_ANONYMOUS_FUNCTIONS
            }
        }
    }
}
