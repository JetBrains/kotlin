package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

abstract class AbstractJsES6Test(
    pathToTestDir: String = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix: String,
) : AbstractJsTest(pathToTestDir, testGroupOutputDirPrefix, TargetBackend.JS_IR_ES6) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +JsEnvironmentConfigurationDirectives.ES6_MODE
            }
        }
    }
}


open class AbstractJsES6BoxTest : AbstractJsES6Test(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = "es6Box/"
)

open class AbstractJsES6CodegenBoxTest : AbstractJsES6Test(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = "codegen/es6Box/"
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

open class AbstractJsES6CodegenInlineTest : AbstractJsES6Test(
    pathToTestDir = "compiler/testData/codegen/boxInline/",
    testGroupOutputDirPrefix = "codegen/es6BoxInline/"
)

open class AbstractJsES6CodegenWasmJsInteropTest : AbstractJsES6Test(
    pathToTestDir = "compiler/testData/codegen/wasmJsInterop",
    testGroupOutputDirPrefix = "codegen/wasmJsInteropJsEs6"
)
