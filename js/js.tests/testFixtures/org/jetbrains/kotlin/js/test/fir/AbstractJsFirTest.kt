/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWebFacade
import org.jetbrains.kotlin.js.test.handlers.JsIrRecompiledArtifactsIdentityHandler
import org.jetbrains.kotlin.js.test.handlers.JsLineNumberHandler
import org.jetbrains.kotlin.js.test.handlers.JsWrongModuleHandler
import org.jetbrains.kotlin.js.test.ir.AbstractJsBlackBoxCodegenTestBase
import org.jetbrains.kotlin.js.test.utils.configureJsTypeScriptExportTest
import org.jetbrains.kotlin.js.test.utils.configureLineNumberTests
import org.jetbrains.kotlin.js.test.utils.configureSteppingTests
import org.jetbrains.kotlin.parsing.parseBoolean
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.handlers.IrPreprocessedInlineFunctionDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR_AFTER_INLINE
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2_MULTI_MODULE
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.KlibAbiConsistencyDirectives.CHECK_SAME_ABI_AFTER_INLINING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgConsistencyHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolvedTypesVerifier
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.SplittingModuleTransformerForBoxTests
import org.jetbrains.kotlin.test.services.SplittingTestConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import java.lang.Boolean.getBoolean


open class AbstractFirJsTest(
    pathToTestDir: String = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix: String,
    targetBackend: TargetBackend = TargetBackend.JS_IR,
    val parser: FirParser = FirParser.Psi,
) : AbstractJsBlackBoxCodegenTestBase<FirOutputArtifact>(
    FrontendKinds.FIR, targetBackend, pathToTestDir, testGroupOutputDirPrefix
) {
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirCliWebFacade

    override val frontendToIrConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrCliWebFacade

    override val serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::FirKlibSerializerCliWebFacade

    override val backendFacades: JsBackendFacades
        get() = JsBackendFacades.WithRecompilation

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            setupDefaultDirectivesForFirJsBoxTest(parser)
            firHandlersStep {
                useHandlers(
                    ::FirDumpHandler,
                    ::FirCfgDumpHandler,
                    ::FirCfgConsistencyHandler,
                    ::FirResolvedTypesVerifier,
                )
            }
            defaultDirectives {
                LANGUAGE with listOf(
                    "-${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "-${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
            }

            configureJsArtifactsHandlersStep {
                useHandlers(
                    ::JsIrRecompiledArtifactsIdentityHandler,
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

open class AbstractPsiJsBoxTest : AbstractFirJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = "psiBox/",
    parser = FirParser.Psi,
)

open class AbstractLightTreeJsBoxTest : AbstractFirJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = "lightTreeBox/",
    parser = FirParser.LightTree,
)

open class AbstractFirJsCodegenBoxTestBase(testGroupOutputDirPrefix: String) : AbstractFirJsTest(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }

        builder.useAfterAnalysisCheckers(
            ::FirMetaInfoDiffSuppressor
        )
    }
}

open class AbstractJsCodegenBoxTest : AbstractFirJsCodegenBoxTestBase(
    testGroupOutputDirPrefix = "codegen/box/"
)

open class AbstractJsCodegenBoxWithInlinedFunInKlibTest : AbstractFirJsCodegenBoxTestBase(
    testGroupOutputDirPrefix = "codegen/boxWithInlinedFunInKlib"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +CHECK_SAME_ABI_AFTER_INLINING
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
            }
            configureLoweredIrHandlersStep {
                useHandlers(
                    { ts, ak -> IrTextDumpHandler(ts, ak, "inlined.ir", DUMP_IR_AFTER_INLINE) },
                    ::IrPreprocessedInlineFunctionDumpHandler,
                )
            }
        }
    }
}

open class AbstractJsCodegenInlineTest(
    testGroupOutputDirPrefix: String = "codegen/boxInline/"
) : AbstractFirJsTest(
    pathToTestDir = "compiler/testData/codegen/boxInline/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
)

open class AbstractJsCodegenInlineWithInlinedFunInKlibTest(
    testGroupOutputDirPrefix: String = "codegen/boxInlineInlined/"
) : AbstractJsCodegenInlineTest(
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
            }
        }
    }
}

open class AbstractJsCodegenSplittingInlineWithInlinedFunInKlibTest : AbstractJsCodegenInlineWithInlinedFunInKlibTest(
    testGroupOutputDirPrefix = "codegen/boxInlineSplitted/"
) {
    override val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>?
        get() = listOf(IGNORE_BACKEND_K2_MULTI_MODULE)

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        @OptIn(TestInfrastructureInternals::class)
        builder.useModuleStructureTransformers(
            ::SplittingModuleTransformerForBoxTests
        )
        builder.useMetaTestConfigurators(::SplittingTestConfigurator)
    }
}

open class AbstractJsTypeScriptExportTest(
    testGroupOutputDirPrefix: String = "typescript-export/es5"
) : AbstractFirJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/typescript-export/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureJsTypeScriptExportTest()
    }
}

open class AbstractJsTypeScriptExportWithInlinedFunInKlibTest : AbstractJsTypeScriptExportTest(
    testGroupOutputDirPrefix = "typescript-export/es5-withInlinedFunInKlib"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
            }
        }
    }
}

open class AbstractJsES6TypeScriptExportTest(
    testGroupOutputDirPrefix: String = "typescript-export/es6"
) : AbstractJsES6Test(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/typescript-export/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureJsTypeScriptExportTest()
    }
}

open class AbstractJsLineNumberTest(
    testGroupOutputDirPrefix: String = "lineNumbers/"
) : AbstractFirJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/lineNumbers/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureLineNumberTests(::JsLineNumberHandler)
    }
}

open class AbstractJsLineNumberWithInlinedFunInKlibTest : AbstractJsLineNumberTest(
    testGroupOutputDirPrefix = "lineNumbersInlined/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureLineNumberTests(::JsLineNumberHandler)
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
            }
        }
    }
}

open class AbstractSourceMapGenerationSmokeTest : AbstractFirJsTest(
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

open class AbstractFirMultiModuleOrderTest : AbstractFirJsTest(
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

open class AbstractJsSteppingTest(
    testGroupOutputDirPrefix: String = "debug/stepping/"
) : AbstractFirJsTest(
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

open class AbstractJsSteppingWithInlinedFunInKlibTest(
    testGroupOutputDirPrefix: String = "debug/steppingWithInlinedFunInKlib/"
) : AbstractJsSteppingTest(
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
            }
        }
    }
}

open class AbstractJsSteppingSplitTest : AbstractJsSteppingTest(
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

open class AbstractJsSteppingSplitWithInlinedFunInKlibTest : AbstractJsSteppingWithInlinedFunInKlibTest(
    testGroupOutputDirPrefix = "debug/steppingSplitWithInlinedFunInKlib/"
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

open class AbstractJsCodegenWasmJsInteropTest(
    testGroupOutputDirPrefix: String = "codegen/wasmJsInteropJs/"
) : AbstractFirJsTest(
    pathToTestDir = "compiler/testData/codegen/wasmJsInterop/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
)

open class AbstractJsCodegenWasmJsInteropWithInlinedFunInKlibTest : AbstractJsCodegenWasmJsInteropTest(
    testGroupOutputDirPrefix = "codegen/wasmJsInteropJsWithInlinedFunInKlib/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
            }
        }
    }
}

fun TestConfigurationBuilder.setupDefaultDirectivesForFirJsBoxTest(parser: FirParser) {
    defaultDirectives {
        val runIc = getBoolean("kotlin.js.ir.icMode")
        if (runIc) +JsEnvironmentConfigurationDirectives.RUN_IC
        if (getBoolean("kotlin.js.ir.klibMainModule")) +JsEnvironmentConfigurationDirectives.KLIB_MAIN_MODULE
        if (getBoolean("kotlin.js.ir.perModule", true)) +JsEnvironmentConfigurationDirectives.PER_MODULE
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
