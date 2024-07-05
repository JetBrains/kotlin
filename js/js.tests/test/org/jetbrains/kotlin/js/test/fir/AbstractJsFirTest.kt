package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.js.test.converters.FirJsKlibBackendFacade
import org.jetbrains.kotlin.js.test.converters.JsIrBackendFacade
import org.jetbrains.kotlin.js.test.converters.incremental.RecompileModuleJsIrBackendFacade
import org.jetbrains.kotlin.js.test.handlers.JsIrRecompiledArtifactsIdentityHandler
import org.jetbrains.kotlin.js.test.handlers.JsWrongModuleHandler
import org.jetbrains.kotlin.js.test.handlers.createFirJsLineNumberHandler
import org.jetbrains.kotlin.js.test.ir.AbstractJsBlackBoxCodegenTestBase
import org.jetbrains.kotlin.js.test.utils.configureJsTypeScriptExportTest
import org.jetbrains.kotlin.js.test.utils.configureLineNumberTests
import org.jetbrains.kotlin.js.test.utils.configureSteppingTests
import org.jetbrains.kotlin.parsing.parseBoolean
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.*
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrJsResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgConsistencyHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolvedTypesVerifier
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.codegen.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import java.lang.Boolean.getBoolean


open class AbstractFirJsTest(
    pathToTestDir: String = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix: String,
    targetBackend: TargetBackend = TargetBackend.JS_IR,
    val parser: FirParser = FirParser.Psi,
) : AbstractJsBlackBoxCodegenTestBase<FirOutputArtifact, IrBackendInput, BinaryArtifacts.KLib>(
    FrontendKinds.FIR, targetBackend, pathToTestDir, testGroupOutputDirPrefix, skipMinification = true
) {
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrJsResultsConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::FirJsKlibBackendFacade

    override val afterBackendFacade: Constructor<AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Js>>?
        get() = ::JsIrBackendFacade

    override val recompileFacade: Constructor<AbstractTestFacade<BinaryArtifacts.Js, BinaryArtifacts.Js>>
        get() = { RecompileModuleJsIrBackendFacade(it) }

    private fun getBoolean(s: String, default: Boolean) = System.getProperty(s)?.let { parseBoolean(it) } ?: default

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
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

            firHandlersStep {
                useHandlers(
                    ::FirDumpHandler,
                    ::FirCfgDumpHandler,
                    ::FirCfgConsistencyHandler,
                    ::FirResolvedTypesVerifier,
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

open class AbstractFirPsiJsBoxTest : AbstractFirJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = "firPsiBox/",
    parser = FirParser.Psi,
)

open class AbstractFirLightTreeJsBoxTest : AbstractFirJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = "firLightTreeBox/",
    parser = FirParser.LightTree,
)

open class AbstractFirJsCodegenBoxTest : AbstractFirJsTest(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = "codegen/firBox/"
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

open class AbstractFirJsCodegenInlineTest : AbstractFirJsTest(
    pathToTestDir = "compiler/testData/codegen/boxInline/",
    testGroupOutputDirPrefix = "codegen/firBoxInline/"
)

open class AbstractFirJsTypeScriptExportTest : AbstractFirJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/typescript-export/",
    testGroupOutputDirPrefix = "typescript-export/fir/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureJsTypeScriptExportTest()
    }
}

open class AbstractFirJsES6TypeScriptExportTest : AbstractFirJsES6Test(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/typescript-export/",
    testGroupOutputDirPrefix = "typescript-export/fir-es6"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureJsTypeScriptExportTest()
    }
}

open class AbstractFirJsLineNumberTest : AbstractFirJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/lineNumbers/",
    testGroupOutputDirPrefix = "firLineNumbers/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureLineNumberTests(::createFirJsLineNumberHandler)
    }
}

open class AbstractFirSourceMapGenerationSmokeTest : AbstractFirJsTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/sourcemap/",
    testGroupOutputDirPrefix = "firSourcemap/"
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

open class AbstractFirJsSteppingTest : AbstractFirJsTest(
    pathToTestDir = "compiler/testData/debug/stepping/",
    testGroupOutputDirPrefix = "debug/firStepping/"
) {
    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForJsBlackBoxCodegenTest()
        configureSteppingTests()
    }
}

open class AbstractFirJsCodegenWasmJsInteropTest : AbstractFirJsTest(
    pathToTestDir = "compiler/testData/codegen/wasmJsInterop/",
    testGroupOutputDirPrefix = "codegen/firWasmJsInteropJs/"
)

// TODO(KT-64570): Don't inherit from AbstractFirJsTest after we move the common prefix of lowerings before serialization.
open class AbstractFirJsKlibSyntheticAccessorTest : AbstractFirJsTest(
    pathToTestDir = "compiler/testData/klib/syntheticAccessors/",
    testGroupOutputDirPrefix = "klib/syntheticAccessors-k2/",
) {

    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForJsBlackBoxCodegenTest()
        defaultDirectives {
            +CodegenTestDirectives.ENABLE_IR_VISIBILITY_CHECKS_AFTER_INLINING
            +CodegenTestDirectives.ENABLE_EXPERIMENTAL_DOUBLE_INLINING
        }
    }
}
