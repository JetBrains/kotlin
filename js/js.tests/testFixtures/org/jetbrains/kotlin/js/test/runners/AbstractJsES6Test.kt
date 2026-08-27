/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpAfterInliningVerifyingHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.builders.configureKlibArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.commonIrHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.USE_CONST_AND_LET_FOR_VARIABLES
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

abstract class AbstractJsES6Test(
    pathToTestDir: String = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix: String,
    parser: FirParser = FirParser.Psi
) : AbstractJsTest(pathToTestDir, testGroupOutputDirPrefix, TargetBackend.JS_IR_ES6, parser) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +JsEnvironmentConfigurationDirectives.ES6_MODE
            }
        }
        builder.configureLoweredIrDumpHandlers()
    }
}


abstract class AbstractJsES6BoxTest : AbstractJsES6Test(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = "es6Box/"
)

abstract class AbstractJsES6WithConstLetBoxTest : AbstractJsES6Test(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = "es6WithConstLetBox/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useConstLet()
    }
}

abstract class AbstractJsES6CodegenBoxTest(
    testGroupOutputDirPrefix: String = "codegen/es6Box/",
) : AbstractJsES6Test(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix,
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }

        builder.useFailureSuppressors(
            ::FirMetaInfoDiffSuppressor
        )

        builder.configureIrHandlersStep {
            commonIrHandlersForCodegenTest()
        }

        // TODO KT-87965: Move it to setupCommonHandlersForJsTest() to fully turn or IR Inliner checks in all testrunners, inlcluding TS export
        builder.configureKlibArtifactsHandlersStep {
            useHandlers(::KlibAbiDumpAfterInliningVerifyingHandler)
        }
    }
}

abstract class AbstractJsES6WithConstLetCodegenBoxTest : AbstractJsES6CodegenBoxTest(
    testGroupOutputDirPrefix = "codegen/es6WithConstLet/",
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useConstLet()
    }
}

abstract class AbstractJsES6CodegenInlineTest : AbstractJsES6Test(
    pathToTestDir = "compiler/testData/codegen/boxInline/",
    testGroupOutputDirPrefix = "codegen/es6BoxInline/"
)

abstract class AbstractJsES6WithConstLetCodegenInlineTest : AbstractJsES6Test(
    pathToTestDir = "compiler/testData/codegen/boxInline/",
    testGroupOutputDirPrefix = "codegen/es6BoxWithConstLetInline/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useConstLet()
    }
}

abstract class AbstractJsES6CodegenWasmJsInteropTest : AbstractJsES6Test(
    pathToTestDir = "compiler/testData/codegen/boxWasmJsInterop",
    testGroupOutputDirPrefix = "codegen/boxWasmJsInteropEs6",
)

private fun TestConfigurationBuilder.useConstLet() {
    defaultDirectives {
        +USE_CONST_AND_LET_FOR_VARIABLES
    }
}
