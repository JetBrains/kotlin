/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.commonIrHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

abstract class AbstractJsSwcTest(
    pathToTestDir: String = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix: String,
    parser: FirParser = FirParser.Psi
) : AbstractJsTest(pathToTestDir, testGroupOutputDirPrefix, TargetBackend.JS_IR_ES6, parser) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +JsEnvironmentConfigurationDirectives.DELEGATE_JS_TRANSPILATION
            }
        }
        builder.configureLoweredIrDumpHandlers()
    }
}

abstract class AbstractJsSwcBoxTest(
    testGroupOutputDirPrefix: String = "swcBox/",
) : AbstractJsSwcTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix,
)

abstract class AbstractJsSwcCodegenBoxTestBase(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) : AbstractJsSwcTest(pathToTestDir, testGroupOutputDirPrefix) {
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
    }
}

abstract class AbstractJsSwcCodegenBoxTest(
    testGroupOutputDirPrefix: String = "codegen/swcBox/"
) : AbstractJsSwcCodegenBoxTestBase(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = testGroupOutputDirPrefix,
)

abstract class AbstractJsSwcCodegenBoxInlineTest : AbstractJsSwcCodegenBoxTestBase(
    pathToTestDir = "compiler/testData/codegen/boxInline",
    testGroupOutputDirPrefix = "codegen/swcBoxInline/"
)
