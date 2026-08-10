/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners

import org.jetbrains.kotlin.js.test.handlers.JsIrRecompiledArtifactsIdentityHandler
import org.jetbrains.kotlin.js.test.handlers.JsLineNumberHandler
import org.jetbrains.kotlin.js.test.handlers.JsSizeHandler
import org.jetbrains.kotlin.js.test.handlers.JsWrongModuleHandler
import org.jetbrains.kotlin.js.test.utils.configureLineNumberTests
import org.jetbrains.kotlin.js.test.utils.configureSteppingTests
import org.jetbrains.kotlin.parsing.parseBoolean
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.handlers.IrPreprocessedInlineFunctionDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.configuration.setupIrTextDumpHandlers
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR_AFTER_INLINE
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR_AFTER_INLINE_DIFFERENCE
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2_MULTI_MODULE
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.KlibAbiConsistencyDirectives.CHECK_SAME_ABI_AFTER_INLINING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgConsistencyHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolvedTypesVerifier
import org.jetbrains.kotlin.test.services.SplittingModuleTransformerForBoxTests
import org.jetbrains.kotlin.test.services.SplittingTestConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import java.lang.Boolean.getBoolean


abstract class AbstractJsTest(
    pathToTestDir: String = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix: String,
    targetBackend: TargetBackend = TargetBackend.JS_IR,
    val parser: FirParser = FirParser.Psi,
) : AbstractJsBlackBoxCodegenTestBase(targetBackend, pathToTestDir, testGroupOutputDirPrefix) {
    override val backendFacades: JsBackendFacades
        get() = JsBackendFacades.WithRecompilation

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            setUpDefaultDirectivesForJsBoxTest(parser)
            defaultDirectives {
                +CHECK_SAME_ABI_AFTER_INLINING
            }
            configureFirHandlersStep {
                useHandlers(
                    ::FirDumpHandler,
                    ::FirCfgDumpHandler,
                    ::FirCfgConsistencyHandler,
                    ::FirResolvedTypesVerifier,
                )
            }
            configureIrHandlersStep {
                setupIrTextDumpHandlers()
            }
            configureJsArtifactsHandlersStep {
                useHandlers(
                    ::JsIrRecompiledArtifactsIdentityHandler,
                    ::JsSizeHandler,
                )
            }

            forTestsMatching("${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/closure/inlineAnonymousFunctions/*") {
                defaultDirectives {
                    +JsEnvironmentConfigurationDirectives.GENERATE_INLINE_ANONYMOUS_FUNCTIONS
                }
            }
        }
    }
}

abstract class AbstractPsiJsBoxTest : AbstractJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = "psiBox/",
    parser = FirParser.Psi,
)

abstract class AbstractLightTreeJsBoxTest : AbstractJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = "lightTreeBox/",
    parser = FirParser.LightTree,
)

abstract class AbstractJsLineNumberTest(
    testGroupOutputDirPrefix: String = "lineNumbers/"
) : AbstractJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/lineNumbers/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureLineNumberTests(::JsLineNumberHandler)
    }
}

abstract class AbstractSourceMapGenerationSmokeTest : AbstractJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/sourcemap/",
    testGroupOutputDirPrefix = "sourcemap/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +JsEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP
                -JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
            }
        }
    }
}

abstract class AbstractFirMultiModuleOrderTest : AbstractJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/multiModuleOrder/",
    testGroupOutputDirPrefix = "firMultiModuleOrder/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureJsArtifactsHandlersStep {
                useHandlers(
                    ::JsWrongModuleHandler
                )
            }
        }
    }
}

abstract class AbstractJsSteppingTest(
    testGroupOutputDirPrefix: String = "debug/stepping/"
) : AbstractJsTest(
    pathToTestDir = "compiler/testData/debug/stepping/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    override val enableBoxHandlers: Boolean
        get() = false

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureSteppingTests()
    }
}

abstract class AbstractJsSteppingSplitTest : AbstractJsSteppingTest(
    testGroupOutputDirPrefix = "debug/steppingSplit/"
) {
    override val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>?
        get() = listOf(IGNORE_BACKEND_K2_MULTI_MODULE)

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            @OptIn(TestInfrastructureInternals::class)
            useModuleStructureTransformers(
                ::SplittingModuleTransformerForBoxTests
            )
            useMetaTestConfigurators(::SplittingTestConfigurator)
        }
    }
}

abstract class AbstractJsCodegenWasmJsInteropTest(
    testGroupOutputDirPrefix: String = "codegen/boxWasmJsInteropJs/"
) : AbstractJsTest(
    pathToTestDir = "compiler/testData/codegen/boxWasmJsInterop/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
)

fun TestConfigurationBuilderBase<*, *>.setUpDefaultDirectivesForJsBoxTest(parser: FirParser) {
    defaultDirectives {
        val runIc = getBoolean("kotlin.js.ir.icMode")
        if (runIc) +JsEnvironmentConfigurationDirectives.RUN_IC
        if (getBoolean("kotlin.js.ir.klibMainModule")) +JsEnvironmentConfigurationDirectives.KLIB_MAIN_MODULE
        if (getBoolean("kotlin.js.ir.dce", true)) +JsEnvironmentConfigurationDirectives.RUN_IR_DCE
        +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
        -JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
        DiagnosticsDirectives.DIAGNOSTICS with listOf("-infos")
        FirDiagnosticsDirectives.FIR_PARSER with parser
    }
}

private fun getBoolean(s: String, default: Boolean): Boolean {
    return System.getProperty(s)?.let { parseBoolean(it) } ?: default
}

fun TestConfigurationBuilder.configureLoweredIrDumpHandlers() {
    configureLoweredIrHandlersStep {
        useHandlers(
            { testServices, artifactKind ->
                IrTextDumpHandler(
                    testServices = testServices,
                    artifactKind = artifactKind,
                    customExtension = "inlined.ir",
                    directive = DUMP_IR_AFTER_INLINE,
                    directiveForIrDifference = DUMP_IR_AFTER_INLINE_DIFFERENCE,
                    showOffsets = true,
                )
            },
            ::IrPreprocessedInlineFunctionDumpHandler,
        )
    }
}
