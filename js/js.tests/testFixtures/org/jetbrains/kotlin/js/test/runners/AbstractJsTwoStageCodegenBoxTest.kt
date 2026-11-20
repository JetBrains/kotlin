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
import org.jetbrains.kotlin.test.klib.KlibZipFileSystemCacheCloser
import org.jetbrains.kotlin.test.klib.KlibZipFileSystemCacheConfigurator
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind

/**
 * The K/JS codegen box tests, executed by the two-stage grouping test engine:
 * - the non-grouping (first) stage compiles every test into a KLIB and runs the same checks as the first phase of
 *   the one-stage pipeline (see [AbstractJsBlackBoxCodegenTestBase]),
 * - the grouping (second) stage links batches of such KLIBs into executables and runs `box()` of every test.
 *
 * The JS of a single test cannot be told apart in a grouped batch, so a batch is checked only by what checks every
 * executable, whatever the test: its source maps and its JS AST are validated, and `box()` of every test is run in every
 * translation mode. [JsGroupingTestIsolator] isolates every test that checks anything else, a test that dumps its FIR,
 * IR or KLIB ABI, or checks its JS, as well as a test of several modules, which would get the names of the batch for
 * its modules and would not be linked with its main module as the main one. An isolated test is checked as in the
 * one-stage pipeline.
 *
 * In the per-module modes, the launcher module is the main module of a batch, and the modules of its tests are its
 * libraries. So a batch checks how modules export and import declarations, while the code generated for the main
 * module of a test is checked by the isolated tests only.
 */
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
            // A test expected to fail may fail on either stage.
            useFailureSuppressors(
                ::BlackBoxCodegenSuppressor.bind(customIgnoreDirective, additionalIgnoreDirectives),
                ::FirMetaInfoDiffSuppressor,
            )
        }

        nonGroupingStage {
            useGroupingTestIsolators(::createGroupingTestIsolator)
            // A grouped batch links several per-test KLIBs at once, so the helper sources must live in a single shared KLIB
            // instead of being duplicated in every per-test KLIB.
            @OptIn(TestInfrastructureInternals::class)
            useModuleStructureTransformers(::JsGroupedTestHelpersModuleTransformer)

            commonCodegenConfiguration()
            commonConfigurationForJsTest()
            // A grouped batch is linked in the per-module modes as well, which name the files of the modules after them.
            useConfigurators(::JsGroupedTestModuleOutputNameConfigurator)
            setupCommonFirstStageHandlersForJsTest()
            enableMetaInfoHandler()

            // Named after the second stage of the one-stage pipeline, but needed here: the module structure of a test, with
            // its directives, is built by the non-grouping stage, and the grouping stage links with the services of the tests.
            defaultDirectivesForJsBackendSecondStageTest(pathToTestDir, testGroupOutputDirPrefix)
            defaultDiagnosticsDirectivesForJsCodegenTest()
            // The dumps would spell the package a grouped test is put into, but a test that dumps anything is isolated.
            setUpFirstStageForJsBoxTest(FirParser.LightTree)
            configureFirHandlersStep {
                commonFirHandlersForCodegenTest()
            }

            // Point the JS stack traces of an isolated test at its dumped artifacts (see `JsGroupingStageArtifactsDumper`).
            useFailureSuppressors(JsArtifactsDumpHandler::Suppressor)
            // The non-grouping stage compiles against the zipped stdlib and kotlin-test KLIBs.
            useConfigurators(::KlibZipFileSystemCacheConfigurator)
            useAfterAnalysisCheckers(::KlibZipFileSystemCacheCloser, ::NonIncrementalFirstStageStateReleaser)
        }

        groupingStage {
            facadeStep { JsInProcessSecondStageFacade.Grouping(it, recompile = true, linkPerModule = true) }
            handlersStep(ArtifactKinds.Js, CompilationStage.SECOND) {
                useHandlers(
                    // An isolated test is checked as in the one-stage pipeline, a grouped batch by running `box()` of every test.
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

/**
 * Applies [KlibOnlyFirstStageStateReleaser] to a test that is not recompiled incrementally: its grouping stage
 * needs nothing but its KLIBs. `RecompileModuleJsIrBackendFacade` starts from the IR of the first stage instead.
 */
private class NonIncrementalFirstStageStateReleaser(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    private val releaser = KlibOnlyFirstStageStateReleaser(testServices)

    override fun check(thereWereFailures: Boolean) {
        if (JsEnvironmentConfigurator.incrementalEnabled(testServices)) return
        releaser.check(thereWereFailures)
    }
}

abstract class AbstractJsCodegenBoxTest(
    testGroupOutputDirPrefix: String = "codegen/box/",
) : AbstractJsTwoStageCodegenBoxTestBase(
    pathToTestDir = "compiler/testData/codegen/box",
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
