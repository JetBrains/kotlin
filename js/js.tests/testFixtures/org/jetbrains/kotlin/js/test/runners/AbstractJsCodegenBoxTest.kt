/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners

import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliJsFacade
import org.jetbrains.kotlin.js.test.converters.JsGroupingSecondStageFacade
import org.jetbrains.kotlin.js.test.converters.JsIrPreSerializationLoweringFacade
import org.jetbrains.kotlin.js.test.grouping.JsGroupingTestIsolator
import org.jetbrains.kotlin.js.test.handlers.JsGroupingStageBoxRunner
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.handlers.FirJsKlibAbiDumpBeforeInliningSavingHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpAfterInliningVerifyingHandler
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.KlibBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.TwoStageTestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.builders.loweredIrHandlersStep
import org.jetbrains.kotlin.test.configuration.commonCodegenConfiguration
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.commonIrHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.setupIrTextDumpHandlers
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR_AFTER_SPLITTING
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR_AFTER_SPLITTING_DIFFERENCE
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2_MULTI_MODULE
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.KlibAbiConsistencyDirectives.CHECK_SAME_ABI_AFTER_INLINING
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgConsistencyHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolvedTypesVerifier
import org.jetbrains.kotlin.test.grouping.AbstractTwoStageKotlinCompilerJsTest
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.SplittingModuleTransformerForBoxTests
import org.jetbrains.kotlin.test.services.SplittingTestConfigurator

/**
 * The K/JS codegen box tests, executed by the two-stage grouping test engine
 * ([org.jetbrains.kotlin.test.grouping.CompilerTestGroupingTestEngine]).
 *
 * The non-grouping stage compiles every test to its own KLIB, the same way the single-stage
 * [AbstractJsBlackBoxCodegenTestBase] pipeline does. The grouping stage then links a whole batch of those KLIBs into one
 * JS module and runs it once (see [JsGroupingSecondStageFacade] and [JsGroupingStageBoxRunner]), which is where the
 * speedup comes from: one link and one V8 load for tens of tests instead of one per test.
 *
 * Two consequences shape the configuration below, both of them following the K/Wasm counterpart
 * [org.jetbrains.kotlin.wasm.test.AbstractWasmCodegenBoxTest]:
 *  - the batch is one link and one run, so anything a test needs done its own way makes it isolated — see
 *    [JsGroupingTestIsolator] for the full list;
 *  - tests sharing a link must not clash, so their packages and module names are renamed per test
 *    ([org.jetbrains.kotlin.test.services.BatchingPackageInserter], `ESCAPE_MODULE_NAME`). Every handler whose expected
 *    output spells out those names is gated on a directive the isolator isolates on, so it is only ever reached by a
 *    test that was not renamed.
 *
 * The grouping stage runs the box runner and nothing else, which drops the JS-artifact handlers the single-stage
 * pipeline installs through `configureJsBoxHandlers` — [org.jetbrains.kotlin.js.test.handlers.JsAstHandler],
 * [org.jetbrains.kotlin.js.test.handlers.JsSourceMapValidator], [org.jetbrains.kotlin.js.test.handlers.JsSizeHandler],
 * `JsTypeScriptCompilationHandler` and `NodeJsGeneratorHandler`. None of them can run against a batch: they interpret
 * per-test source directives, per-test source maps and per-test output sizes, whereas a batch is a single JS module
 * built from many tests. The same testdata keeps being checked by them through the single-stage JS box test classes
 * (`AbstractJsES6CodegenBoxTest` over `codegen/box`, `AbstractJsCodegenInlineTest` over `codegen/boxInline`), so this
 * costs the second, non-ES6 evaluation of those checks rather than the checks themselves.
 */
abstract class AbstractJsCodegenBoxTestBase(
    private val pathToTestDir: String = "compiler/testData/codegen/box/",
    private val testGroupOutputDirPrefix: String,
    private val backend: TargetBackend = TargetBackend.JS_IR,
    private val parser: FirParser = FirParser.Psi,
) : AbstractTwoStageKotlinCompilerJsTest() {
    protected open val customIgnoreDirective: ValueDirective<TargetBackend>?
        get() = null

    protected open val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>?
        get() = null

    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        val targetBackend = backend
        commonConfiguration {
            globalDefaults {
                this.targetBackend = targetBackend
            }
            commonCodegenConfiguration()
            commonServicesConfigurationForJsCodegenTest()
            defaultDirectivesForJsBackendSecondStageTest(pathToTestDir, testGroupOutputDirPrefix)
            setUpDefaultDirectivesForJsBoxTest(parser)
            // Registered on both stages: an `IGNORE_BACKEND` failure may equally happen while compiling the test and
            // while running the batch it was linked into.
            useBlackBoxCodegenSuppressorForJsTest(customIgnoreDirective, additionalIgnoreDirectives)
            useFailureSuppressors(::FirMetaInfoDiffSuppressor)

            defaultDirectives {
                +CHECK_SAME_ABI_AFTER_INLINING
            }

            forTestsNotMatching(
                "compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*" or
                        "compiler/testData/diagnostics/*"
            ) {
                defaultDirectives {
                    DIAGNOSTICS with "-warnings"
                }
            }
        }

        nonGroupingStage {
            enableMetaInfoHandler()
            useGroupingTestIsolators(::JsGroupingTestIsolator)

            setupStepsForJsFirstStageUpToSerialization()

            configureFirHandlersStep {
                commonFirHandlersForCodegenTest()
            }
            configureIrHandlersStep {
                commonIrHandlersForCodegenTest()
            }
        }

        groupingStage {
            facadeStep(::JsGroupingSecondStageFacade)
            handlersStep(ArtifactKinds.Js, CompilationStage.SECOND) {
                useHandlers(::JsGroupingStageBoxRunner)
            }
        }
    }
}

/**
 * The non-grouping (stage-1) pipeline: FIR, FIR2IR, pre-serialization lowerings, KLIB serialization.
 *
 * Every dump handler here compares against testdata that spells out the test's package names, which grouping rewrites —
 * but each of them only fires on a directive that [JsGroupingTestIsolator] treats as a reason to isolate the test, and
 * an isolated test is never renamed. Keeping them is therefore both safe and necessary: dropping them would not just
 * lose the dump coverage, it would also silently turn the `IGNORE_*` mutes of the tests that exist to exercise those
 * dumps into "this test can be unmuted" failures.
 */
private fun TestConfigurationBuilder.setupStepsForJsFirstStageUpToSerialization() {
    facadeStep(::FirCliWebFacade)
    firHandlersStep {
        useHandlers(
            ::FirDumpHandler,
            ::FirCfgDumpHandler,
            ::FirCfgConsistencyHandler,
            ::FirResolvedTypesVerifier,
            ::FirDiagnosticsHandler,
        )
    }

    facadeStep(::Fir2IrCliWebFacade)
    irHandlersStep {
        setupIrTextDumpHandlers()
        useHandlers(
            ::NoIrCompilationErrorsHandler,
            ::FirJsKlibAbiDumpBeforeInliningSavingHandler,
            ::IrDiagnosticsHandler,
        )
    }

    facadeStep(::JsIrPreSerializationLoweringFacade)
    loweredIrHandlersStep {
        useHandlers(::NoIrCompilationErrorsHandler)
    }

    facadeStep(::FirKlibSerializerCliJsFacade)
    klibArtifactsHandlersStep {
        useHandlers(::KlibBackendDiagnosticsHandler, ::KlibAbiDumpAfterInliningVerifyingHandler, ::KlibAbiDumpHandler)
    }
}

abstract class AbstractJsCodegenBoxTest : AbstractJsCodegenBoxTestBase(
    pathToTestDir = "compiler/testData/codegen/",
    testGroupOutputDirPrefix = "codegen/box/"
) {
    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(this)
        nonGroupingStage {
            // Only ever reached by tests carrying `DUMP_IR_AFTER_INLINE`, which the isolator keeps out of batches, so
            // the dumped declarations still carry their original package names.
            configureLoweredIrDumpHandlers()
        }
    }
}

abstract class AbstractJsKlibSyntheticAccessorsBoxTest : AbstractJsCodegenBoxTestBase(
    pathToTestDir = "compiler/testData/klib/syntheticAccessors/",
    testGroupOutputDirPrefix = "klib/syntheticAccessors/"
)

abstract class AbstractJsCodegenSplittingTest(
    pathToTestDir: String = "compiler/testData/codegen/",
    testGroupOutputDirPrefix: String = "codegen/boxInlineSplitted/",
) : AbstractJsCodegenBoxTestBase(pathToTestDir, testGroupOutputDirPrefix) {
    override val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>?
        get() = listOf(IGNORE_BACKEND_K2_MULTI_MODULE)

    override fun configure(builder: TwoStageTestConfigurationBuilder): Unit = with(builder) {
        super.configure(this)
        // Splitting a test's modules is a property of how its sources are compiled, so all of this belongs to the
        // non-grouping stage; the grouping stage only links and runs what that stage produced.
        @OptIn(TestInfrastructureInternals::class)
        nonGroupingStageBuilder.useModuleStructureTransformers(::SplittingModuleTransformerForBoxTests)
        nonGroupingStageBuilder.useMetaTestConfigurators(::SplittingTestConfigurator)
        nonGroupingStage {
            // Dumps the IR of the split module structure. Only ever reached by tests carrying
            // `DUMP_IR_AFTER_SPLITTING`, which the isolator keeps out of batches, so the dumped declarations still
            // carry their original package names.
            configureIrHandlersStep {
                useHandlers(
                    { testServices, artifactKind ->
                        IrTextDumpHandler(
                            testServices = testServices,
                            artifactKind = artifactKind,
                            customExtension = "splitted.ir",
                            directive = DUMP_IR_AFTER_SPLITTING,
                            directiveForIrDifference = DUMP_IR_AFTER_SPLITTING_DIFFERENCE,
                            showOffsets = true,
                        )
                    },
                )
            }
        }
    }
}
