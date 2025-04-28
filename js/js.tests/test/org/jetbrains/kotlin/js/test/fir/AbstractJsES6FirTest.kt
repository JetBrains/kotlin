package org.jetbrains.kotlin.js.test.fir

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
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


open class AbstractFirJsES6BoxTest(testGroupOutputDirPrefix: String = "firEs6Box/") : AbstractFirJsES6Test(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
)

open class AbstractFirJsES6BoxWithInlinedFunInKlibTest : AbstractFirJsES6BoxTest(
    testGroupOutputDirPrefix = "firEs6BoxWithInlinedFunInKlib/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                LANGUAGE with "+${LanguageFeature.IrInlinerBeforeKlibSerialization.name}"
            }
        }
    }
}

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

open class AbstractFirJsES6CodegenInlineTest(testGroupOutputDirPrefix: String = "codegen/firEs6BoxInline/") : AbstractFirJsES6Test(
    pathToTestDir = "compiler/testData/codegen/boxInline/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix
)

open class AbstractFirJsES6CodegenInlineWithInlinedFunInKlibTest : AbstractFirJsES6CodegenInlineTest(
    testGroupOutputDirPrefix = "codegen/firEs6BoxInlineWithInlinedFunInKlib/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                LANGUAGE with "+${LanguageFeature.IrInlinerBeforeKlibSerialization.name}"
            }
        }
    }
}

open class AbstractFirJsES6CodegenWasmJsInteropTest(testGroupOutputDirPrefix: String = "codegen/wasmJsInteropJsEs6") : AbstractFirJsES6Test(
    pathToTestDir = "compiler/testData/codegen/wasmJsInterop",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix,
)

open class AbstractFirJsES6CodegenWasmJsInteropWithInlinedFunInKlibTest : AbstractFirJsES6CodegenWasmJsInteropTest(
    testGroupOutputDirPrefix = "codegen/wasmJsInteropJsEs6WithInlinedFunInKlib"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                LANGUAGE with "+${LanguageFeature.IrInlinerBeforeKlibSerialization.name}"
            }
        }
    }
}
