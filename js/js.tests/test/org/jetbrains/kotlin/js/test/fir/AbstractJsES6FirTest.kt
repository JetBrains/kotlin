package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.runners.codegen.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

abstract class AbstractFirJsES6Test(
    pathToTestDir: String = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix: String,
) : AbstractFirJsTest(pathToTestDir, testGroupOutputDirPrefix, TargetBackend.JS_IR_ES6) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +JsEnvironmentConfigurationDirectives.ES6_MODE
            }
        }
    }
}


open class AbstractFirJsES6BoxTest : AbstractFirJsES6Test(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = "firEs6Box/"
)

open class AbstractFirJsES6CodegenBoxTest : AbstractFirJsES6Test(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = "codegen/firEs6Box/"
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

open class AbstractFirJsES6CodegenBoxErrorTest : AbstractFirJsES6Test(
    pathToTestDir = "compiler/testData/codegen/boxError/",
    testGroupOutputDirPrefix = "codegen/firEs6BoxError/"
)

open class AbstractFirJsES6CodegenInlineTest : AbstractFirJsES6Test(
    pathToTestDir = "compiler/testData/codegen/boxInline/",
    testGroupOutputDirPrefix = "codegen/firEs6BoxInline/"
)

open class AbstractFirJsES6CodegenWasmJsInteropTest : AbstractFirJsES6Test(
    pathToTestDir = "compiler/testData/codegen/wasmJsInterop",
    testGroupOutputDirPrefix = "codegen/wasmJsInteropJsEs6"
)
