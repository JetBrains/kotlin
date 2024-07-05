/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import org.jetbrains.kotlin.js.test.JsSteppingTestAdditionalSourceProvider
import org.jetbrains.kotlin.js.test.converters.JsIrBackendFacade
import org.jetbrains.kotlin.js.test.converters.JsKlibBackendFacade
import org.jetbrains.kotlin.js.test.converters.incremental.RecompileModuleJsIrBackendFacade
import org.jetbrains.kotlin.js.test.handlers.*
import org.jetbrains.kotlin.js.test.utils.configureJsTypeScriptExportTest
import org.jetbrains.kotlin.js.test.utils.configureLineNumberTests
import org.jetbrains.kotlin.js.test.utils.configureSteppingTests
import org.jetbrains.kotlin.parsing.parseBoolean
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJsArtifactsHandlersStep
import org.jetbrains.kotlin.test.builders.jsArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind
import java.lang.Boolean.getBoolean

abstract class AbstractJsIrTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
    targetBackend: TargetBackend = TargetBackend.JS_IR,
) : AbstractJsBlackBoxCodegenTestBase<ClassicFrontendOutputArtifact, IrBackendInput, BinaryArtifacts.KLib>(
    FrontendKinds.ClassicFrontend, targetBackend, pathToTestDir, testGroupOutputDirPrefix, skipMinification = true
) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::JsKlibBackendFacade

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
                -JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
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

open class AbstractIrBoxJsTest : AbstractJsIrTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = "irBox/"
)

open class AbstractIrJsCodegenBoxTest : AbstractJsIrTest(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = "codegen/irBox/"
)

open class AbstractIrJsCodegenInlineTest : AbstractJsIrTest(
    pathToTestDir = "compiler/testData/codegen/boxInline/",
    testGroupOutputDirPrefix = "codegen/irBoxInline/"
)

open class AbstractIrJsTypeScriptExportTest : AbstractJsIrTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/typescript-export/js/",
    testGroupOutputDirPrefix = "typescript-export/ir/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureJsTypeScriptExportTest()
    }
}

open class AbstractIrJsES6TypeScriptExportTest : AbstractJsIrES6Test(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/typescript-export/js/",
    testGroupOutputDirPrefix = "typescript-export/ir-es6/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureJsTypeScriptExportTest()
    }
}

open class AbstractJsIrLineNumberTest : AbstractJsIrTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/lineNumbers/",
    testGroupOutputDirPrefix = "irLineNumbers/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureLineNumberTests(::createIrJsLineNumberHandler)
    }
}

open class AbstractSourceMapGenerationSmokeTest : AbstractJsIrTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/sourcemap/",
    testGroupOutputDirPrefix = "irSourcemap/"
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

open class AbstractMultiModuleOrderTest : AbstractJsIrTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/multiModuleOrder/",
    testGroupOutputDirPrefix = "irMultiModuleOrder/"
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

open class AbstractWebDemoExamplesTest : AbstractJsIrTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/webDemoExamples/",
    testGroupOutputDirPrefix = "webDemoExamples/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +JsEnvironmentConfigurationDirectives.KJS_WITH_FULL_RUNTIME
                -JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
                JsEnvironmentConfigurationDirectives.DONT_RUN_GENERATED_CODE.with("JS_IR")
            }

            configureJsArtifactsHandlersStep {
                useHandlers(::MainCallWithArgumentsHandler)
            }
        }
    }
}

open class AbstractIrJsSteppingTest : AbstractJsIrTest(
    pathToTestDir = "compiler/testData/debug/stepping/",
    testGroupOutputDirPrefix = "debug/irStepping/"
) {
    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForJsBlackBoxCodegenTest()
        configureSteppingTests()
    }
}

open class AbstractIrJsLocalVariableTest : AbstractJsIrTest(
    pathToTestDir = "compiler/testData/debug/localVariables/",
    testGroupOutputDirPrefix = "debug/localVariables/"
) {
    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForJsBlackBoxCodegenTest()
        defaultDirectives {
            +JsEnvironmentConfigurationDirectives.NO_COMMON_FILES
        }
        useAdditionalSourceProviders(::JsSteppingTestAdditionalSourceProvider)
        jsArtifactsHandlersStep {
            useHandlers(
                ::JsDebugRunner.bind(true)
            )
        }
    }
}

open class AbstractIrCodegenWasmJsInteropJsTest : AbstractJsIrTest(
    pathToTestDir = "compiler/testData/codegen/wasmJsInterop/",
    testGroupOutputDirPrefix = "codegen/irWasmJsInteropJs/"
)

// TODO(KT-64570): Don't inherit from AbstractJsIrTest after we move the common prefix of lowerings before serialization.
open class AbstractClassicJsKlibSyntheticAccessorTest : AbstractJsIrTest(
    pathToTestDir = "compiler/testData/klib/syntheticAccessors/",
    testGroupOutputDirPrefix = "klib/syntheticAccessors-k1/",
) {

    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForJsBlackBoxCodegenTest()
        defaultDirectives {
            +CodegenTestDirectives.ENABLE_IR_VISIBILITY_CHECKS_AFTER_INLINING
            +CodegenTestDirectives.ENABLE_EXPERIMENTAL_DOUBLE_INLINING
        }
    }
}
